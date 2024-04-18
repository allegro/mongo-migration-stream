package pl.allegro.tech.mongomigrationstream.core.synchronization

import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.BulkWriteOptions
import io.github.oshai.kotlinlogging.KotlinLogging
import org.bson.BsonDocument

private val logger = KotlinLogging.logger { }

internal class EventPublisher(
    private val destinationCollection: MongoCollection<BsonDocument>
) {

    internal fun publishBulkEvents(events: List<ChangeEvent>): BulkWriteResult {
        val bulkEvents = events.mapNotNull { it.toWriteModel() }
        logger.info{"Event: [$events] mapped to: [$bulkEvents]"}
        return BulkWriteResult.unacknowledged()

//        try {
//            val result = destinationCollection.bulkWrite(bulkEvents, BulkWriteOptions().ordered(true))
//            logger.info{"SUCCESS: [$events] mapped to: [$bulkEvents]"}
//            return result
//        } catch (e: Exception) {
//            logger.error{"FAILED: [$events] mapped to: [$bulkEvents]"}
//            return BulkWriteResult.unacknowledged()
//        }
    }
}
