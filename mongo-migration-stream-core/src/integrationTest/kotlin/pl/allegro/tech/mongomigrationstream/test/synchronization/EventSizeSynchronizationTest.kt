package pl.allegro.tech.mongomigrationstream.test.synchronization

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.booleans.shouldBeTrue
import org.awaitility.kotlin.await
import org.bson.BsonBinary
import org.bson.Document
import pl.allegro.tech.mongomigrationstream.utils.createEmptyCollectionAndPerformMigration
import pl.allegro.tech.mongomigrationstream.utils.destinationCollection
import pl.allegro.tech.mongomigrationstream.utils.sourceCollection
import pl.allegro.tech.mongomigrationstream.utils.validateHashEquality
import java.util.concurrent.TimeUnit

private const val ONE_MEGABYTE = 1_048_576
private const val SIXTEEN_MEGABYTES = 16 * ONE_MEGABYTE

internal class EventSizeSynchronizationTest : ShouldSpec({

    should("synchronize event below 16MB") {
        // given: empty collection in source Mongo and performed migration
        val collectionName = "validSizeEventCollection"
        val mms = createEmptyCollectionAndPerformMigration(collectionName)

        // when: inserting valid size event in source Mongo
        sourceCollection(collectionName).insertOne(Document("key", BsonBinary(ByteArray(SIXTEEN_MEGABYTES - ONE_MEGABYTE) { 1 })))

        // then: should synchronize event in destination Mongo
        with(destinationCollection(collectionName)) {
            await.atMost(5, TimeUnit.SECONDS).until { countDocuments() == 1L }
        }

        // and: hash of source and destination collection should be the same
        validateHashEquality(collectionName).shouldBeTrue()

        mms.stop()
    }
})
