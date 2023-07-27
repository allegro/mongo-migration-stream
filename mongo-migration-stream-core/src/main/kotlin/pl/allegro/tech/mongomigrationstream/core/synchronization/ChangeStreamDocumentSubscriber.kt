package pl.allegro.tech.mongomigrationstream.core.synchronization

import com.mongodb.client.model.changestream.ChangeStreamDocument
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import org.bson.BsonDocument
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import pl.allegro.tech.mongomigrationstream.core.metrics.MigrationMetrics
import pl.allegro.tech.mongomigrationstream.core.mongo.SourceToDestination
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.FailedEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateInfo

private val logger = KotlinLogging.logger {}

internal class ChangeStreamDocumentSubscriber(
    private val sourceToDestination: SourceToDestination,
    private val stateInfo: StateInfo,
    private val eventConsumer: EventConsumer,
    meterRegistry: MeterRegistry,
) : Subscriber<ChangeStreamDocument<BsonDocument>> {
    private lateinit var subscription: Subscription
    private val dbCollection = sourceToDestination.source
    private val counter = Counter.builder(MigrationMetrics.CHANGE_EVENT_COUNTER)
        .description("Stores how many change event were processed by ChangeStreamDocumentSubscriber from source Mongo change stream")
        .tags(
            "database", dbCollection.dbName,
            "collection", dbCollection.collectionName
        ).register(meterRegistry)

    override fun onSubscribe(s: Subscription) {
        logger.info { "Starting subscriber of dbCollection: [$dbCollection]" }
        subscription = s
        subscription.request(Long.MAX_VALUE)
    }

    override fun onNext(rawEvent: ChangeStreamDocument<BsonDocument>) {
        counter.increment()
        eventConsumer.saveEventToLocalQueue(ChangeEvent.fromMongoChangeStreamDocument(rawEvent))
    }

    override fun onError(cause: Throwable) {
        logger.error { cause.message }
        stateInfo.notifyStateChange(FailedEvent(sourceToDestination, cause))
        onComplete()
    }

    override fun onComplete() {
        logger.info { "Cancelling subscriber of dbCollection: [$dbCollection]" }
        subscription.cancel()
    }
}
