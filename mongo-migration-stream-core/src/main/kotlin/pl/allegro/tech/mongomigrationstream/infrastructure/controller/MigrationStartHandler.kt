package pl.allegro.tech.mongomigrationstream.infrastructure.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import pl.allegro.tech.mongomigrationstream.configuration.ApplicationProperties
import pl.allegro.tech.mongomigrationstream.core.performer.PerformerController
import pl.allegro.tech.mongomigrationstream.core.validation.ExternalDependenciesValidator
import pl.allegro.tech.mongomigrationstream.core.validation.ExternalDependencyValidationException
import pl.allegro.tech.mongomigrationstream.infrastructure.mongo.MongoDbClients

private val logger = KotlinLogging.logger { }

internal class MigrationStartHandler(
    private val properties: ApplicationProperties,
    private val mongoDbClients: MongoDbClients,
    private val performerController: PerformerController,
) {

    fun start() {
        validateIfCanStart()
        startPerformers()
    }

    private fun validateIfCanStart() {
        logger.info { "Validating if mongo-migration-stream can perform migration" }
        try {
            ExternalDependenciesValidator.validateExternalDependencies(properties, mongoDbClients)
        } catch (exception: ExternalDependencyValidationException) {
            logger.error(exception) { "Validation failed when starting mongo-migration-stream" }
            throw MongoMigrationStreamStartException("Validation failed when starting mongo-migration-stream", exception)
        }
        logger.info { "Validation passed successfully" }
    }

    private fun startPerformers() {
        performerController.startPerformers()
    }
}
