package pl.allegro.tech.mongomigrationstream.test.index

import com.mongodb.client.model.IndexModel
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import org.awaitility.kotlin.await
import org.bson.Document
import pl.allegro.tech.mongomigrationstream.utils.destinationCollection
import pl.allegro.tech.mongomigrationstream.utils.multipleTestDocuments
import pl.allegro.tech.mongomigrationstream.utils.sameCollectionMongoMigrationStream
import pl.allegro.tech.mongomigrationstream.utils.sourceCollection
import pl.allegro.tech.mongomigrationstream.utils.sourceDb
import java.util.concurrent.TimeUnit

internal class IndexClonerTest : ShouldSpec({
    should("migrate collection with indexes") {
        // given: collection with indexes in source Mongo
        val collectionName = "collectionWithIndexes"
        sourceDb.createCollection(collectionName)
        sourceCollection(collectionName).createIndexes(
            listOf(
                IndexModel(
                    Indexes.ascending("key1"),
                    IndexOptions()
                        .name("key1Index")
                        .background(false)
                        .hidden(false)
                        .sparse(true)
                ),
                IndexModel(
                    Indexes.ascending("key2"),
                    IndexOptions()
                        .name("key2Index")
                        .background(true)
                )
            )
        )

        with(sourceDb.getCollection(collectionName)) { insertMany(multipleTestDocuments) }
        val documentsInSourceCollection = sourceCollection(collectionName).find().toList()
        documentsInSourceCollection shouldHaveSize (2)

        // when: starting transfer
        val mms = sameCollectionMongoMigrationStream(collectionName)
        mms.start()

        // then: should create indexes
        await.atMost(5, TimeUnit.SECONDS).until {
            val destinationIndexes = destinationCollection(collectionName).listIndexes().toList()
            destinationIndexes.filter { it.getString("name") != "_id_" }.size == 2
        }

        destinationCollection(collectionName).listIndexes().toList().map { unifyIndexBackground(it) }.shouldContainExactlyInAnyOrder(
            sourceCollection(collectionName).listIndexes().toList().map { unifyIndexBackground(it) }
        )

        mms.stop()
    }

    should("set background:true when cloning indexes") {
        // given: collection with background:false index in source Mongo
        val collectionName = "collectionWithBackgroundFalseIndex"
        sourceDb.createCollection(collectionName)
        sourceCollection(collectionName).createIndexes(
            listOf(
                IndexModel(
                    Indexes.ascending("key1"),
                    IndexOptions()
                        .name("key1Index")
                        .background(false)
                )
            )
        )

        with(sourceDb.getCollection(collectionName)) { insertMany(multipleTestDocuments) }
        val documentsInSourceCollection = sourceCollection(collectionName).find().toList()
        documentsInSourceCollection shouldHaveSize (2)

        // when: starting transfer
        val mms = sameCollectionMongoMigrationStream(collectionName)
        mms.start()

        // then: cloned index should have background:true
        await.atMost(5, TimeUnit.SECONDS).until {
            val destinationIndexes = destinationCollection(collectionName).listIndexes().toList()
            destinationIndexes.filter { it.getString("name") != "_id_" }.size == 1
        }

        destinationCollection(collectionName).listIndexes().toList()
            .filter { it.getString("name") != "_id_" }
            .all { it.getBoolean("background") }
            .shouldBeTrue()

        mms.stop()
    }

    should("not migrate incompatible indexes").config(enabled = false) {
        // given: collection with geoHaystack index in source Mongo 3.6
        val collectionName = "collectionWithGeoHaystackIndex"
        sourceDb.createCollection(collectionName)
        sourceCollection(collectionName).createIndexes(
            listOf(
                IndexModel(
                    Indexes.geoHaystack("gh", Indexes.ascending("stars")),
                    IndexOptions().bucketSize(1.0).name("geoHaystack")
                )
            )
        )

        with(sourceDb.getCollection(collectionName)) { insertMany(multipleTestDocuments) }
        val documentsInSourceCollection = sourceCollection(collectionName).find().toList()
        documentsInSourceCollection shouldHaveSize (2)

        // when: starting transfer
        val mms = sameCollectionMongoMigrationStream(collectionName)
        mms.start()

        // then: should not create geoHaystack index in destination Mongo
        await.atMost(5, TimeUnit.SECONDS).until {
            destinationCollection(collectionName).countDocuments() == 2L
        }

        destinationCollection(collectionName).listIndexes().toList().filter { it.getString("name") == "geoHaystack" }
            .shouldBeEmpty()

        mms.stop()
    }
})

private fun unifyIndex(document: Document): Document = document.apply { remove("ns") }
private fun unifyIndexBackground(document: Document): Document = unifyIndex(document).apply { remove("background") }
