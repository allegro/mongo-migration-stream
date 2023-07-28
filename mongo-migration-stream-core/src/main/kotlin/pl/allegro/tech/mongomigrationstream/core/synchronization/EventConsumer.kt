package pl.allegro.tech.mongomigrationstream.core.synchronization

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import pl.allegro.tech.mongomigrationstream.core.metrics.MigrationMetrics
import pl.allegro.tech.mongomigrationstream.core.queue.EventQueue

internal class EventConsumer(
    private val queue: EventQueue<ChangeEvent>,
    meterRegistry: MeterRegistry
) {
    private val timer = Timer.builder(MigrationMetrics.CHANGE_EVENT_FETCH_SAVE_TIMER)
        .description("Measures how long it takes to fetch ChangeStream event and save it to local queue")
        .register(meterRegistry)

    internal fun saveEventToLocalQueue(event: ChangeEvent) {
        timer.record { queue.offer(event) }
    }
}
