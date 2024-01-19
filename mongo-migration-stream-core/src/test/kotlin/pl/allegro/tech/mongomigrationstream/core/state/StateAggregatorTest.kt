package pl.allegro.tech.mongomigrationstream.core.state

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import pl.allegro.tech.mongomigrationstream.core.mongo.DbCollection
import pl.allegro.tech.mongomigrationstream.core.mongo.SourceToDestination

internal class StateAggregatorTest : ShouldSpec({

    should("aggregate state from no events") {
        // Given:
        val eventStore = StateEventStore()

        // When:
        val result = StateAggregator.aggregateMigrationState(eventStore)

        // Then:
        result.collectionStates.shouldBeEmpty()
    }

    should("aggregate state from [Start] event") {
        // Given:
        val sourceToDestination = SourceToDestination(DbCollection("", ""), DbCollection("", ""))
        val eventStore = StateEventStore()
        eventStore.storeEvent(StateEvent.StartEvent(sourceToDestination))

        // When:
        val result = StateAggregator.aggregateMigrationState(eventStore)

        // Then:
        result.collectionStates.size.shouldBe(1) // Only one source to destination
        result.collectionStates[0].sourceToDestination.shouldBe(sourceToDestination)
        result.collectionStates[0].steps.size.shouldBe(1) // Only one step for a given source to destination
        result.collectionStates[0].steps[0].type.shouldBe(State.StepType.NEW)
    }

    should("aggregate state from [Start, SourceToLocalStart] events") {
        // Given:
        val sourceToDestination = SourceToDestination(DbCollection("", ""), DbCollection("", ""))
        val eventStore = StateEventStore()
        eventStore.storeEvent(StateEvent.StartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.SourceToLocalStartEvent(sourceToDestination))

        // When:
        val result = StateAggregator.aggregateMigrationState(eventStore)

        // Then:
        result.collectionStates.size.shouldBe(1)
        result.collectionStates[0].sourceToDestination.shouldBe(sourceToDestination)
        result.collectionStates[0].steps.size.shouldBe(2)
        result.collectionStates[0].steps[1].type.shouldBe(State.StepType.SOURCE_TO_LOCAL)
    }

    should("aggregate state from [Start, SourceToLocalStart, DumpStart] events") {
        // Given:
        val sourceToDestination = SourceToDestination(DbCollection("", ""), DbCollection("", ""))
        val eventStore = StateEventStore()
        eventStore.storeEvent(StateEvent.StartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.SourceToLocalStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpStartEvent(sourceToDestination))

        // When:
        val result = StateAggregator.aggregateMigrationState(eventStore)

        // Then:
        result.collectionStates.size.shouldBe(1) // Only one source to destination
        result.collectionStates[0].sourceToDestination.shouldBe(sourceToDestination)
        result.collectionStates[0].steps.size.shouldBe(3)
        result.collectionStates[0].steps[2].type.shouldBe(State.StepType.DUMP)
    }

    should("contain info about dump when update dump event is aggregated") {
        // Given:
        val sourceToDestination = SourceToDestination(DbCollection("", ""), DbCollection("", ""))
        val eventStore = StateEventStore()
        val infoMessage = "Info message"
        eventStore.storeEvent(StateEvent.StartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.SourceToLocalStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpUpdateEvent(sourceToDestination, infoMessage))

        // When:
        val result = StateAggregator.aggregateMigrationState(eventStore)

        // Then:
        result.collectionStates[0].steps[2].info.size.shouldBe(1)
        result.collectionStates[0].steps[2].info.first().message.shouldBe(infoMessage)
    }

    should("contain only last info about dump when multiple update dump events are aggregated") {
        // Given:
        val sourceToDestination = SourceToDestination(DbCollection("", ""), DbCollection("", ""))
        val eventStore = StateEventStore()
        val newestMessage = "Newest message"
        eventStore.storeEvent(StateEvent.StartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.SourceToLocalStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpUpdateEvent(sourceToDestination, ""))
        eventStore.storeEvent(StateEvent.DumpUpdateEvent(sourceToDestination, ""))
        eventStore.storeEvent(StateEvent.DumpUpdateEvent(sourceToDestination, ""))
        eventStore.storeEvent(StateEvent.DumpUpdateEvent(sourceToDestination, newestMessage))

        // When:
        val result = StateAggregator.aggregateMigrationState(eventStore)

        // Then:
        result.collectionStates[0].steps[2].info.size.shouldBe(1) // Even though there are multiple DumpUpdateEvents, store only the last one
        result.collectionStates[0].steps[2].info.first().message.shouldBe(newestMessage)
    }

    should("aggregate state from [Start, SourceToLocalStart, DumpStart, DumpUpdate, DumpFinish] events") {
        // Given:
        val sourceToDestination = SourceToDestination(DbCollection("", ""), DbCollection("", ""))
        val eventStore = StateEventStore()
        val dumpInfo = "dumpInfo"
        eventStore.storeEvent(StateEvent.StartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.SourceToLocalStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpUpdateEvent(sourceToDestination, dumpInfo))
        eventStore.storeEvent(StateEvent.DumpFinishEvent(sourceToDestination))

        // When:
        val result = StateAggregator.aggregateMigrationState(eventStore)

        // Then:
        result.collectionStates.size.shouldBe(1)
        result.collectionStates[0].sourceToDestination.shouldBe(sourceToDestination)
        result.collectionStates[0].steps.size.shouldBe(3)
        result.collectionStates[0].steps[2].type.shouldBe(State.StepType.DUMP)

        val dumpStep = result.collectionStates[0].steps[2]
        dumpStep.startDate.shouldNotBeNull()
        dumpStep.endDate.shouldNotBeNull()
        dumpStep.info.first().message.shouldBe(dumpInfo)
    }

    should("aggregate state from [Start, SourceToLocalStart, DumpStart, DumpUpdate, DumpFinish, RestoreStart] events") {
        // Given:
        val sourceToDestination = SourceToDestination(DbCollection("", ""), DbCollection("", ""))
        val eventStore = StateEventStore()
        eventStore.storeEvent(StateEvent.StartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.SourceToLocalStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpUpdateEvent(sourceToDestination, ""))
        eventStore.storeEvent(StateEvent.DumpFinishEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.RestoreStartEvent(sourceToDestination))

        // When:
        val result = StateAggregator.aggregateMigrationState(eventStore)

        // Then:
        result.collectionStates.size.shouldBe(1)
        result.collectionStates[0].sourceToDestination.shouldBe(sourceToDestination)
        result.collectionStates[0].steps.size.shouldBe(4)
        result.collectionStates[0].steps[3].type.shouldBe(State.StepType.RESTORE)
        val restoreStep = result.collectionStates[0].steps[3]
        restoreStep.startDate.shouldNotBeNull()
        restoreStep.endDate.shouldBeNull()
        restoreStep.info.shouldBeEmpty()
    }

    should("contain info about restore when update restore event is aggregated") {
        // Given:
        val sourceToDestination = SourceToDestination(DbCollection("", ""), DbCollection("", ""))
        val eventStore = StateEventStore()
        val restoreUpdateMessage = "restore update message"
        eventStore.storeEvent(StateEvent.StartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.SourceToLocalStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpUpdateEvent(sourceToDestination, ""))
        eventStore.storeEvent(StateEvent.DumpFinishEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.RestoreStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.RestoreUpdateEvent(sourceToDestination, restoreUpdateMessage))

        // When:
        val result = StateAggregator.aggregateMigrationState(eventStore)

        // Then:
        result.collectionStates[0].steps[3].type.shouldBe(State.StepType.RESTORE)
        result.collectionStates[0].steps[3].info.size.shouldBe(1)
        result.collectionStates[0].steps[3].info.first().message.shouldBe(restoreUpdateMessage)
    }

    should("contain only last info about restore when multiple update restore events are aggregated") {
        // Given:
        val sourceToDestination = SourceToDestination(DbCollection("", ""), DbCollection("", ""))
        val eventStore = StateEventStore()
        val newestMessage = "restore update message"
        eventStore.storeEvent(StateEvent.StartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.SourceToLocalStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpUpdateEvent(sourceToDestination, ""))
        eventStore.storeEvent(StateEvent.DumpFinishEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.RestoreStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.RestoreUpdateEvent(sourceToDestination, ""))
        eventStore.storeEvent(StateEvent.RestoreUpdateEvent(sourceToDestination, ""))
        eventStore.storeEvent(StateEvent.RestoreUpdateEvent(sourceToDestination, ""))
        eventStore.storeEvent(StateEvent.RestoreUpdateEvent(sourceToDestination, newestMessage))

        // When:
        val result = StateAggregator.aggregateMigrationState(eventStore)

        // Then:
        result.collectionStates[0].steps[3].type.shouldBe(State.StepType.RESTORE)
        result.collectionStates[0].steps[3].info.size.shouldBe(1) // despite multiple RestoreUpdate, only last one is stored
        result.collectionStates[0].steps[3].info.first().message.shouldBe(newestMessage)
    }

    should("aggregate state from [Start, SourceToLocalStart, DumpStart, DumpUpdate, DumpFinish, RestoreStart, RestoreFinish] events") {
        // Given:
        val sourceToDestination = SourceToDestination(DbCollection("", ""), DbCollection("", ""))
        val eventStore = StateEventStore()
        eventStore.storeEvent(StateEvent.StartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.SourceToLocalStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpUpdateEvent(sourceToDestination, ""))
        eventStore.storeEvent(StateEvent.DumpFinishEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.RestoreStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.RestoreFinishEvent(sourceToDestination))

        // When:
        val result = StateAggregator.aggregateMigrationState(eventStore)

        // Then:
        result.collectionStates.size.shouldBe(1)
        result.collectionStates[0].sourceToDestination.shouldBe(sourceToDestination)
        result.collectionStates[0].steps.size.shouldBe(4)
        result.collectionStates[0].steps[3].type.shouldBe(State.StepType.RESTORE)

        val restoreStep = result.collectionStates[0].steps[3]
        restoreStep.startDate.shouldNotBeNull()
        restoreStep.endDate.shouldNotBeNull() // Restore step is finished
    }

    should("aggregate state from [Start, SourceToLocalStart, DumpStart, DumpUpdate, DumpFinish, RestoreStart, RestoreFinish, IndexRebuildStart] events") {
        // Given:
        val sourceToDestination = SourceToDestination(DbCollection("", ""), DbCollection("", ""))
        val eventStore = StateEventStore()
        eventStore.storeEvent(StateEvent.StartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.SourceToLocalStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpUpdateEvent(sourceToDestination, ""))
        eventStore.storeEvent(StateEvent.DumpFinishEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.RestoreStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.RestoreFinishEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.IndexRebuildStartEvent(sourceToDestination))

        // When:
        val result = StateAggregator.aggregateMigrationState(eventStore)

        // Then:
        result.collectionStates.size.shouldBe(1)
        result.collectionStates[0].sourceToDestination.shouldBe(sourceToDestination)
        result.collectionStates[0].steps.size.shouldBe(5)
        result.collectionStates[0].steps[4].type.shouldBe(State.StepType.INDEX_REBUILD)

        val indexRebuildStep = result.collectionStates[0].steps[4]
        indexRebuildStep.startDate.shouldNotBeNull()
        indexRebuildStep.endDate.shouldBeNull()
    }

    should("aggregate state from [Start, SourceToLocalStart, DumpStart, DumpUpdate, DumpFinish, RestoreStart, RestoreFinish, IndexRebuildStart, IndexRebuildFinish] events") {
        // Given:
        val sourceToDestination = SourceToDestination(DbCollection("", ""), DbCollection("", ""))
        val eventStore = StateEventStore()
        eventStore.storeEvent(StateEvent.StartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.SourceToLocalStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpUpdateEvent(sourceToDestination, ""))
        eventStore.storeEvent(StateEvent.DumpFinishEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.RestoreStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.RestoreFinishEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.IndexRebuildStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.IndexRebuildFinishEvent(sourceToDestination))

        // When:
        val result = StateAggregator.aggregateMigrationState(eventStore)

        // Then:
        result.collectionStates.size.shouldBe(1)
        result.collectionStates[0].sourceToDestination.shouldBe(sourceToDestination)
        result.collectionStates[0].steps.size.shouldBe(5)
        result.collectionStates[0].steps[4].type.shouldBe(State.StepType.INDEX_REBUILD)

        val indexRebuildStep = result.collectionStates[0].steps[4]
        indexRebuildStep.startDate.shouldNotBeNull()
        indexRebuildStep.endDate.shouldNotBeNull() // Index rebuilding is finished
    }

    should("aggregate state from [Start, SourceToLocalStart, DumpStart, DumpUpdate, DumpFinish, RestoreStart, RestoreFinish, IndexRebuildStart, IndexRebuildFinish, LocalToDestinationStart] events") {
        // Given:
        val sourceToDestination = SourceToDestination(DbCollection("", ""), DbCollection("", ""))
        val eventStore = StateEventStore()
        eventStore.storeEvent(StateEvent.StartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.SourceToLocalStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpUpdateEvent(sourceToDestination, ""))
        eventStore.storeEvent(StateEvent.DumpFinishEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.RestoreStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.RestoreFinishEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.IndexRebuildStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.LocalToDestinationStartEvent(sourceToDestination)) // Local to destination most of the time will occur earlier than index rebuild finish
        eventStore.storeEvent(StateEvent.IndexRebuildFinishEvent(sourceToDestination))

        // When:
        val result = StateAggregator.aggregateMigrationState(eventStore)

        // Then:
        result.collectionStates.size.shouldBe(1)
        result.collectionStates[0].sourceToDestination.shouldBe(sourceToDestination)
        result.collectionStates[0].steps.size.shouldBe(6)
        result.collectionStates[0].steps[5].type.shouldBe(State.StepType.LOCAL_TO_DESTINATION)

        val localToDestination = result.collectionStates[0].steps[5]
        localToDestination.startDate.shouldNotBeNull()
        localToDestination.endDate.shouldBeNull()
    }

    should("aggregate state from [Start, SourceToLocalStart, DumpStart, DumpUpdate, DumpFinish, RestoreStart, RestoreFinish, IndexRebuildStart, IndexRebuildFinish, LocalToDestinationStart, Stop] events") {
        // Given:
        val sourceToDestination = SourceToDestination(DbCollection("", ""), DbCollection("", ""))
        val eventStore = StateEventStore()
        eventStore.storeEvent(StateEvent.StartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.SourceToLocalStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpUpdateEvent(sourceToDestination, ""))
        eventStore.storeEvent(StateEvent.DumpFinishEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.RestoreStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.RestoreFinishEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.IndexRebuildStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.LocalToDestinationStartEvent(sourceToDestination)) // Local to destination most of the time will occur earlier than index rebuild finish
        eventStore.storeEvent(StateEvent.IndexRebuildFinishEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.StopEvent(sourceToDestination))

        // When:
        val result = StateAggregator.aggregateMigrationState(eventStore)

        // Then:
        result.collectionStates.size.shouldBe(1)
        result.collectionStates[0].sourceToDestination.shouldBe(sourceToDestination)
        result.collectionStates[0].steps.size.shouldBe(7)
        result.collectionStates[0].steps[6].type.shouldBe(State.StepType.FINISHED)

        val finishStep = result.collectionStates[0].steps[6]
        finishStep.startDate.shouldNotBeNull()
    }

    should("aggregate state from [Start, SourceToLocalStart, DumpStart, DumpUpdate, DumpFinish, RestoreStart, RestoreFinish, IndexRebuildStart, IndexRebuildFinish, LocalToDestinationStart, Pause] events") {
        // Given:
        val sourceToDestination = SourceToDestination(DbCollection("", ""), DbCollection("", ""))
        val eventStore = StateEventStore()
        eventStore.storeEvent(StateEvent.StartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.SourceToLocalStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpUpdateEvent(sourceToDestination, ""))
        eventStore.storeEvent(StateEvent.DumpFinishEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.RestoreStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.RestoreFinishEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.IndexRebuildStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.LocalToDestinationStartEvent(sourceToDestination)) // Local to destination most of the time will occur earlier than index rebuild finish
        eventStore.storeEvent(StateEvent.IndexRebuildFinishEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.PauseEvent(sourceToDestination))

        // When:
        val result = StateAggregator.aggregateMigrationState(eventStore)

        // Then:
        result.collectionStates.size.shouldBe(1)
        result.collectionStates[0].sourceToDestination.shouldBe(sourceToDestination)
        result.collectionStates[0].steps.size.shouldBe(7)
        result.collectionStates[0].steps[6].type.shouldBe(State.StepType.PAUSED)

        val pausedStep = result.collectionStates[0].steps[6]
        pausedStep.startDate.shouldNotBeNull()
    }

    should("aggregate state from [Start, SourceToLocalStart, DumpStart, DumpUpdate, DumpFinish, RestoreStart, RestoreFinish, IndexRebuildStart, IndexRebuildFinish, LocalToDestinationStart, Pause, Resume] events") {
        // Given:
        val sourceToDestination = SourceToDestination(DbCollection("", ""), DbCollection("", ""))
        val eventStore = StateEventStore()
        eventStore.storeEvent(StateEvent.StartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.SourceToLocalStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpUpdateEvent(sourceToDestination, ""))
        eventStore.storeEvent(StateEvent.DumpFinishEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.RestoreStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.RestoreFinishEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.IndexRebuildStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.LocalToDestinationStartEvent(sourceToDestination)) // Local to destination most of the time will occur earlier than index rebuild finish
        eventStore.storeEvent(StateEvent.IndexRebuildFinishEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.PauseEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.ResumeEvent(sourceToDestination))

        // When:
        val result = StateAggregator.aggregateMigrationState(eventStore)

        // Then:
        result.collectionStates.size.shouldBe(1)
        result.collectionStates[0].sourceToDestination.shouldBe(sourceToDestination)
        result.collectionStates[0].steps.size.shouldBe(8)
        result.collectionStates[0].steps[7].type.shouldBe(State.StepType.RESUMED)

        val resumedStep = result.collectionStates[0].steps[7]
        resumedStep.startDate.shouldNotBeNull()
    }

    should("aggregate state from [Start, SourceToLocalStart, DumpStart, DumpUpdate, DumpFinish, RestoreStart, RestoreFinish, IndexRebuildStart, IndexRebuildFinish, LocalToDestinationStart, Pause, Resume, Failed] events") {
        // Given:
        val sourceToDestination = SourceToDestination(DbCollection("", ""), DbCollection("", ""))
        val eventStore = StateEventStore()
        eventStore.storeEvent(StateEvent.StartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.SourceToLocalStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpUpdateEvent(sourceToDestination, ""))
        eventStore.storeEvent(StateEvent.DumpFinishEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.RestoreStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.RestoreFinishEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.IndexRebuildStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.LocalToDestinationStartEvent(sourceToDestination)) // Local to destination most of the time will occur earlier than index rebuild finish
        eventStore.storeEvent(StateEvent.IndexRebuildFinishEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.PauseEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.ResumeEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.FailedEvent(sourceToDestination))

        // When:
        val result = StateAggregator.aggregateMigrationState(eventStore)

        // Then:
        result.collectionStates.size.shouldBe(1)
        result.collectionStates[0].sourceToDestination.shouldBe(sourceToDestination)
        result.collectionStates[0].steps.size.shouldBe(9)
        result.collectionStates[0].steps[8].type.shouldBe(State.StepType.FAILED)
    }

    should("aggregate failed events if FailedEvent occurred between other events") {
        // Given:
        val sourceToDestination = SourceToDestination(DbCollection("", ""), DbCollection("", ""))
        val eventStore = StateEventStore()
        eventStore.storeEvent(StateEvent.StartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.SourceToLocalStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpStartEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.FailedEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.DumpFinishEvent(sourceToDestination))
        eventStore.storeEvent(StateEvent.RestoreStartEvent(sourceToDestination))

        // When:
        val result = StateAggregator.aggregateMigrationState(eventStore)

        // Then:
        result.collectionStates.size.shouldBe(1)
        result.collectionStates[0].sourceToDestination.shouldBe(sourceToDestination)
        result.collectionStates[0].steps.size.shouldBe(5)
        result.collectionStates[0].steps[3].type.shouldBe(State.StepType.FAILED) // FailedEvent is not the last one
        result.collectionStates[0].steps[4].type.shouldBe(State.StepType.RESTORE)
    }
})
