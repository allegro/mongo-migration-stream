package pl.allegro.tech.mongomigrationstream.core.state

import com.google.common.eventbus.EventBus

class StateInfo(stateEventHandler: StateEventHandler) {
    private val eventStore: StateEventStore = StateEventStore()
    private val changeRecorder: EventBusChangeRecorder = EventBusChangeRecorder(eventStore, stateEventHandler)
    private val eventBus: EventBus = EventBus()

    init {
        eventBus.register(changeRecorder)
    }

    internal fun notifyStateChange(event: StateEvent) {
        eventBus.post(event)
    }

    fun getMigrationState(): State = StateAggregator.aggregateMigrationState(eventStore)
}
