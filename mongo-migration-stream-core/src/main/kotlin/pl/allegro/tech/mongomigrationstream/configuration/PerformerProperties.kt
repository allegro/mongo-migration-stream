package pl.allegro.tech.mongomigrationstream.configuration

import com.mongodb.ReadPreference
import pl.allegro.tech.mongomigrationstream.core.synchronization.BatchSizeProvider

data class PerformerProperties(
    val rootPath: String,
    val mongoToolsPath: String,
    val queueFactory: QueueFactoryType,
    val dumpReadPreference: ReadPreference,
    val batchSizeProvider: BatchSizeProvider,
    val isCompressionEnabled: Boolean,
) {
    sealed class QueueFactoryType
    object InMemoryQueueFactoryType : QueueFactoryType()
    object BiqQueueFactoryType : QueueFactoryType()
}
