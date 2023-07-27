package pl.allegro.tech.mongomigrationstream.core.detector

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import mu.KotlinLogging
import pl.allegro.tech.mongomigrationstream.core.mongo.SourceToDestination
import pl.allegro.tech.mongomigrationstream.core.queue.EventQueue
import pl.allegro.tech.mongomigrationstream.core.synchronization.ChangeEvent
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger { }

internal class QueueSizeSynchronizationDetector(
    private val allCollectionsMapping: Set<SourceToDestination>,
    private val queues: Map<SourceToDestination, EventQueue<ChangeEvent>>,
    private val executor: Executor,
    private val meterRegistry: MeterRegistry
) : SynchronizationDetector {

    // {"<DBNAME>:<DST_COLLECTION>": <QUEUE_SIZE>}
    private val metrics = ConcurrentHashMap<String, AtomicInteger>()

    override fun detect(): Set<DetectionResult> = runCatching {
        allCollectionsMapping.map {
            CompletableFuture.supplyAsync({ queueSizeDetection(it) }, executor)
        }.map { it.join() }.toSet()
    }.onFailure { logger.warn { "Unable to perform detection with queue size. Cause: [${it.message}]" } }
        .getOrElse { emptySet() }

    private fun queueSizeDetection(collectionMapping: SourceToDestination): DetectionResult {
        val queueSize = queues[collectionMapping]?.size()!!

        createOrUpdateMetrics(queueSize, collectionMapping)

        return QueueSizeDetectionResult(
            collectionMapping,
            queueSize == 0,
            queueSize
        )
    }

    private fun createOrUpdateMetrics(
        queueSize: Int,
        collectionMapping: SourceToDestination
    ) {

        val metric = metrics.getOrPut(
            "${collectionMapping.destination.dbName}:${collectionMapping.destination.collectionName}"
        ) {
            AtomicInteger(0)
        }
        metric.set(queueSize)

        meterRegistry.gauge(
            "queue_size",
            Tags.of(
                Tag.of("destination_collection", collectionMapping.destination.collectionName),
                Tag.of("destination_database", collectionMapping.destination.dbName)
            ),
            metric
        )
    }
}
