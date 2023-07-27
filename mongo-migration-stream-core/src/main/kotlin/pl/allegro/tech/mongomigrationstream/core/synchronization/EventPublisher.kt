package pl.allegro.tech.mongomigrationstream.core.synchronization

import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.BulkWriteOptions
import org.bson.BsonDocument

internal class EventPublisher(
    private val destinationCollection: MongoCollection<BsonDocument>
) {

    internal fun publishBulkEvents(events: List<ChangeEvent>): BulkWriteResult {
        val bulkEvents = events.mapNotNull { it.toWriteModel() }
        return destinationCollection.bulkWrite(bulkEvents, BulkWriteOptions().ordered(true))
    }
}
