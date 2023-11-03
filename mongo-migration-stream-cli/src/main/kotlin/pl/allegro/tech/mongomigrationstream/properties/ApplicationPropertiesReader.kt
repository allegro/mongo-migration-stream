package pl.allegro.tech.mongomigrationstream.properties

import com.mongodb.ReadPreference
import com.ufoscout.properlty.Properlty
import pl.allegro.tech.mongomigrationstream.configuration.ApplicationProperties
import pl.allegro.tech.mongomigrationstream.configuration.CollectionsProperties
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties.CollectionCountSynchronizationDetectorType
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties.DbAvailabilityValidatorType
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties.DbHashSynchronizationDetectorType
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties.DestinationMissingCollectionType
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties.LoggingSynchronizationHandler
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties.MongoToolsValidatorType
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties.QueueSizeSynchronizationDetectorType
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties.SourceCollectionAvailabilityType
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties.SynchronizationDetectorType
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties.SynchronizationHandlerType
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties.ValidatorType
import pl.allegro.tech.mongomigrationstream.configuration.MongoProperties
import pl.allegro.tech.mongomigrationstream.configuration.PerformerProperties
import pl.allegro.tech.mongomigrationstream.configuration.PerformerProperties.QueueFactoryType
import pl.allegro.tech.mongomigrationstream.configuration.StateConfig
import pl.allegro.tech.mongomigrationstream.core.synchronization.ConstantValueBatchSizeProvider
import pl.allegro.tech.mongomigrationstream.infrastructure.handler.LoggingDetectionResultHandler
import pl.allegro.tech.mongomigrationstream.infrastructure.handler.LoggingStateEventHandler

internal object ApplicationPropertiesReader {

    fun readApplicationProperties(paths: List<String>): ApplicationProperties {
        val properties: Properlty = Properlty.builder()
            .apply { paths.forEach { path -> add(path) } }
            .build()

        val general = readGeneralProperties(properties)
        val source = readMongoProperties(properties, "source")
        val destination = readMongoProperties(properties, "destination")
        val collections = readCollectionsProperties(properties)
        val performer = readPerformerProperties(properties)
        val stateConfig = defaultStateConfig()

        return ApplicationProperties(
            generalProperties = general,
            sourceDbProperties = source,
            destinationDbProperties = destination,
            collectionsProperties = collections,
            performerProperties = performer,
            stateConfig = stateConfig
        )
    }

    private fun defaultStateConfig(): StateConfig = StateConfig(LoggingStateEventHandler)

    private fun readPerformerProperties(properties: Properlty): PerformerProperties {
        val rootPath = properties.requiredProperty("custom.rootPath", "/tmp/")
        val mongoToolsPath = properties.requiredProperty("custom.mongoToolsPath", "")
        val queueFactoryType = properties.requiredProperty("custom.queue.factory", "InMemoryCustomQueueFactory")
        val dumpReadPreference = properties.requiredProperty("custom.dumpReadPreference", ReadPreference.primary().name)
        val batchSize = properties.requiredProperty("custom.batchSize", "1000").toIntOrNull() ?: 1000

        return PerformerProperties(
            rootPath,
            mongoToolsPath,
            QueueFactoryTypeMapper.from(queueFactoryType),
            ReadPreference.valueOf(dumpReadPreference),
            ConstantValueBatchSizeProvider(batchSize)
        )
    }

    private fun readGeneralProperties(properties: Properlty): GeneralProperties {
        val shouldPerformMigration = properties.getBoolean("perform.transfer", true)
        val shouldPerformSynchronization = properties.getBoolean("perform.synchronization", true)
        val synchronizationHandlers = properties.getArray("perform.synchronization.handlers", ",")
            .map { SynchronizationHandlerTypeMapper.from(it) }
            .map {
                when (it) {
                    LoggingSynchronizationHandler -> LoggingDetectionResultHandler
                }
            }.toSet()
        val synchronizationDetectors = properties.getArray("perform.synchronization.detectors", ",")
            .map { SynchronizationDetectorTypeMapper.from(it) }
            .toSet()

        val validators = properties.getArray("perform.synchronization.validators", ",")
            .map { ValidatorTypeMapper.from(it) }
            .toSet()

        return GeneralProperties(
            shouldPerformMigration,
            shouldPerformSynchronization,
            synchronizationHandlers,
            synchronizationDetectors,
            validators
        )
    }

    private fun readMongoProperties(properties: Properlty, prefix: String): MongoProperties {
        val uri = properties.requiredProperty("$prefix.db.uri")
        val dbName = properties.requiredProperty("$prefix.db.name")

        val shouldUseAuth = properties.getBoolean("$prefix.db.authentication.enabled", false)
        val authentication = if (shouldUseAuth) {
            MongoProperties.MongoAuthenticationProperties(
                username = properties.requiredProperty("$prefix.db.authentication.username"),
                password = properties.requiredProperty("$prefix.db.authentication.password"),
                authDbName = properties.requiredProperty("$prefix.db.authentication.authDbName")
            )
        } else null

        return MongoProperties(uri, dbName, authentication)
    }

    private fun readCollectionsProperties(properties: Properlty): CollectionsProperties {
        val sourceCollections = properties.getArray("collections.source", ",").map { it }
        val destinationCollections = properties.getArray("collections.destination", ",").map { it }
        return CollectionsProperties(sourceCollections, destinationCollections)
    }

    private fun Properlty.requiredProperty(propertyName: String, defaultValue: String? = null): String =
        this[propertyName] ?: defaultValue ?: throw MissingRequiredPropertyException(propertyName)

    class MissingRequiredPropertyException(propertyName: String) :
        RuntimeException("Required property [$propertyName] is missing. Please add it in property file.")

    internal object QueueFactoryTypeMapper {
        private val supportedTypes = mapOf(
            "InMemoryCustomQueueFactory" to PerformerProperties.InMemoryQueueFactoryType,
            "BiqQueueFactory" to PerformerProperties.BiqQueueFactoryType
        )

        fun from(type: String): QueueFactoryType =
            supportedTypes[type] ?: throw InvalidQueueFactoryType(type)

        class InvalidQueueFactoryType(type: String) :
            IllegalArgumentException(
                "Invalid queue factory type: [$type]. Possible types are: [${supportedTypes.keys}]"
            )
    }

    internal object SynchronizationHandlerTypeMapper {
        private val supportedSynchronizationHandlers = mapOf(
            "LoggingSynchronizationHandler" to LoggingSynchronizationHandler,
        )

        fun from(type: String): SynchronizationHandlerType =
            supportedSynchronizationHandlers[type] ?: throw InvalidSynchronizationHandlerTypeException(type)

        class InvalidSynchronizationHandlerTypeException(synchronizationHandlerType: String) :
            IllegalArgumentException(
                "Invalid synchronization handler type: [$synchronizationHandlerType]. " +
                    "Possible types are: [${supportedSynchronizationHandlers.keys}]"
            )
    }

    internal object SynchronizationDetectorTypeMapper {
        private val supportedSynchronizationDetectors = mapOf(
            "DbHash" to DbHashSynchronizationDetectorType,
            "QueueSize" to QueueSizeSynchronizationDetectorType,
            "CollectionCount" to CollectionCountSynchronizationDetectorType
        )

        fun from(type: String): SynchronizationDetectorType =
            supportedSynchronizationDetectors[type] ?: throw InvalidSynchronizationDetectorTypeException(type)

        class InvalidSynchronizationDetectorTypeException(detectorType: String) :
            IllegalArgumentException(
                "Invalid synchronization detector type: " +
                    "[$detectorType]. Possible types are: [${supportedSynchronizationDetectors.keys}]"
            )
    }

    internal object ValidatorTypeMapper {
        private val supportedValidators = mapOf(
            "DestinationCollectionMissing" to DestinationMissingCollectionType,
            "DbAvailability" to DbAvailabilityValidatorType,
            "SourceCollectionAvailable" to SourceCollectionAvailabilityType,
            "MontoToolsAvailable" to MongoToolsValidatorType
        )

        fun from(type: String): ValidatorType =
            supportedValidators[type] ?: throw InvalidValidatorTypeException(type)

        class InvalidValidatorTypeException(validatorType: String) :
            IllegalArgumentException(
                "Invalid validator type: " +
                    "[$validatorType]. Possible types are: [${supportedValidators.keys}]"
            )
    }
}
