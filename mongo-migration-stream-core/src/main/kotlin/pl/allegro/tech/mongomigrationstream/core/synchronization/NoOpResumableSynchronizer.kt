package pl.allegro.tech.mongomigrationstream.core.synchronization

import pl.allegro.tech.mongomigrationstream.core.performer.ResumableSynchronizer
import pl.allegro.tech.mongomigrationstream.core.performer.SynchronizationResult
import pl.allegro.tech.mongomigrationstream.core.performer.SynchronizationSuccess

internal class NoOpResumableSynchronizer : ResumableSynchronizer {
    override fun pause() {}
    override fun resume() {}
    override fun startSynchronization(): SynchronizationResult = SynchronizationSuccess
    override fun stop() {}
}
