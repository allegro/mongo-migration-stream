package pl.allegro.tech.mongomigrationstream.test.pause

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.awaitility.kotlin.await
import org.bson.Document
import pl.allegro.tech.mongomigrationstream.utils.createEmptyCollectionAndPerformMigration
import pl.allegro.tech.mongomigrationstream.utils.destinationCollection
import pl.allegro.tech.mongomigrationstream.utils.sourceDb
import java.util.concurrent.TimeUnit.SECONDS

internal class PauseMigrationTest : ShouldSpec({
    should("pause and resume migration") {
        // given: no events from source Mongo
        val collectionName = "pauseAndResumeCollectionWithNoEvents"

        // when: migration succeeded
        val mms = createEmptyCollectionAndPerformMigration(collectionName)

        // then: should be no documents on destination collection
        destinationCollection(collectionName).countDocuments().shouldBe(0)

        // when: inserting one document
        with(sourceDb.getCollection(collectionName)) { insertOne(Document().append("key", "1")) }

        // then: destination Mongo should have inserted 1 document
        await.atMost(5, SECONDS).until { destinationCollection(collectionName).countDocuments() == 1L }

        // when: pausing migration
        mms.pause()

        // and: inserting one document into source
        with(sourceDb.getCollection(collectionName)) { insertOne(Document().append("key", "2")) }

        // then: during 5 seconds destination Mongo collection size should still equal to 1
        await.during(5, SECONDS).until { destinationCollection(collectionName).countDocuments() == 1L }

        // when: resuming migration
        mms.resume()

        // then: should send event to destination Mongo
        await.until {
            destinationCollection(collectionName).countDocuments() == 2L
        }

        mms.stop()
    }
})
