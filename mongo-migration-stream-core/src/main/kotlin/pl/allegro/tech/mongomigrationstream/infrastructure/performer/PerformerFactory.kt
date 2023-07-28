package pl.allegro.tech.mongomigrationstream.infrastructure.performer

import com.mongodb.client.MongoDatabase
import io.micrometer.core.instrument.MeterRegistry
import pl.allegro.tech.mongomigrationstream.configuration.ApplicationProperties
import pl.allegro.tech.mongomigrationstream.configuration.MongoProperties.MongoAuthenticationProperties
import pl.allegro.tech.mongomigrationstream.core.index.IndexClonerImpl
import pl.allegro.tech.mongomigrationstream.core.index.NoOpIndexCloner
import pl.allegro.tech.mongomigrationstream.core.mongo.SourceToDestination
import pl.allegro.tech.mongomigrationstream.core.performer.IndexCloner
import pl.allegro.tech.mongomigrationstream.core.performer.Performer
import pl.allegro.tech.mongomigrationstream.core.performer.ResumableSynchronizer
import pl.allegro.tech.mongomigrationstream.core.performer.Synchronizer
import pl.allegro.tech.mongomigrationstream.core.performer.Transfer
import pl.allegro.tech.mongomigrationstream.core.queue.EventQueue
import pl.allegro.tech.mongomigrationstream.core.state.StateInfo
import pl.allegro.tech.mongomigrationstream.core.synchronization.BatchSizeProvider
import pl.allegro.tech.mongomigrationstream.core.synchronization.ChangeEvent
import pl.allegro.tech.mongomigrationstream.core.synchronization.LocalToDestinationSynchronizer
import pl.allegro.tech.mongomigrationstream.core.synchronization.NoOpResumableSynchronizer
import pl.allegro.tech.mongomigrationstream.core.synchronization.NoOpSynchronizer
import pl.allegro.tech.mongomigrationstream.core.synchronization.SourceToLocalSynchronizer
import pl.allegro.tech.mongomigrationstream.core.transfer.MongoToolsTransfer
import pl.allegro.tech.mongomigrationstream.core.transfer.NoOpTransfer
import pl.allegro.tech.mongomigrationstream.core.transfer.authentication.PasswordConfigFileGenerator
import pl.allegro.tech.mongomigrationstream.core.transfer.authentication.PasswordConfigFiles
import pl.allegro.tech.mongomigrationstream.infrastructure.mongo.MongoDbClients
import java.nio.file.Path
import kotlin.io.path.absolutePathString

internal object PerformerFactory {
    fun createPerformers(
        properties: ApplicationProperties,
        mongoDbClients: MongoDbClients,
        queues: Map<SourceToDestination, EventQueue<ChangeEvent>>,
        stateInfo: StateInfo,
        meterRegistry: MeterRegistry
    ): List<Performer> {
        val passwordConfigFiles = createMongoToolsAuthConfig(properties)
        val allPerformers = properties.sourceToDestinationMapping.map {
            createPerformer(properties, it, queues[it]!!, mongoDbClients, passwordConfigFiles, stateInfo, meterRegistry)
        }
        return allPerformers
    }

    private fun createMongoToolsAuthConfig(properties: ApplicationProperties): PasswordConfigFiles {
        val rootPath = properties.performerProperties.rootPath
        return PasswordConfigFiles(
            properties.sourceDbProperties.authenticationProperties
                ?.createPasswordConfigFile(rootPath, "source")
                ?.absolutePathString(),
            properties.destinationDbProperties.authenticationProperties
                ?.createPasswordConfigFile(rootPath, "destination")
                ?.absolutePathString()
        )
    }

    private fun MongoAuthenticationProperties.createPasswordConfigFile(rootPath: String, db: String): Path =
        PasswordConfigFileGenerator.generatePasswordConfigFile(rootPath, db, password)

    private fun createPerformer(
        properties: ApplicationProperties,
        sourceToDestination: SourceToDestination,
        queue: EventQueue<ChangeEvent>,
        mongoDbClients: MongoDbClients,
        passwordConfigFiles: PasswordConfigFiles,
        stateInfo: StateInfo,
        meterRegistry: MeterRegistry
    ): Performer {
        val sourceToLocalSynchronizer: Synchronizer = createSourceToLocalSynchronizer(
            properties.generalProperties.shouldPerformSynchronization,
            sourceToDestination,
            mongoDbClients,
            queue,
            stateInfo,
            meterRegistry
        )
        val localToDestinationSynchronizer: ResumableSynchronizer = createLocalToDestinationSynchronizer(
            properties.generalProperties.shouldPerformSynchronization,
            sourceToDestination,
            mongoDbClients,
            queue,
            stateInfo,
            properties.performerProperties.batchSizeProvider
        )
        val transfer: Transfer = createTransfer(
            properties.generalProperties.shouldPerformTransfer,
            properties,
            sourceToDestination,
            passwordConfigFiles,
            stateInfo
        )
        val indexCloner: IndexCloner =
            createIndexCloner(
                properties.generalProperties.shouldPerformTransfer,
                sourceToDestination,
                mongoDbClients.sourceDatabase,
                mongoDbClients.destinationDatabase,
                stateInfo
            )

        return Performer(
            sourceToLocalSynchronizer,
            transfer,
            localToDestinationSynchronizer,
            indexCloner,
            sourceToDestination,
            stateInfo
        )
    }

    private fun createLocalToDestinationSynchronizer(
        shouldPerformSynchronization: Boolean,
        sourceToDestination: SourceToDestination,
        mongoDbClients: MongoDbClients,
        queue: EventQueue<ChangeEvent>,
        stateInfo: StateInfo,
        batchSizeProvider: BatchSizeProvider
    ): ResumableSynchronizer = if (shouldPerformSynchronization) LocalToDestinationSynchronizer(
        mongoDbClients.destinationDatabase,
        sourceToDestination,
        queue,
        stateInfo,
        batchSizeProvider
    ) else NoOpResumableSynchronizer()

    private fun createTransfer(
        shouldPerformTransfer: Boolean,
        properties: ApplicationProperties,
        sourceToDestination: SourceToDestination,
        passwordConfigFiles: PasswordConfigFiles,
        stateInfo: StateInfo
    ): Transfer = if (shouldPerformTransfer) MongoToolsTransfer(
        properties,
        sourceToDestination,
        passwordConfigFiles,
        stateInfo
    ) else NoOpTransfer()

    private fun createSourceToLocalSynchronizer(
        shouldPerformSynchronization: Boolean,
        sourceToDestination: SourceToDestination,
        mongoDbClients: MongoDbClients,
        queue: EventQueue<ChangeEvent>,
        stateInfo: StateInfo,
        meterRegistry: MeterRegistry
    ): Synchronizer = if (shouldPerformSynchronization) SourceToLocalSynchronizer(
        sourceToDestination,
        mongoDbClients.sourceDatabase,
        mongoDbClients.reactiveSourceDatabase,
        queue,
        stateInfo,
        meterRegistry
    ) else NoOpSynchronizer()

    private fun createIndexCloner(
        shouldPerformTransfer: Boolean,
        sourceToDestination: SourceToDestination,
        sourceDatabase: MongoDatabase,
        destinationDatabase: MongoDatabase,
        stateInfo: StateInfo
    ): IndexCloner =
        if (shouldPerformTransfer) IndexClonerImpl(sourceToDestination, sourceDatabase, destinationDatabase, stateInfo)
        else NoOpIndexCloner()
}
