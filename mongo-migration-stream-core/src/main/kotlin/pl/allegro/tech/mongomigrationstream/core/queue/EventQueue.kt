package pl.allegro.tech.mongomigrationstream.core.queue

internal interface EventQueue<E> {
    fun offer(element: E): Boolean
    fun poll(): E
    fun peek(): E
    fun size(): Int
    fun removeAll()
    fun isEmpty(): Boolean = size() == 0
}
