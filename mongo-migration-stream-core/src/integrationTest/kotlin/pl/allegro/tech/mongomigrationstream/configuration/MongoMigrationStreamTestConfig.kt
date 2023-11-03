package pl.allegro.tech.mongomigrationstream.configuration

import com.mongodb.ReadPreference
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import pl.allegro.tech.mongomigrationstream.MongoMigrationStream
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties.CollectionCountSynchronizationDetectorType
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties.DbAvailabilityValidatorType
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties.DbHashSynchronizationDetectorType
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties.DestinationMissingCollectionType
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties.QueueSizeSynchronizationDetectorType
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties.SourceCollectionAvailabilityType
import pl.allegro.tech.mongomigrationstream.configuration.MongoMigrationStreamTestProperties.DESTINATION_DB_NAME
import pl.allegro.tech.mongomigrationstream.configuration.MongoMigrationStreamTestProperties.MONGO_TOOLS_PATH
import pl.allegro.tech.mongomigrationstream.configuration.MongoMigrationStreamTestProperties.ROOT_PATH
import pl.allegro.tech.mongomigrationstream.configuration.MongoMigrationStreamTestProperties.SOURCE_DB_NAME
import pl.allegro.tech.mongomigrationstream.configuration.MongoMigrationStreamTestProperties.destinationCollections
import pl.allegro.tech.mongomigrationstream.configuration.MongoMigrationStreamTestProperties.sourceCollections
import pl.allegro.tech.mongomigrationstream.core.synchronization.ConstantValueBatchSizeProvider
import pl.allegro.tech.mongomigrationstream.infrastructure.handler.LoggingDetectionResultHandler
import pl.allegro.tech.mongomigrationstream.infrastructure.handler.LoggingStateEventHandler

internal object MongoMigrationStreamTestConfig {
    fun createMongoMigrationStream(
        generalProperties: GeneralProperties = defaultGeneralProperties(),
        sourceDbProperties: MongoProperties = defaultSourceDbProperties(),
        destinationDbProperties: MongoProperties = defaultDestinationDbProperties(),
        collectionsProperties: CollectionsProperties = defaultCollectionsProperties(),
        performerProperties: PerformerProperties = defaultPerformerProperties(),
        stateConfig: StateConfig = defaultStateConfig()
    ) = MongoMigrationStream(
        properties = ApplicationProperties(
            generalProperties = generalProperties,
            sourceDbProperties = sourceDbProperties,
            destinationDbProperties = destinationDbProperties,
            collectionsProperties = collectionsProperties,
            performerProperties = performerProperties,
            stateConfig = stateConfig
        ),
        SimpleMeterRegistry()
    )

    fun defaultGeneralProperties() = GeneralProperties(
        shouldPerformTransfer = true,
        shouldPerformSynchronization = true,
        synchronizationHandlers = setOf(LoggingDetectionResultHandler),
        synchronizationDetectors = setOf(
            DbHashSynchronizationDetectorType,
            QueueSizeSynchronizationDetectorType,
            CollectionCountSynchronizationDetectorType,
        ),
        databaseValidators = setOf(
            DbAvailabilityValidatorType,
            SourceCollectionAvailabilityType,
            DestinationMissingCollectionType
        ),
        defaultTimeoutInSeconds = 10,
    )

    private fun defaultPerformerProperties() = PerformerProperties(
        rootPath = ROOT_PATH,
        mongoToolsPath = MONGO_TOOLS_PATH,
        queueFactory = PerformerProperties.BiqQueueFactoryType,
        dumpReadPreference = ReadPreference.primary(),
        batchSizeProvider = ConstantValueBatchSizeProvider(1000)
    )

    private fun defaultCollectionsProperties() = CollectionsProperties(
        sourceCollections = sourceCollections,
        destinationCollections = destinationCollections
    )

    private fun defaultSourceDbProperties() = MongoProperties(
        uri = IntegrationTestProjectConfig.mongoExtension.mongo36uri(),
        dbName = SOURCE_DB_NAME,
        authenticationProperties = null,
        connectTimeoutInSeconds = 10,
        readTimeoutInSeconds = 10,
        serverSelectionTimeoutInSeconds = 10,
        readPreference = ReadPreference.secondary(),
    )

    private fun defaultDestinationDbProperties() = MongoProperties(
        uri = IntegrationTestProjectConfig.mongoExtension.mongo60uri(),
        dbName = DESTINATION_DB_NAME,
        authenticationProperties = null,
        connectTimeoutInSeconds = 10,
        readTimeoutInSeconds = 10,
        serverSelectionTimeoutInSeconds = 10,
    )

    private fun defaultStateConfig() = StateConfig(LoggingStateEventHandler)
}
