package pl.allegro.tech.mongomigrationstream

import io.micrometer.core.instrument.MeterRegistry
import pl.allegro.tech.mongomigrationstream.configuration.ApplicationProperties
import pl.allegro.tech.mongomigrationstream.core.state.StateInfo
import pl.allegro.tech.mongomigrationstream.infrastructure.controller.MigrationController

class MongoMigrationStream(
    properties: ApplicationProperties,
    meterRegistry: MeterRegistry
) {
    val stateInfo = StateInfo(properties.stateConfig.stateEventHandler)
    private val migrationController = MigrationController(properties, stateInfo, meterRegistry)

    fun start() {
        migrationController.startMigration()
    }

    fun stop() {
        migrationController.stopMigration()
    }

    fun pause() {
        migrationController.pauseMigration()
    }

    fun resume() {
        migrationController.resumeMigration()
    }
}
