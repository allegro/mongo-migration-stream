package pl.allegro.tech.mongomigrationstream.test.preconditions

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.assertThrows
import pl.allegro.tech.mongomigrationstream.configuration.CollectionsProperties
import pl.allegro.tech.mongomigrationstream.configuration.MongoMigrationStreamTestConfig.createMongoMigrationStream
import pl.allegro.tech.mongomigrationstream.configuration.MongoProperties
import pl.allegro.tech.mongomigrationstream.infrastructure.controller.MongoMigrationStreamStartException
import pl.allegro.tech.mongomigrationstream.test.preconditions.StartMigrationValidationTest.TestData.invalidMongoProperties

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
    }
}
