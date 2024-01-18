package pl.allegro.tech.mongomigrationstream.core.state

import pl.allegro.tech.mongomigrationstream.core.mongo.SourceToDestination
import java.time.Instant

sealed class StateEvent(
    open val sourceToDestination: SourceToDestination,
    val type: Type,
    val date: Instant = Instant.now(),
) {
    enum class Type {
        START, SOURCE_TO_LOCAL_START, DUMP_START, DUMP_UPDATE, DUMP_FINISH, RESTORE_START, RESTORE_UPDATE, RESTORE_FINISH,
        INDEX_REBUILD_START, INDEX_REBUILD_FINISH, LOCAL_TO_DESTINATION_START, STOP, PAUSE, RESUME, FAILED;
    }

    data class StartEvent(override val sourceToDestination: SourceToDestination) :
        StateEvent(sourceToDestination, Type.START)

    data class SourceToLocalStartEvent(override val sourceToDestination: SourceToDestination) :
        StateEvent(sourceToDestination, Type.SOURCE_TO_LOCAL_START)

    data class DumpStartEvent(override val sourceToDestination: SourceToDestination) :
        StateEvent(sourceToDestination, Type.DUMP_START)

    data class DumpUpdateEvent(override val sourceToDestination: SourceToDestination, val info: String) :
        StateEvent(sourceToDestination, Type.DUMP_UPDATE)

    data class DumpFinishEvent(override val sourceToDestination: SourceToDestination) :
        StateEvent(sourceToDestination, Type.DUMP_FINISH)

    data class RestoreStartEvent(override val sourceToDestination: SourceToDestination) :
        StateEvent(sourceToDestination, Type.RESTORE_START)

    data class RestoreUpdateEvent(override val sourceToDestination: SourceToDestination, val info: String) :
        StateEvent(sourceToDestination, Type.RESTORE_UPDATE)

    data class RestoreFinishEvent(override val sourceToDestination: SourceToDestination) :
        StateEvent(sourceToDestination, Type.RESTORE_FINISH)

    data class IndexRebuildStartEvent(override val sourceToDestination: SourceToDestination) :
        StateEvent(sourceToDestination, Type.INDEX_REBUILD_START)

    data class IndexRebuildFinishEvent(override val sourceToDestination: SourceToDestination) :
        StateEvent(sourceToDestination, Type.INDEX_REBUILD_FINISH)

    data class LocalToDestinationStartEvent(override val sourceToDestination: SourceToDestination) :
        StateEvent(sourceToDestination, Type.LOCAL_TO_DESTINATION_START)

    data class StopEvent(override val sourceToDestination: SourceToDestination) :
        StateEvent(sourceToDestination, Type.STOP)

    data class PauseEvent(override val sourceToDestination: SourceToDestination) :
        StateEvent(sourceToDestination, Type.PAUSE)

    data class ResumeEvent(override val sourceToDestination: SourceToDestination) :
        StateEvent(sourceToDestination, Type.RESUME)

    data class FailedEvent(override val sourceToDestination: SourceToDestination, val throwable: Throwable? = null) :
        StateEvent(sourceToDestination, Type.FAILED)
}
