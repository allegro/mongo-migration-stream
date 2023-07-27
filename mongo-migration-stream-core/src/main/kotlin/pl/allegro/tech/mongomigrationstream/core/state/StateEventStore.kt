package pl.allegro.tech.mongomigrationstream.core.state

import pl.allegro.tech.mongomigrationstream.core.mongo.SourceToDestination
import java.util.concurrent.ConcurrentHashMap

internal class StateEventStore {
    private val events: ConcurrentHashMap<SourceToDestination, List<StateEvent>> = ConcurrentHashMap()

    fun storeEvent(event: StateEvent) {
        events[event.sourceToDestination] = events.getOrDefault(event.sourceToDestination, emptyList()) + listOf(event)
    }

    fun getEvents(sourceToDestination: SourceToDestination): List<StateEvent> = events[sourceToDestination] ?: emptyList()
    fun getAllEvents(): Map<SourceToDestination, List<StateEvent>> = events
}
