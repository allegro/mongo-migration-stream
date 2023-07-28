package pl.allegro.tech.mongomigrationstream.test.synchronization

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.awaitility.kotlin.await
import pl.allegro.tech.mongomigrationstream.configuration.CollectionsProperties
import pl.allegro.tech.mongomigrationstream.configuration.MongoMigrationStreamTestConfig
import pl.allegro.tech.mongomigrationstream.utils.destinationCollection
import pl.allegro.tech.mongomigrationstream.utils.destinationDb
import pl.allegro.tech.mongomigrationstream.utils.multipleTestDocuments
import pl.allegro.tech.mongomigrationstream.utils.sourceCollection
import pl.allegro.tech.mongomigrationstream.utils.sourceDb
import pl.allegro.tech.mongomigrationstream.utils.testDocument
import java.util.concurrent.TimeUnit.SECONDS

internal class SynchronizationConfigurationTest : ShouldSpec({
    should("only synchronize collection when transfer disabled") {
        // given: non-empty collection in source Mongo
        val collectionName = "onlySynchronizeCollection"
        sourceDb.createCollection(collectionName)

        with(sourceDb.getCollection(collectionName)) {
            insertMany(multipleTestDocuments)
        }

        val documentsInSourceCollection = sourceCollection(collectionName).find().toList()
        documentsInSourceCollection.size.shouldBe(2)

        // when: starting migration with disabled transfer
        val mms = MongoMigrationStreamTestConfig.createMongoMigrationStream(
            generalProperties = MongoMigrationStreamTestConfig.defaultGeneralProperties().copy(
                shouldPerformTransfer = false,
                shouldPerformSynchronization = true
            ),
            collectionsProperties = CollectionsProperties(
                listOf(collectionName),
                listOf(collectionName)
            )
        )
        mms.start()

        // then: should not create collection on destination Mongo
        await.during(5, SECONDS).until { destinationDb.listCollectionNames().none { it == collectionName } }

        // when: event on source Mongo
        with(sourceDb.getCollection(collectionName)) { insertOne(testDocument) }

        // then: should create collection on destination Mongo
        await.atMost(5, SECONDS).until { destinationDb.listCollectionNames().any { it == collectionName } }

        // and: event should be synchronized to destination Mongo
        destinationCollection(collectionName).countDocuments().shouldBe(1L)

        mms.stop()
    }
})
