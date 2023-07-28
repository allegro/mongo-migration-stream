package pl.allegro.tech.mongomigrationstream.core.state

import pl.allegro.tech.mongomigrationstream.core.mongo.SourceToDestination
import java.time.Instant

sealed class StateEvent(
    open val sourceToDestination: SourceToDestination,
    val date: Instant = Instant.now()
) {
    data class StartEvent(override val sourceToDestination: SourceToDestination) :
        StateEvent(sourceToDestination)

    data class SourceToLocalStartEvent(override val sourceToDestination: SourceToDestination) :
        StateEvent(sourceToDestination)

    data class DumpStartEvent(override val sourceToDestination: SourceToDestination) :
        StateEvent(sourceToDestination)

    data class DumpUpdateEvent(override val sourceToDestination: SourceToDestination, val info: String) :
        StateEvent(sourceToDestination)

    data class DumpFinishEvent(override val sourceToDestination: SourceToDestination) :
        StateEvent(sourceToDestination)

    data class RestoreStartEvent(override val sourceToDestination: SourceToDestination) :
        StateEvent(sourceToDestination)

    data class RestoreUpdateEvent(override val sourceToDestination: SourceToDestination, val info: String) :
        StateEvent(sourceToDestination)

    data class RestoreFinishEvent(override val sourceToDestination: SourceToDestination) :
        StateEvent(sourceToDestination)

    data class IndexRebuildStartEvent(override val sourceToDestination: SourceToDestination) :
        StateEvent(sourceToDestination)

    data class IndexRebuildFinishEvent(override val sourceToDestination: SourceToDestination) :
        StateEvent(sourceToDestination)

    data class LocalToDestinationStartEvent(override val sourceToDestination: SourceToDestination) :
        StateEvent(sourceToDestination)

    data class StopEvent(override val sourceToDestination: SourceToDestination) :
        StateEvent(sourceToDestination)

    data class PauseEvent(override val sourceToDestination: SourceToDestination) :
        StateEvent(sourceToDestination)

    data class ResumeEvent(override val sourceToDestination: SourceToDestination) :
        StateEvent(sourceToDestination)

    data class FailedEvent(override val sourceToDestination: SourceToDestination, val throwable: Throwable? = null) :
        StateEvent(sourceToDestination)
}
