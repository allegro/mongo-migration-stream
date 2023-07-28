package pl.allegro.tech.mongomigrationstream.utils

import com.mongodb.client.MongoDatabase
import org.awaitility.kotlin.await
import org.bson.Document
import pl.allegro.tech.mongomigrationstream.MongoMigrationStream
import pl.allegro.tech.mongomigrationstream.configuration.CollectionsProperties
import pl.allegro.tech.mongomigrationstream.configuration.IntegrationTestProjectConfig
import pl.allegro.tech.mongomigrationstream.configuration.MongoMigrationStreamTestConfig
import java.util.concurrent.TimeUnit

internal val sourceDb = IntegrationTestProjectConfig.mongoExtension.mongo36db()
internal val destinationDb = IntegrationTestProjectConfig.mongoExtension.mongo60db()
internal fun sourceCollection(collectionName: String) = sourceDb.getCollection(collectionName)
internal fun destinationCollection(collectionName: String) = destinationDb.getCollection(collectionName)
internal val testDocument = Document().append("key1", "value1")
internal val multipleTestDocuments = listOf(Document().append("key1", "value1"), Document().append("key2", "value2"))
internal fun sameCollectionMongoMigrationStream(collectionName: String) =
    MongoMigrationStreamTestConfig.createMongoMigrationStream(
        collectionsProperties = CollectionsProperties(
            listOf(collectionName),
            listOf(collectionName)
        )
    )

internal fun validateHashEquality(
    sourceCollectionName: String,
    destinationCollectionName: String = sourceCollectionName
): Boolean {
    fun hashForDbCollection(db: MongoDatabase, sourceCollectionName: String): String =
        db.runCommand(Document().append("dbHash", 1)).get("collections", Document::class.java).getString(sourceCollectionName)

    val sourceHash: String = hashForDbCollection(sourceDb, sourceCollectionName)
    val destinationHash: String = hashForDbCollection(destinationDb, destinationCollectionName)
    return sourceHash == destinationHash
}

internal fun createEmptyCollectionAndPerformMigration(collectionName: String): MongoMigrationStream {
    sourceDb.createCollection(collectionName)
    val mms = sameCollectionMongoMigrationStream(collectionName)
    mms.start()

    // TODO: Replace with event about full migration/starting synchronization
    await.atMost(5, TimeUnit.SECONDS).until { destinationDb.listCollectionNames().contains(collectionName) }

    return mms
}
