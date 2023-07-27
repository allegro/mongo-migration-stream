package pl.allegro.tech.mongomigrationstream.test.synchronization

import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.awaitility.kotlin.await
import org.bson.Document
import pl.allegro.tech.mongomigrationstream.utils.createEmptyCollectionAndPerformMigration
import pl.allegro.tech.mongomigrationstream.utils.destinationCollection
import pl.allegro.tech.mongomigrationstream.utils.destinationDb
import pl.allegro.tech.mongomigrationstream.utils.multipleTestDocuments
import pl.allegro.tech.mongomigrationstream.utils.sourceCollection
import pl.allegro.tech.mongomigrationstream.utils.sourceDb
import pl.allegro.tech.mongomigrationstream.utils.testDocument
import java.util.concurrent.TimeUnit.SECONDS

internal class OperationSynchronizationTest : ShouldSpec({
    should("synchronize no events") {
        // given: no events from source Mongo
        val collectionName = "collectionWithNoEvents"

        // when: migration succeeded
        val mms = createEmptyCollectionAndPerformMigration(collectionName)

        // then: should be no documents on destination collection
        destinationCollection(collectionName).countDocuments().shouldBe(0)

        mms.stop()
    }

    should("synchronize insert event") {
        // given: collection in source Mongo
        val collectionName = "insertEvents"
        val mms = createEmptyCollectionAndPerformMigration(collectionName)

        // when: inserting data in source Mongo
        with(sourceDb.getCollection(collectionName)) { insertOne(testDocument) }

        val documentsInSourceCollection = sourceCollection(collectionName).find().toList()
        documentsInSourceCollection.size.shouldBe(1)

        // then: destination Mongo should have inserted document
        await.atMost(5, SECONDS).until { destinationCollection(collectionName).countDocuments() == 1L }
        destinationCollection(collectionName).find().toList().shouldContainExactlyInAnyOrder(documentsInSourceCollection)

        mms.stop()
    }

    should("synchronize update event") {
        // given: collection in source Mongo
        val collectionName = "updateEvents"
        val mms = createEmptyCollectionAndPerformMigration(collectionName)

        // when: updating data in source Mongo
        with(sourceDb.getCollection(collectionName)) {
            val id = insertOne(Document().append("key1", "value1")).insertedId?.asObjectId()?.value!!
            updateOne(Filters.eq(id), Updates.set("key1", "changedValue1"))
        }

        val documentsInSourceCollection = sourceCollection(collectionName).find().toList()
        documentsInSourceCollection.size.shouldBe(1)

        // then: destination Mongo should have updated document
        await.atMost(5, SECONDS).until { destinationCollection(collectionName).countDocuments() == 1L }
        with(destinationCollection(collectionName).find().toList()) {
            this.shouldContainExactlyInAnyOrder(documentsInSourceCollection)
            this.first().getString("key1").shouldBe("changedValue1")
        }

        mms.stop()
    }

    should("synchronize delete event") {
        // given: collection in source Mongo
        val collectionName = "deleteEvents"
        val mms = createEmptyCollectionAndPerformMigration(collectionName)

        // when: deleting data in source Mongo
        with(sourceDb.getCollection(collectionName)) {
            val id = insertMany(multipleTestDocuments).insertedIds.values.toList().first().asObjectId().value
            deleteOne(Filters.eq(id))
        }

        val documentsInSourceCollection = sourceCollection(collectionName).find().toList()
        documentsInSourceCollection.size.shouldBe(1)

        // then: destination Mongo should have deleted document
        await.atMost(5, SECONDS).until { destinationCollection(collectionName).countDocuments() == 1L }
        with(destinationCollection(collectionName).find().toList()) {
            this.shouldContainExactlyInAnyOrder(documentsInSourceCollection)
        }
        mms.stop()
    }

    should("synchronize upsert event") {
        // given: collection in source Mongo
        val collectionName = "upsertEvents"
        val mms = createEmptyCollectionAndPerformMigration(collectionName)

        // when: upsert data in source Mongo
        with(sourceDb.getCollection(collectionName)) {
            val id = insertOne(testDocument).insertedId?.asObjectId()?.value!!
            updateOne(Filters.eq(id), Updates.set("key1", "changedValue1"), UpdateOptions().upsert(true))
        }

        val documentsInSourceCollection = sourceCollection(collectionName).find().toList()
        documentsInSourceCollection.size.shouldBe(1)

        // then: destination Mongo should have upserted document
        await.atMost(5, SECONDS).until { destinationCollection(collectionName).countDocuments() == 1L }
        with(destinationCollection(collectionName).find().toList()) {
            this.shouldContainExactlyInAnyOrder(documentsInSourceCollection)
        }
        mms.stop()
    }

    should("not synchronize drop event") {
        // given: collection in source Mongo
        val collectionName = "dropEvents"
        val mms = createEmptyCollectionAndPerformMigration(collectionName)

        // when: drop collection in source Mongo
        sourceDb.getCollection(collectionName).drop()

        // then: destination Mongo should have not dropped collection
        destinationDb.listCollectionNames().shouldContain(collectionName)
        destinationCollection(collectionName).countDocuments().shouldBe(0)

        mms.stop()
    }
})
