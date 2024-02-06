package pl.allegro.tech.mongomigrationstream.test.preconditions

import com.mongodb.ReadPreference
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.assertThrows
import pl.allegro.tech.mongomigrationstream.configuration.CollectionsProperties
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties
import pl.allegro.tech.mongomigrationstream.configuration.MongoMigrationStreamTestConfig.createMongoMigrationStream
import pl.allegro.tech.mongomigrationstream.configuration.MongoMigrationStreamTestProperties
import pl.allegro.tech.mongomigrationstream.configuration.MongoProperties
import pl.allegro.tech.mongomigrationstream.configuration.PerformerProperties
import pl.allegro.tech.mongomigrationstream.core.synchronization.ConstantValueBatchSizeProvider
import pl.allegro.tech.mongomigrationstream.infrastructure.controller.MongoMigrationStreamStartException
import pl.allegro.tech.mongomigrationstream.infrastructure.handler.LoggingDetectionResultHandler
import pl.allegro.tech.mongomigrationstream.test.preconditions.StartMigrationValidationTest.TestData.generalPropertiesWithDBToolsValidator
import pl.allegro.tech.mongomigrationstream.test.preconditions.StartMigrationValidationTest.TestData.invalidMongoProperties
import pl.allegro.tech.mongomigrationstream.test.preconditions.StartMigrationValidationTest.TestData.invalidPerformerProperties

internal class StartMigrationValidationTest : ShouldSpec({
    should("not perform migration when source db is not available") {
        // given: Invalid source URI
        val sourceProperties = invalidMongoProperties()

        // when: validating if migration can be performed
        val result = assertThrows<MongoMigrationStreamStartException> {
            createMongoMigrationStream(sourceDbProperties = sourceProperties).start()
        }

        // then: should fail
        result.cause!!.message.shouldContain("Database is not available")
    }

    should("not perform migration when destination db is not available") {
        // given: Invalid destination URI
        val destinationProperties = invalidMongoProperties()

        // when: validating if migration can be performed
        val result = assertThrows<MongoMigrationStreamStartException> {
            createMongoMigrationStream(destinationDbProperties = destinationProperties)
                .start()
        }

        // then: should fail
        result.cause!!.message.shouldContain("Database is not available")
    }

    should("not perform migration when source collection does not exist") {
        // given: Non-existing source collection
        val collectionsProperties = CollectionsProperties(
            sourceCollections = listOf("nonExistingCollection"),
            destinationCollections = listOf("collection1ChangedName")
        )

        // when: validating if migration can be performed
        val result = assertThrows<MongoMigrationStreamStartException> {
            createMongoMigrationStream(collectionsProperties = collectionsProperties)
                .start()
        }

        // then: should fail
        result.cause!!.message.shouldContain("Non-existing collections: [[nonExistingCollection]]")
    }

    should("not perform migration when database tools aren't available") {
        // given
        val performerProperties = invalidPerformerProperties()
        val generalProperties = generalPropertiesWithDBToolsValidator()

        // when
        val result = assertThrows<MongoMigrationStreamStartException> {
            createMongoMigrationStream(
                generalProperties = generalProperties,
                performerProperties = performerProperties
            ).start()
        }

        // then: should fail
        result.cause!!.message.shouldContain("MongoDB tools installation isn't working or doesn't exist")
    }
}) {
    private object TestData {
        fun invalidMongoProperties(): MongoProperties {
            val invalidUri =
                "mongodb://non_existing_host:36301,non_existing_host:36302,non_existing_host:36303/?replicaSet=invalidReplicaSet"
            return MongoProperties(
                uri = invalidUri,
                dbName = "test",
                authenticationProperties = null
            )
        }

        fun generalPropertiesWithDBToolsValidator(): GeneralProperties =
            GeneralProperties(
                shouldPerformTransfer = true,
                shouldPerformSynchronization = true,
                synchronizationHandlers = setOf(LoggingDetectionResultHandler),
                synchronizationDetectors = setOf(
                    GeneralProperties.DbHashSynchronizationDetectorType,
                    GeneralProperties.QueueSizeSynchronizationDetectorType,
                    GeneralProperties.CollectionCountSynchronizationDetectorType,
                ),
                databaseValidators = setOf(
                    GeneralProperties.MongoToolsValidatorType
                )
            )

        fun invalidPerformerProperties() = PerformerProperties(
            rootPath = MongoMigrationStreamTestProperties.ROOT_PATH,
            mongoToolsPath = MongoMigrationStreamTestProperties.MONGO_TOOLS_PATH,
            queueFactory = PerformerProperties.BiqQueueFactoryType,
            dumpReadPreference = ReadPreference.primary(),
            batchSizeProvider = ConstantValueBatchSizeProvider(1000),
            isCompressionEnabled = false,
            insertionWorkersPerCollection = 1,
        )
    }
}
