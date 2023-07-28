package pl.allegro.tech.mongomigrationstream.core.state

interface StateEventHandler {
    fun handle(event: StateEvent)
}
