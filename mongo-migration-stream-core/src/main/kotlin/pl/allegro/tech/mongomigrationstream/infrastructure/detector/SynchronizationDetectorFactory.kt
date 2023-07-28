package pl.allegro.tech.mongomigrationstream.infrastructure.detector

import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import pl.allegro.tech.mongomigrationstream.configuration.ApplicationProperties
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties.CollectionCountSynchronizationDetectorType
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties.DbHashSynchronizationDetectorType
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties.QueueSizeSynchronizationDetectorType
import pl.allegro.tech.mongomigrationstream.core.concurrency.MigrationExecutors
import pl.allegro.tech.mongomigrationstream.core.detector.CollectionCountSynchronizationDetector
import pl.allegro.tech.mongomigrationstream.core.detector.HashSynchronizationDetector
import pl.allegro.tech.mongomigrationstream.core.detector.QueueSizeSynchronizationDetector
import pl.allegro.tech.mongomigrationstream.core.detector.SynchronizationDetector
import pl.allegro.tech.mongomigrationstream.core.detector.handler.DetectionResultHandler
import pl.allegro.tech.mongomigrationstream.core.mongo.SourceToDestination
import pl.allegro.tech.mongomigrationstream.core.queue.EventQueue
import pl.allegro.tech.mongomigrationstream.core.synchronization.ChangeEvent
import pl.allegro.tech.mongomigrationstream.infrastructure.mongo.MongoDbClients
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger { }
private const val INITIAL_DELAY_SECONDS = 0L
private const val FREQUENCY_SECONDS = 10L

internal class SynchronizationDetectorFactory(
    private val properties: ApplicationProperties,
    private val mongoDbClients: MongoDbClients,
    private val queues: Map<SourceToDestination, EventQueue<ChangeEvent>>,
    private val meterRegistry: MeterRegistry
) {
    private val finishedTransfers = AtomicInteger(0)
    private val synchronizationDetectorExecutor = MigrationExecutors.createSynchronizationDetectorExecutor()
    private val scheduledSynchronizationDetectorExecutor = MigrationExecutors.createScheduledSynchronizationDetectorExecutor()

    fun tryToStartDetectingSynchronization() {
        finishedTransfers.getAndUpdate {
            if (it + 1 == properties.sourceToDestinationMapping.size) {
                startDetectingSynchronization()
            }
            it + 1
        }
    }

    private fun startDetectingSynchronization() {
        logger.info { "Starting detecting synchronization..." }

        val detectors = createDetectors(properties)
        val handlers = properties.generalProperties.synchronizationHandlers

        scheduledSynchronizationDetectorExecutor.scheduleAtFixedRate(
            { detectAndHandleSynchronization(detectors, handlers) },
            INITIAL_DELAY_SECONDS,
            FREQUENCY_SECONDS,
            SECONDS
        )
    }

    private fun detectAndHandleSynchronization(
        detectors: Set<SynchronizationDetector>,
        handlers: Set<DetectionResultHandler>
    ) {
        runCatching {
            detectors
                .map { CompletableFuture.supplyAsync({ it.detect() }, synchronizationDetectorExecutor) }
                .flatMap { it.join() }
        }.onFailure { logger.warn(it) { "Cannot detect synchronization" } }
            .map { results ->
                handlers.forEach { handler ->
                    results.forEach { result ->
                        handler.handle(result)
                    }
                }
            }.onFailure { logger.warn(it) { "Cannot handle synchronization" } }
    }

    private fun createDetectors(properties: ApplicationProperties): Set<SynchronizationDetector> {
        val sourceDb = mongoDbClients.sourceDatabase
        val destinationDb = mongoDbClients.destinationDatabase
        val collectionMapping = properties.sourceToDestinationMapping

        val hashDetector =
            HashSynchronizationDetector(sourceDb, destinationDb, collectionMapping, synchronizationDetectorExecutor)
        val queueSizeDetector =
            QueueSizeSynchronizationDetector(collectionMapping, queues, synchronizationDetectorExecutor, meterRegistry)
        val collectionCountDetector =
            CollectionCountSynchronizationDetector(
                sourceDb,
                destinationDb,
                collectionMapping,
                synchronizationDetectorExecutor,
                meterRegistry
            )

        return properties.generalProperties.synchronizationDetectors.map {
            when (it) {
                DbHashSynchronizationDetectorType -> hashDetector
                CollectionCountSynchronizationDetectorType -> collectionCountDetector
                QueueSizeSynchronizationDetectorType -> queueSizeDetector
            }
        }.toSet()
    }

    fun stopDetectingSynchronization() {
        try {
            scheduledSynchronizationDetectorExecutor.shutdown()
            synchronizationDetectorExecutor.shutdown()
        } catch (throwable: Throwable) {
            logger.warn(throwable) { "Exception while shutting down SynchronizationDetectorFactory" }
        } finally {
            logger.info { "Shut down SynchronizationDetectorFactory" }
        }
    }
}
