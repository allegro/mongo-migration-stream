package pl.allegro.tech.mongomigrationstream.core.state

import pl.allegro.tech.mongomigrationstream.core.mongo.SourceToDestination
import java.util.concurrent.ConcurrentHashMap

internal class StateEventStore {
    private val events: ConcurrentHashMap<SourceToDestination, ConcurrentHashMap<StateEvent.Type, StateEvent>> = ConcurrentHashMap()

    fun storeEvent(event: StateEvent) {
        val eventsForCollection = events.getOrDefault(event.sourceToDestination, ConcurrentHashMap())
        eventsForCollection[event.type] = event
        events[event.sourceToDestination] = eventsForCollection
    }

    fun getEvents(sourceToDestination: SourceToDestination): Map<StateEvent.Type, StateEvent> = events[sourceToDestination] ?: emptyMap()
    fun getAllEvents(): Map<SourceToDestination, Map<StateEvent.Type, StateEvent>> = events
}
