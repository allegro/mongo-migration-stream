package pl.allegro.tech.mongomigrationstream.core.state

import com.google.common.eventbus.Subscribe

internal class EventBusChangeRecorder(
    private val eventStore: StateEventStore,
    private val stateEventHandler: StateEventHandler
) {
    @Subscribe
    fun recordMigrationEvent(event: StateEvent) {
        eventStore.storeEvent(event)
        stateEventHandler.handle(event)
    }
}
