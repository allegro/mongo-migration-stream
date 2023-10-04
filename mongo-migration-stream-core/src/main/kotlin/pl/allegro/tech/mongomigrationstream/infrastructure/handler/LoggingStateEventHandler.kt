package pl.allegro.tech.mongomigrationstream.infrastructure.handler

import io.github.oshai.kotlinlogging.KotlinLogging
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEventHandler

private val logger = KotlinLogging.logger { }

object LoggingStateEventHandler : StateEventHandler {
    override fun handle(event: StateEvent) {
        logger.info { event }
    }
}
