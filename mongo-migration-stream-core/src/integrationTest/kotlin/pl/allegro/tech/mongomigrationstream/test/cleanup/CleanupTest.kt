package pl.allegro.tech.mongomigrationstream.test.cleanup

import com.mongodb.ReadPreference
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import org.awaitility.kotlin.await
import org.bson.Document
import pl.allegro.tech.mongomigrationstream.configuration.CollectionsProperties
import pl.allegro.tech.mongomigrationstream.configuration.MongoMigrationStreamTestConfig
import pl.allegro.tech.mongomigrationstream.configuration.PerformerProperties
import pl.allegro.tech.mongomigrationstream.core.synchronization.ConstantValueBatchSizeProvider
import pl.allegro.tech.mongomigrationstream.utils.destinationCollection
import pl.allegro.tech.mongomigrationstream.utils.sourceCollection
import pl.allegro.tech.mongomigrationstream.utils.sourceDb
import java.io.File
import java.util.concurrent.TimeUnit

internal class CleanupTest : ShouldSpec({
    listOf(
        "dumps",
        "queues"
    ).forEach { element ->
        should("remove $element after successful migration") {
            // given: empty collection in source Mongo and performed migration
            val collectionName = "${element}EventCollection"
            sourceDb.createCollection(collectionName)

            val path = "/tmp/mongo-migration-stream-test-cleanup-$element"
            val mms = MongoMigrationStreamTestConfig.createMongoMigrationStream(
                collectionsProperties = CollectionsProperties(
                    listOf(collectionName),
                    listOf(collectionName)
                ),
                performerProperties = PerformerProperties(
                    rootPath = path,
                    mongoToolsPath = "",
                    queueFactory = PerformerProperties.BiqQueueFactoryType,
                    dumpReadPreference = ReadPreference.primary(),
                    batchSizeProvider = ConstantValueBatchSizeProvider(1000),
                    isCompressionEnabled = false,
                    insertionWorkersPerCollection = 1
                )
            )
            mms.start()

            // when: successful migration and synchronization
            sourceCollection(collectionName).insertOne(Document("key", "value"))
            with(destinationCollection(collectionName)) {
                await.atMost(5, TimeUnit.SECONDS).until { countDocuments() == 1L }
            }
            mms.stop()

            // then: should remove leftovers
            File(path).resolve(element).list().shouldBeEmpty()
        }
    }
})
