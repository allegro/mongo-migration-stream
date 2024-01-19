package pl.allegro.tech.mongomigrationstream.core.state

import pl.allegro.tech.mongomigrationstream.core.mongo.SourceToDestination
import pl.allegro.tech.mongomigrationstream.core.state.State.CollectionState
import pl.allegro.tech.mongomigrationstream.core.state.State.CollectionStep
import pl.allegro.tech.mongomigrationstream.core.state.State.CollectionStep.Info
import pl.allegro.tech.mongomigrationstream.core.state.State.StepType
import pl.allegro.tech.mongomigrationstream.core.state.State.StepType.DUMP
import pl.allegro.tech.mongomigrationstream.core.state.State.StepType.FAILED
import pl.allegro.tech.mongomigrationstream.core.state.State.StepType.FINISHED
import pl.allegro.tech.mongomigrationstream.core.state.State.StepType.INDEX_REBUILD
import pl.allegro.tech.mongomigrationstream.core.state.State.StepType.LOCAL_TO_DESTINATION
import pl.allegro.tech.mongomigrationstream.core.state.State.StepType.NEW
import pl.allegro.tech.mongomigrationstream.core.state.State.StepType.PAUSED
import pl.allegro.tech.mongomigrationstream.core.state.State.StepType.RESTORE
import pl.allegro.tech.mongomigrationstream.core.state.State.StepType.RESUMED
import pl.allegro.tech.mongomigrationstream.core.state.State.StepType.SOURCE_TO_LOCAL
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.DumpFinishEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.DumpStartEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.DumpUpdateEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.FailedEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.IndexRebuildFinishEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.IndexRebuildStartEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.LocalToDestinationStartEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.PauseEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.RestoreFinishEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.RestoreStartEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.RestoreUpdateEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.ResumeEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.SourceToLocalStartEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.StartEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.StopEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.Type

internal object StateAggregator {
    fun aggregateMigrationState(eventStore: StateEventStore): State =
        State(
            eventStore.getAllEvents().map { (sourceToDestination, events) ->
                aggregateCollectionMigrationState(sourceToDestination, events)
            }
        )

    private fun aggregateCollectionMigrationState(
        sourceToDestination: SourceToDestination,
        events: Map<Type, StateEvent>
    ): CollectionState = CollectionState(
        sourceToDestination,
        eventsToSteps(events)
    )

    private fun eventsToSteps(events: Map<Type, StateEvent>): List<CollectionStep> {
        return events.values.sortedBy { it.date }.fold(mutableMapOf<StepType, CollectionStep>()) { result, migrationEvent ->
            buildSteps(result, migrationEvent)
        }.values.toList().sortedBy { it.startDate }
    }

    private fun buildSteps(
        steps: MutableMap<StepType, CollectionStep>,
        event: StateEvent
    ): MutableMap<StepType, CollectionStep> {
        when (event) {
            is StartEvent -> updateSteps(steps, CollectionStep(NEW, event.date))
            is SourceToLocalStartEvent -> updateSteps(steps, CollectionStep(SOURCE_TO_LOCAL, event.date))
            is DumpStartEvent -> updateSteps(steps, CollectionStep(DUMP, event.date))
            is DumpFinishEvent -> steps[DUMP]?.copy(endDate = event.date)?.let { updateSteps(steps, it) }
            is DumpUpdateEvent -> steps[DUMP]?.copy(info = listOf(Info(event.date, event.info)))?.let { updateSteps(steps, it) }
            is RestoreStartEvent -> updateSteps(steps, CollectionStep(RESTORE, event.date))
            is RestoreUpdateEvent -> steps[RESTORE]?.copy(info = listOf(Info(event.date, event.info)))?.let { updateSteps(steps, it) }
            is RestoreFinishEvent -> steps[RESTORE]?.copy(endDate = event.date)?.let { updateSteps(steps, it) }
            is IndexRebuildStartEvent -> updateSteps(steps, CollectionStep(INDEX_REBUILD, event.date))
            is IndexRebuildFinishEvent -> steps[INDEX_REBUILD]?.copy(endDate = event.date)?.let { updateSteps(steps, it) }
            is LocalToDestinationStartEvent -> updateSteps(steps, CollectionStep(LOCAL_TO_DESTINATION, event.date))
            is StopEvent -> updateSteps(steps, CollectionStep(FINISHED, event.date))
            is PauseEvent -> updateSteps(steps, CollectionStep(PAUSED, event.date))
            is ResumeEvent -> updateSteps(steps, CollectionStep(RESUMED, event.date))
            is FailedEvent -> updateSteps(steps, CollectionStep(FAILED, event.date))
        }
        return steps
    }

    private fun updateSteps(
        steps: MutableMap<StepType, CollectionStep>,
        collectionStep: CollectionStep
    ): MutableMap<StepType, CollectionStep> {
        steps[collectionStep.type] = collectionStep
        return steps
    }
}
