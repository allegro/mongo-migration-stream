package pl.allegro.tech.mongomigrationstream.core.detector

import pl.allegro.tech.mongomigrationstream.core.mongo.SourceToDestination

sealed class DetectionResult(
    open val collectionMapping: SourceToDestination,
    open val isSynchronized: Boolean
)

internal data class HashDetectionResult(
    override val collectionMapping: SourceToDestination,
    override val isSynchronized: Boolean
) : DetectionResult(collectionMapping, isSynchronized) {
    override fun toString(): String =
        "Hash synchronization detector - collection: [$collectionMapping] hashes equality: [$isSynchronized]"
}

internal data class CollectionCountDetectionResult(
    override val collectionMapping: SourceToDestination,
    override val isSynchronized: Boolean,
    val sourceCount: Long,
    val destinationCount: Long
) : DetectionResult(collectionMapping, isSynchronized) {
    override fun toString(): String =
        "Collection count detector - " + "collection: [$collectionMapping] source size: [$sourceCount], " +
            "destination size: [$destinationCount], " + "diff: [${sourceCount - destinationCount}]"
}

internal data class QueueSizeDetectionResult(
    override val collectionMapping: SourceToDestination,
    override val isSynchronized: Boolean,
    val queueSize: Int
) : DetectionResult(collectionMapping, isSynchronized) {
    override fun toString(): String =
        "Queue size detector - collection: [$collectionMapping] queue size: [$queueSize]"
}
