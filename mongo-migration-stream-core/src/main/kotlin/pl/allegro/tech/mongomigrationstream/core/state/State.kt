package pl.allegro.tech.mongomigrationstream.core.state

import pl.allegro.tech.mongomigrationstream.core.mongo.SourceToDestination
import java.time.Instant

data class State(
    val collectionStates: List<CollectionState>
) {
    data class CollectionState(
        val sourceToDestination: SourceToDestination,
        val steps: List<CollectionStep>
    )

    data class CollectionStep(
        val type: StepType,
        val startDate: Instant,
        val endDate: Instant? = null,
        val info: List<Info> = emptyList()
    ) {
        data class Info(
            val date: Instant,
            val message: String
        )
    }

    enum class StepType {
        NEW,
        SOURCE_TO_LOCAL,
        DUMP,
        RESTORE,
        INDEX_REBUILD,
        LOCAL_TO_DESTINATION,
        PAUSED,
        RESUMED,
        FINISHED,
        FAILED
    }
}
