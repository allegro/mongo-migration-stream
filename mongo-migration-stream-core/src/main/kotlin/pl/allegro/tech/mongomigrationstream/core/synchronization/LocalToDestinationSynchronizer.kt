package pl.allegro.tech.mongomigrationstream.core.synchronization

import com.mongodb.MongoException
import com.mongodb.client.MongoDatabase
import dev.failsafe.Failsafe
import dev.failsafe.RetryPolicy
import dev.failsafe.Timeout
import dev.failsafe.function.CheckedRunnable
import io.github.oshai.kotlinlogging.KotlinLogging
import org.bson.BsonDocument
import pl.allegro.tech.mongomigrationstream.core.concurrency.MigrationExecutors
import pl.allegro.tech.mongomigrationstream.core.mongo.SourceToDestination
import pl.allegro.tech.mongomigrationstream.core.performer.ResumableSynchronizer
import pl.allegro.tech.mongomigrationstream.core.performer.SynchronizationResult
import pl.allegro.tech.mongomigrationstream.core.performer.SynchronizationSuccess
import pl.allegro.tech.mongomigrationstream.core.queue.EventQueue
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.FailedEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.LocalToDestinationStartEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.PauseEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.ResumeEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateInfo
import pl.allegro.tech.mongomigrationstream.core.synchronization.SynchronizationParameters.BULK_WRITE_BACKOFF_MAX_DELAY
import pl.allegro.tech.mongomigrationstream.core.synchronization.SynchronizationParameters.BULK_WRITE_BACKOFF_MIN_DELAY
import pl.allegro.tech.mongomigrationstream.core.synchronization.SynchronizationParameters.BULK_WRITE_TIMEOUT
import pl.allegro.tech.mongomigrationstream.core.synchronization.SynchronizationParameters.INFINITE_RETRIES
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger { }

internal class LocalToDestinationSynchronizer(
    private val destinationDb: MongoDatabase,
    private val sourceToDestination: SourceToDestination,
    private val queue: EventQueue<ChangeEvent>,
    private val stateInfo: StateInfo,
    private val batchSizeProvider: BatchSizeProvider
) : ResumableSynchronizer {
    private val executor: ExecutorService = MigrationExecutors.createLocalToDestinationExecutor(sourceToDestination.source)
    private val shouldSynchronize: AtomicBoolean = AtomicBoolean(true)
    private val isPaused: AtomicBoolean = AtomicBoolean(false)

    private val timeoutPolicy = Timeout.builder<Nothing>(BULK_WRITE_TIMEOUT).build()
    private val retryPolicy = RetryPolicy.builder<Nothing>()
        .handle(listOf(MongoException::class.java, Exception::class.java))
        .withBackoff(
            BULK_WRITE_BACKOFF_MIN_DELAY,
            BULK_WRITE_BACKOFF_MAX_DELAY
        ) // Delays between failed batches: [1m, 2m, 4m, 8m, 16m, 32m, 64m, 128m, 256m]
        .withMaxDuration(BULK_WRITE_BACKOFF_MAX_DELAY)
        .withMaxRetries(INFINITE_RETRIES)
        .onFailedAttempt { logger.error(it.lastException) { "Unable to send batch to destination Mongo" } }
        .onRetry { logger.warn { "Retrying to send batch to destination Mongo, attempt number: [${it.attemptCount}]" } }
        .build()

    override fun startSynchronization(): SynchronizationResult {
        logger.info { "Starting LocalToDestination synchronization for collection: [$sourceToDestination]" }
        stateInfo.notifyStateChange(LocalToDestinationStartEvent(sourceToDestination))
        startCollectionSynchronization(
            EventPublisher(
                destinationDb.getCollection(
                    sourceToDestination.destination.collectionName,
                    BsonDocument::class.java
                )
            ),
            queue,
            sourceToDestination
        )

        return SynchronizationSuccess
    }

    override fun pause() {
        stateInfo.notifyStateChange(PauseEvent(sourceToDestination))
        isPaused.set(true)
    }

    override fun resume() {
        stateInfo.notifyStateChange(ResumeEvent(sourceToDestination))
        isPaused.set(false)
    }

    private fun startCollectionSynchronization(
        eventPublisher: EventPublisher,
        queue: EventQueue<ChangeEvent>,
        sourceToDestination: SourceToDestination
    ) {
        executor.execute {
            while (shouldSynchronize.get()) {
                if (!isPaused.get()) {
                    handleEventsFromQueue(queue, eventPublisher, sourceToDestination)
                }
            }
        }
    }

    private fun handleEventsFromQueue(
        queue: EventQueue<ChangeEvent>,
        eventPublisher: EventPublisher,
        sourceToDestination: SourceToDestination
    ) {
        runCatching {
            val batch = pollBatchFromQueue(queue)
            if (batch.isNotEmpty()) {
                tryToSendBatchWithRetryAndTimeout(batch, eventPublisher)
            }
        }.onFailure {
            logger.error(it) { "Error during local to destination synchronization for [$sourceToDestination]. Stopping synchronization for this collection" }
            handleFail(it)
        }
    }

    private fun tryToSendBatchWithRetryAndTimeout(
        batch: List<ChangeEvent>,
        eventPublisher: EventPublisher
    ) {
        Failsafe
            .with(retryPolicy)
            .compose(timeoutPolicy)
            .run(
                CheckedRunnable {
                    measureTimeMillis {
                        eventPublisher.publishBulkEvents(batch)
                    }.let {
                        logger.info { "Sending batch of size: [${batch.size}] to destination mongo took: [$it ms]" }
                    }
                }
            )
    }

    private fun pollBatchFromQueue(queue: EventQueue<ChangeEvent>): List<ChangeEvent> {
        val queueSize = queue.size()
        val batchSize = batchSizeProvider.getBatchSize()
        return when {
            queueSize == 0 -> emptyList()
            queueSize <= batchSize -> pollNElementsFromQueue(queueSize, queue)
            else -> pollNElementsFromQueue(batchSize, queue)
        }
    }

    private fun pollNElementsFromQueue(amount: Int, queue: EventQueue<ChangeEvent>): List<ChangeEvent> {
        logger.debug { "Polling [$amount] elements from queue" }
        return List(amount) { queue.poll() }
    }

    private fun handleFail(throwable: Throwable) {
        stateInfo.notifyStateChange(FailedEvent(sourceToDestination, throwable))
        stop()
    }

    override fun stop() {
        logger.info { "Trying to shut down LocalToDestinationSynchronizer gracefully..." }
        try {
            shouldSynchronize.set(false)
            queue.removeAll()
            executor.shutdown()
        } catch (throwable: Throwable) {
            logger.warn(throwable) { "Exception while shutting down LocalToDestinationSynchronizer" }
        } finally {
            queue.removeAll()
            logger.info { "Shut down LocalToDestinationSynchronizer" }
        }
    }
}
