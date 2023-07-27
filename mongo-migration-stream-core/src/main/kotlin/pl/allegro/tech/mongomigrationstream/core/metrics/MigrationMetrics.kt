package pl.allegro.tech.mongomigrationstream.core.metrics

internal object MigrationMetrics {
    const val CHANGE_EVENT_COUNTER = "change_event_counter"
    const val CHANGE_EVENT_FETCH_SAVE_TIMER = "save_change_event_to_queue_timer"
    const val SOURCE_COLLECTION_SIZE = "source_collection_size"
    const val DESTINATION_COLLECTION_SIZE = "destination_collection_size"
}
