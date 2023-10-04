package pl.allegro.tech.mongomigrationstream.infrastructure.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import pl.allegro.tech.mongomigrationstream.core.cleanup.MigrationCleanup
import pl.allegro.tech.mongomigrationstream.core.performer.PerformerController
import pl.allegro.tech.mongomigrationstream.infrastructure.detector.SynchronizationDetectorFactory
import pl.allegro.tech.mongomigrationstream.infrastructure.mongo.MongoDbClients

private val logger = KotlinLogging.logger { }

internal class MigrationStopHandler(
    private val mongoDbClients: MongoDbClients,
    private val synchronizationDetector: SynchronizationDetectorFactory,
    private val performerController: PerformerController,
    private val migrationCleanup: MigrationCleanup
) {
    fun stop() {
        logger.info { "Trying to stop mongo-migration-stream..." }
        try {
            synchronizationDetector.stopDetectingSynchronization()
            performerController.stopPerformers()
            migrationCleanup.cleanupAfterMigration()
            mongoDbClients.closeClients()
        } catch (e: Throwable) {
            logger.error(e) { "Error when stopping mongo-migration-stream" }
            throw MongoMigrationStreamStopException("Error when stopping mongo-migration-stream", e)
        }
        logger.info { "Stopped mongo-migration-stream successfully" }
    }
}
