package pl.allegro.tech.mongomigrationstream.test.synchronization

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.booleans.shouldBeTrue
import org.awaitility.kotlin.await
import org.bson.BsonArray
import org.bson.BsonBinary
import org.bson.BsonBoolean
import org.bson.BsonDateTime
import org.bson.BsonDecimal128
import org.bson.BsonDouble
import org.bson.BsonInt32
import org.bson.BsonInt64
import org.bson.BsonJavaScript
import org.bson.BsonMaxKey
import org.bson.BsonMinKey
import org.bson.BsonNull
import org.bson.BsonObjectId
import org.bson.BsonRegularExpression
import org.bson.BsonString
import org.bson.BsonTimestamp
import org.bson.Document
import org.bson.types.Decimal128
import pl.allegro.tech.mongomigrationstream.utils.createEmptyCollectionAndPerformMigration
import pl.allegro.tech.mongomigrationstream.utils.destinationCollection
import pl.allegro.tech.mongomigrationstream.utils.sourceCollection
import pl.allegro.tech.mongomigrationstream.utils.validateHashEquality
import java.util.concurrent.TimeUnit.SECONDS

internal class DocumentTypeSynchronizationTest : ShouldSpec({

    listOf(
        ("string" to Document().append("key", BsonString("value"))),
        ("int32" to Document().append("key", BsonInt32(123))),
        ("int64" to Document().append("key", BsonInt64(123456))),
        ("double" to Document().append("key", BsonDouble(1.2345))),
        ("object" to Document().append("key", Document().append("key", Document().append("nestedKey", "nestedValue")))),
        ("array" to Document().append("key", BsonArray(listOf(BsonInt64(1), BsonInt64(2), BsonInt64(3))))),
        ("binaryData" to Document().append("key", BsonBinary(ByteArray(10) { 1 }))),
        ("objectId" to Document().append("key", BsonObjectId())),
        ("boolean" to Document().append("key", BsonBoolean(true))),
        ("date" to Document().append("key", BsonDateTime(1667465316L))),
        ("null" to Document().append("key", BsonNull())),
        ("regularExpression" to Document().append("key", BsonRegularExpression("/d"))),
        ("javascript" to Document().append("key", BsonJavaScript("console.log('Hello, world!')"))),
        ("timestamp" to Document().append("key", BsonTimestamp(1667465316L))),
        ("decimal128" to Document().append("key", BsonDecimal128(Decimal128.POSITIVE_INFINITY))),
        ("minKey" to Document().append("key", BsonMinKey())),
        ("maxKey" to Document().append("key", BsonMaxKey()))
    ).forEach { (type: String, document: Document) ->
        should("synchronize $type event") {
            // given: empty collection in source Mongo and performed migration
            val collectionName = "${type}EventCollection"
            val mms = createEmptyCollectionAndPerformMigration(collectionName)

            // when: inserting event in source Mongo
            sourceCollection(collectionName).insertOne(document)

            // then: should synchronize event in destination Mongo
            with(destinationCollection(collectionName)) {
                await.atMost(5, SECONDS).until { countDocuments() == 1L }
            }

            // and: hash of source and destination collection should be the same
            validateHashEquality(collectionName).shouldBeTrue()

            mms.stop()
        }
    }
})
