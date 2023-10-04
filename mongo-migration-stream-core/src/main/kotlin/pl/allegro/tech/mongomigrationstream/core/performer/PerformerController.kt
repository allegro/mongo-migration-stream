package pl.allegro.tech.mongomigrationstream.core.performer

import io.github.oshai.kotlinlogging.KotlinLogging
import pl.allegro.tech.mongomigrationstream.core.concurrency.MigrationExecutors
import pl.allegro.tech.mongomigrationstream.infrastructure.detector.SynchronizationDetectorFactory

private val logger = KotlinLogging.logger { }

internal class PerformerController(
    private val performers: List<Performer>,
    private val synchronizationDetector: SynchronizationDetectorFactory
) {
    private val executor = MigrationExecutors.createPerformerExecutor(performers.size)

    fun startPerformers() {
        performers.forEach {
            executor.execute {
                it.perform()
                synchronizationDetector.tryToStartDetectingSynchronization()
            }
        }
        logger.info { "Finished start of migration" }
    }

    fun stopPerformers() {
        try {
            performers.forEach { it.stop() }
            executor.shutdown()
        } catch (throwable: Throwable) {
            logger.warn(throwable) { "Exception while shutting down PerformerController" }
        } finally {
            logger.info { "Shut down PerformerController" }
        }
    }
}
