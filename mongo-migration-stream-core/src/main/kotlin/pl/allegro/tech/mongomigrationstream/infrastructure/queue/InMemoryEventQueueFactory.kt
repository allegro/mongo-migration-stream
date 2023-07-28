package pl.allegro.tech.mongomigrationstream.infrastructure.queue

import pl.allegro.tech.mongomigrationstream.core.queue.EventQueue
import pl.allegro.tech.mongomigrationstream.core.queue.EventQueueAbstractFactory
import java.util.concurrent.ConcurrentLinkedQueue

internal class InMemoryEventQueueFactory<E> : EventQueueAbstractFactory<E> {
    override fun produce(queueName: String): EventQueue<E> = InMemoryEventQueue()
}

internal class InMemoryEventQueue<E> : EventQueue<E> {
    private val queue = ConcurrentLinkedQueue<E>()

    override fun offer(element: E): Boolean = queue.offer(element)
    override fun poll(): E = queue.poll()
    override fun peek(): E = queue.peek()
    override fun size(): Int = queue.size
    override fun removeAll() {
        queue.removeAll { true }
    }
}
