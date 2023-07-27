package pl.allegro.tech.mongomigrationstream.core.synchronization

interface BatchSizeProvider {
    fun getBatchSize(): Int
}

class ConstantValueBatchSizeProvider(private val batchSize: Int) : BatchSizeProvider {
    override fun getBatchSize(): Int = batchSize
}
