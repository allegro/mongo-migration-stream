package pl.allegro.tech.mongomigrationstream.core.detector

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import mu.KotlinLogging
import org.bson.BsonDocument
import pl.allegro.tech.mongomigrationstream.core.metrics.MigrationMetrics
import pl.allegro.tech.mongomigrationstream.core.mongo.SourceToDestination
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger { }

internal class CollectionCountSynchronizationDetector(
    private val sourceDb: MongoDatabase,
    private val destinationDb: MongoDatabase,
    private val allCollectionsMapping: Set<SourceToDestination>,
    private val executor: Executor,
    private val meterRegistry: MeterRegistry
) : SynchronizationDetector {

    // {<DBNAME:SOURCE_COLLECTION:DESTINATION_COLLECTION>: <SRC_COLL_SIZE, DST_COLL_SIZE>}}
    private val metricMap = ConcurrentHashMap<String, Pair<AtomicLong, AtomicLong>>()

    override fun detect(): Set<DetectionResult> = runCatching {
        allCollectionsMapping
            .map {
                CompletableFuture.supplyAsync({
                    equalCollectionCountDetection(
                        sourceDb.getCollection(it.source.collectionName, BsonDocument::class.java),
                        destinationDb.getCollection(it.destination.collectionName, BsonDocument::class.java),
                        it
                    )
                }, executor)
            }
            .map { it.join() }
            .toSet()
    }
        .onFailure { logger.warn { "Unable to perform detection with collection count. Cause: [${it.message}]" } }
        .getOrElse { emptySet() }

    private fun equalCollectionCountDetection(
        sourceCollection: MongoCollection<*>,
        destinationCollection: MongoCollection<*>,
        collectionMapping: SourceToDestination
    ): DetectionResult = runCatching {
        listOf(
            CompletableFuture.supplyAsync({ sourceCollection.estimatedDocumentCount() }, executor),
            CompletableFuture.supplyAsync({ destinationCollection.estimatedDocumentCount() }, executor)
        ).map { it.join() }
    }.map {
        createOrUpdateMetrics(it, collectionMapping)
        it
    }.map {
        val (sourceCollectionSize, destinationCollectionSize) = it
        CollectionCountDetectionResult(
            collectionMapping,
            destinationCollectionSize == sourceCollectionSize,
            sourceCollectionSize,
            destinationCollectionSize
        )
    }.getOrThrow()

    private fun createOrUpdateMetrics(
        sourceAndDestinationCollectionSize: List<Long>,
        collectionMapping: SourceToDestination
    ) {
        val (sourceCollectionSize, destinationCollectionSize) = sourceAndDestinationCollectionSize

        val metrics = metricMap.getOrPut(
            collectionMapping.source.dbName +
                ":${collectionMapping.source.collectionName}" +
                ":${collectionMapping.destination.collectionName}"
        ) {
            Pair(AtomicLong(0), AtomicLong(0))
        }
        val (source_coll_size, dest_coll_size) = metrics

        source_coll_size.set(sourceCollectionSize)
        dest_coll_size.set(destinationCollectionSize)
        meterRegistry.gauge(
            MigrationMetrics.SOURCE_COLLECTION_SIZE,
            Tags.of(
                Tag.of("source_collection", collectionMapping.source.collectionName),
                Tag.of("source_database", collectionMapping.source.dbName)
            ),
            source_coll_size
        )
        meterRegistry.gauge(
            MigrationMetrics.DESTINATION_COLLECTION_SIZE,
            Tags.of(
                Tag.of("destination_collection", collectionMapping.destination.collectionName),
                Tag.of("destination_database", collectionMapping.destination.dbName)
            ),
            dest_coll_size
        )
    }
}
