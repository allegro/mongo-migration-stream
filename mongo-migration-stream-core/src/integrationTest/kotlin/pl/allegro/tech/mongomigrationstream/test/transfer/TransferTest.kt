package pl.allegro.tech.mongomigrationstream.test.transfer

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.awaitility.kotlin.await
import pl.allegro.tech.mongomigrationstream.configuration.CollectionsProperties
import pl.allegro.tech.mongomigrationstream.configuration.MongoMigrationStreamTestConfig
import pl.allegro.tech.mongomigrationstream.utils.destinationCollection
import pl.allegro.tech.mongomigrationstream.utils.destinationDb
import pl.allegro.tech.mongomigrationstream.utils.multipleTestDocuments
import pl.allegro.tech.mongomigrationstream.utils.sameCollectionMongoMigrationStream
import pl.allegro.tech.mongomigrationstream.utils.sourceCollection
import pl.allegro.tech.mongomigrationstream.utils.sourceDb
import pl.allegro.tech.mongomigrationstream.utils.testDocument
import java.util.concurrent.TimeUnit.SECONDS

internal class TransferTest : ShouldSpec({
    should("transfer empty collection") {
        // given: empty collection in source Mongo
        val collectionName = "emptyCollection"
        sourceDb.createCollection(collectionName)

        // when: starting transfer
        val mms = sameCollectionMongoMigrationStream(collectionName)
        mms.start()

        // then: should create empty collection
        await.atMost(5, SECONDS).until { destinationDb.listCollectionNames().contains(collectionName) }

        mms.stop()
    }

    should("transfer collection with weird but valid name") {
        // given: empty collection in source Mongo
        val collectionName = "aaAA-1234_xdxdx_2021-03-24T07:19:32.954Z"
        sourceDb.createCollection(collectionName)

        // when: starting transfer
        val mms = sameCollectionMongoMigrationStream(collectionName)
        mms.start()

        // then: should create empty collection
        await.atMost(5, SECONDS).until { destinationDb.listCollectionNames().contains(collectionName) }

        mms.stop()
    }

    should("transfer collection having some elements") {
        // given: collection with 2 elements in source Mongo
        val collectionName = "collectionWithSomeElements"
        sourceDb.createCollection(collectionName)

        with(sourceDb.getCollection(collectionName)) {
            insertMany(multipleTestDocuments)
        }

        val documentsInSourceCollection = sourceCollection(collectionName).find().toList()
        documentsInSourceCollection.size.shouldBe(2)

        // when: starting transfer
        val mms = sameCollectionMongoMigrationStream(collectionName)
        mms.start()

        // then: should create collection
        await.atMost(5, SECONDS).until { destinationDb.listCollectionNames().contains(collectionName) }
        await.atMost(5, SECONDS).until {
            destinationCollection(collectionName).countDocuments() == multipleTestDocuments.size.toLong()
        }

        // and: created collection should contain same elements as source collection
        destinationCollection(collectionName).find().toList().shouldContainExactlyInAnyOrder(documentsInSourceCollection)

        mms.stop()
    }

    should("change collection name during transfer") {
        // given: different collection names
        val oldCollection = "oldCollection"
        val newCollection = "newCollection"
        sourceDb.createCollection(oldCollection)

        // when: starting transfer
        val mms = MongoMigrationStreamTestConfig.createMongoMigrationStream(
            collectionsProperties = CollectionsProperties(
                listOf(oldCollection),
                listOf(newCollection)
            )
        )
        mms.start()

        // then: should create collection with new name
        await.atMost(5, SECONDS).until { destinationDb.listCollectionNames().contains(newCollection) }

        mms.stop()
    }

    should("transfer multiple collections") {
        // given: multiple collections in source Mongo
        val collection1 = "collection1"
        val collection2 = "collection2"
        sourceDb.createCollection(collection1)
        sourceDb.createCollection(collection2)

        // when: starting transfer
        val mms = MongoMigrationStreamTestConfig.createMongoMigrationStream(
            collectionsProperties = CollectionsProperties(
                listOf(collection1, collection2),
                listOf(collection1, collection2)
            )
        )
        mms.start()

        // then: should create collection in destination Mongo
        await.atMost(5, SECONDS).until {
            with(destinationDb.listCollectionNames().toList()) {
                contains(collection1) && contains(collection2)
            }
        }

        mms.stop()
    }

    should("only transfer collection when synchronization disabled") {
        // given: empty collection in source Mongo
        val collectionName = "onlyTransferCollection"
        sourceDb.createCollection(collectionName)

        // when: starting migration with disabled synchronization
        val mms = MongoMigrationStreamTestConfig.createMongoMigrationStream(
            generalProperties = MongoMigrationStreamTestConfig.defaultGeneralProperties().copy(
                shouldPerformTransfer = true,
                shouldPerformSynchronization = false
            ),
            collectionsProperties = CollectionsProperties(
                listOf(collectionName),
                listOf(collectionName)
            )
        )
        mms.start()

        // then: should create empty collection
        await.atMost(5, SECONDS).until { destinationDb.listCollectionNames().contains(collectionName) }
        destinationCollection(collectionName).countDocuments().shouldBe(0L)

        // when: event on source Mongo
        with(sourceDb.getCollection(collectionName)) { insertOne(testDocument) }

        // then: it should not be synchronized to destination Mongo
        await.during(5, SECONDS).until {
            destinationCollection(collectionName).countDocuments() == 0L
        }

        mms.stop()
    }
})
