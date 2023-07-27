package pl.allegro.tech.mongomigrationstream.configuration

import pl.allegro.tech.mongomigrationstream.core.state.StateEventHandler

data class StateConfig(
    val stateEventHandler: StateEventHandler
)
