package pl.allegro.tech.mongomigrationstream.core.queue

internal interface EventQueueAbstractFactory<E> {
    fun produce(queueName: String): EventQueue<E>
}
