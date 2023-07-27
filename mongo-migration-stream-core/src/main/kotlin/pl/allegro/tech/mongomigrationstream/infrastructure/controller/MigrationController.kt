package pl.allegro.tech.mongomigrationstream.infrastructure.controller

import io.micrometer.core.instrument.MeterRegistry
import pl.allegro.tech.mongomigrationstream.configuration.ApplicationProperties
import pl.allegro.tech.mongomigrationstream.core.cleanup.MigrationCleanup
import pl.allegro.tech.mongomigrationstream.core.performer.Performer
import pl.allegro.tech.mongomigrationstream.core.performer.PerformerController
import pl.allegro.tech.mongomigrationstream.core.state.StateInfo
import pl.allegro.tech.mongomigrationstream.infrastructure.detector.SynchronizationDetectorFactory
import pl.allegro.tech.mongomigrationstream.infrastructure.mongo.MongoDbClients
import pl.allegro.tech.mongomigrationstream.infrastructure.performer.PerformerFactory
import pl.allegro.tech.mongomigrationstream.infrastructure.queue.QueueFactory

internal class MigrationController(
    properties: ApplicationProperties,
    stateInfo: StateInfo,
    meterRegistry: MeterRegistry
) {
    private val mongoDbClients = MongoDbClients(properties, meterRegistry)
    private val queues = QueueFactory.createQueues(properties)
    private val performers: List<Performer> =
        PerformerFactory.createPerformers(properties, mongoDbClients, queues, stateInfo, meterRegistry)
    private val synchronizationDetector = SynchronizationDetectorFactory(properties, mongoDbClients, queues, meterRegistry)
    private val performerController = PerformerController(performers, synchronizationDetector)
    private val migrationCleanup = MigrationCleanup(
        properties.performerProperties.rootPath,
        properties.sourceDbProperties.dbName,
        queues
    )
    private val stopHandler = MigrationStopHandler(
        mongoDbClients,
        synchronizationDetector,
        performerController,
        migrationCleanup
    )
    private val startHandler = MigrationStartHandler(
        properties,
        mongoDbClients,
        performerController,
    )

    fun startMigration() {
        startHandler.start()
    }

    fun stopMigration() {
        stopHandler.stop()
    }

    fun pauseMigration() {
        performers.forEach { it.pause() }
    }

    fun resumeMigration() {
        performers.forEach { it.resume() }
    }
}
