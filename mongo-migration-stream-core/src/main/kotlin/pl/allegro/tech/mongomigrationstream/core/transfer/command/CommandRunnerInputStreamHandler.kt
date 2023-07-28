package pl.allegro.tech.mongomigrationstream.core.transfer.command

import mu.KotlinLogging
import pl.allegro.tech.mongomigrationstream.core.mongo.SourceToDestination
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.DumpUpdateEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.RestoreUpdateEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateInfo

internal interface CommandRunnerInputStreamHandler {
    fun handle(inputStreamLine: String)
}

private val logger = KotlinLogging.logger { }

internal class DumpCommandInputStreamHandler(
    private val stateInfo: StateInfo,
    private val sourceToDestination: SourceToDestination,
) : CommandRunnerInputStreamHandler {
    override fun handle(inputStreamLine: String) {
        logger.warn { inputStreamLine }
        stateInfo.notifyStateChange(DumpUpdateEvent(sourceToDestination, inputStreamLine))
    }
}

internal class RestoreCommandInputStreamHandler(
    private val stateInfo: StateInfo,
    private val sourceToDestination: SourceToDestination
) : CommandRunnerInputStreamHandler {
    override fun handle(inputStreamLine: String) {
        logger.warn { inputStreamLine }
        stateInfo.notifyStateChange(RestoreUpdateEvent(sourceToDestination, inputStreamLine))
    }
}
