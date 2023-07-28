package pl.allegro.tech.mongomigrationstream.infrastructure.queue

import com.leansoft.bigqueue.BigQueueImpl
import org.apache.commons.lang3.SerializationUtils
import pl.allegro.tech.mongomigrationstream.core.paths.MigrationPaths
import pl.allegro.tech.mongomigrationstream.core.queue.EventQueue
import pl.allegro.tech.mongomigrationstream.core.queue.EventQueueAbstractFactory
import java.io.Serializable
import java.nio.file.Path
import kotlin.io.path.absolutePathString

internal class BigQueueEventQueueFactory<E : Serializable>(private val rootPath: String) : EventQueueAbstractFactory<E> {
    override fun produce(queueName: String): EventQueue<E> = BigQueueEventQueue(queuePath(), queueName)
    private fun queuePath() = Path.of(rootPath, MigrationPaths.QUEUES_DIR).absolutePathString()
}

internal class BigQueueEventQueue<E : Serializable>(
    path: String,
    queueName: String
) : EventQueue<E> {
    private val queue = BigQueueImpl(path, queueName)

    override fun offer(element: E): Boolean = queue.enqueue(element.toByteArray()).let { true }
    override fun poll(): E = queue.dequeue().toE()
    override fun peek(): E = queue.peek().toE()
    override fun size(): Int = queue.size().toInt()
    override fun removeAll() {
        queue.removeAll()
        queue.gc()
    }

    private fun E.toByteArray(): ByteArray = SerializationUtils.serialize(this)
    private fun ByteArray.toE(): E = SerializationUtils.deserialize(this)
}
