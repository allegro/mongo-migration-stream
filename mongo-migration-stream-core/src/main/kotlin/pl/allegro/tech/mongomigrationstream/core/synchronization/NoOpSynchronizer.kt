package pl.allegro.tech.mongomigrationstream.core.synchronization

import pl.allegro.tech.mongomigrationstream.core.performer.SynchronizationResult
import pl.allegro.tech.mongomigrationstream.core.performer.SynchronizationSuccess
import pl.allegro.tech.mongomigrationstream.core.performer.Synchronizer

internal class NoOpSynchronizer : Synchronizer {
    override fun startSynchronization(): SynchronizationResult = SynchronizationSuccess
    override fun stop() {}
}
