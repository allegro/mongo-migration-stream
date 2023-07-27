package pl.allegro.tech.mongomigrationstream.infrastructure.handler

import pl.allegro.tech.mongomigrationstream.core.state.StateEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEventHandler

object NoOpStateEventHandler : StateEventHandler {
    override fun handle(event: StateEvent) {}
}
