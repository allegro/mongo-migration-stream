package pl.allegro.tech.mongomigrationstream.core.synchronization

import com.mongodb.client.MongoCursor
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.changestream.ChangeStreamDocument
import com.mongodb.client.model.changestream.FullDocument
import com.mongodb.reactivestreams.client.ChangeStreamPublisher
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import org.bson.BsonDocument
import pl.allegro.tech.mongomigrationstream.core.concurrency.MigrationExecutors
import pl.allegro.tech.mongomigrationstream.core.mongo.DbCollection
import pl.allegro.tech.mongomigrationstream.core.mongo.SourceToDestination
import pl.allegro.tech.mongomigrationstream.core.performer.SynchronizationResult
import pl.allegro.tech.mongomigrationstream.core.performer.SynchronizationSuccess
import pl.allegro.tech.mongomigrationstream.core.performer.Synchronizer
import pl.allegro.tech.mongomigrationstream.core.queue.EventQueue
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.SourceToLocalStartEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateInfo
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger { }

internal class SourceToLocalSynchronizer(
    private val sourceToDestination: SourceToDestination,
    private val sourceDb: MongoDatabase,
    private val reactiveSourceDb: com.mongodb.reactivestreams.client.MongoDatabase,
    private val queue: EventQueue<ChangeEvent>,
    private val stateInfo: StateInfo,
    private val meterRegistry: MeterRegistry
) : Synchronizer {
    private val executor: ExecutorService = MigrationExecutors.createSourceToLocalExecutor(sourceToDestination.source)
    private val synchronizedOperations = setOf("insert", "update", "replace", "delete")
    private val shouldSynchronize: AtomicBoolean = AtomicBoolean(true)
    private val cursors = mutableListOf<MongoCursor<ChangeStreamDocument<BsonDocument>>>()
    private val reactiveSubscribers = mutableListOf<ChangeStreamDocumentSubscriber>()

    override fun startSynchronization(): SynchronizationResult {
        logger.info { "Starting SourceToLocal synchronization for collection: [${sourceToDestination.source}]" }
        stateInfo.notifyStateChange(StateEvent.StartEvent(sourceToDestination))

        performReactiveCollectionSynchronization(
            sourceToDestination,
            EventConsumer(queue, meterRegistry)
        ) // TODO: Now we're going to test reactive version of polling events from source mongo

        stateInfo.notifyStateChange(SourceToLocalStartEvent(sourceToDestination))
        return SynchronizationSuccess
    }

    private fun performReactiveCollectionSynchronization(sourceToDestination: SourceToDestination, eventConsumer: EventConsumer) {
        val changeStreamPublisher: ChangeStreamPublisher<BsonDocument> =
            reactiveSourceDb
                .getCollection(sourceToDestination.source.collectionName, BsonDocument::class.java)
                .watch(listOf(Aggregates.match(Filters.`in`("operationType", synchronizedOperations))), BsonDocument::class.java)
                .fullDocument(FullDocument.DEFAULT)

        val changeStreamSubscriber = ChangeStreamDocumentSubscriber(sourceToDestination, stateInfo, eventConsumer, meterRegistry)
        reactiveSubscribers.add(changeStreamSubscriber)

        changeStreamPublisher.subscribe(changeStreamSubscriber)
    }

    private fun performCollectionSynchronization(dbCollection: DbCollection, eventConsumer: EventConsumer) {
        val collectionCursor: MongoCursor<ChangeStreamDocument<BsonDocument>> =
            sourceDb.getCollection(dbCollection.collectionName, BsonDocument::class.java)
                .watch(listOf(Aggregates.match(Filters.`in`("operationType", synchronizedOperations))))
                .fullDocument(FullDocument.DEFAULT)
                .iterator()

        cursors.add(collectionCursor)

        executor.execute {
            while (shouldSynchronize.get()) {
                runCatching {
                    val event: ChangeStreamDocument<BsonDocument> = collectionCursor.next()
                    eventConsumer.saveEventToLocalQueue(ChangeEvent.fromMongoChangeStreamDocument(event))
                }.onFailure { logger.error(it) { "Error during source to local synchronization" } }
            }
        }
    }

    override fun stop() {
        logger.info { "Trying to shut down SourceToLocalSynchronizer gracefully..." }
        try {
            shouldSynchronize.set(false)
            cursors.forEach { it.close() }
            reactiveSubscribers.forEach { it.onComplete() }
            executor.shutdown()
        } catch (throwable: Throwable) {
            logger.warn(throwable) { "Exception while shutting down LocalToDestinationSynchronizer" }
        } finally {
            logger.info { "Shut down SourceToLocalSynchronizer" }
        }
    }
}
