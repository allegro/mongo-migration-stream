package pl.allegro.tech.mongomigrationstream.configuration

import com.mongodb.ReadPreference
import pl.allegro.tech.mongomigrationstream.core.synchronization.BatchSizeProvider

const val MIN_INSERTION_WORKERS_PER_COLLECTION = 1
const val MAX_INSERTION_WORKERS_PER_COLLECTION = 10

data class PerformerProperties(
    val rootPath: String,
    val mongoToolsPath: String,
    val queueFactory: QueueFactoryType,
    val dumpReadPreference: ReadPreference,
    val batchSizeProvider: BatchSizeProvider,
    val isCompressionEnabled: Boolean,
    val insertionWorkersPerCollection: Int
) {
    init {
        if (insertionWorkersPerCollection < MIN_INSERTION_WORKERS_PER_COLLECTION || insertionWorkersPerCollection > MAX_INSERTION_WORKERS_PER_COLLECTION) {
            throw IllegalArgumentException(
                "Number of insertion workers per collection is set to: [$insertionWorkersPerCollection], " +
                    "but should be between [$MIN_INSERTION_WORKERS_PER_COLLECTION and $MAX_INSERTION_WORKERS_PER_COLLECTION]."
            )
        }
    }

    sealed class QueueFactoryType
    object InMemoryQueueFactoryType : QueueFactoryType()
    object BiqQueueFactoryType : QueueFactoryType()
}
