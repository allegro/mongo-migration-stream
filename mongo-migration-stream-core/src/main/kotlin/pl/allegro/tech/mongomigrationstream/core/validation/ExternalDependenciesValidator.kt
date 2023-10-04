package pl.allegro.tech.mongomigrationstream.core.validation

import pl.allegro.tech.mongomigrationstream.configuration.ApplicationProperties
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties.DbAvailabilityValidatorType
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties.DestinationMissingCollectionType
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties.MongoToolsValidatorType
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties.SourceCollectionAvailabilityType
import pl.allegro.tech.mongomigrationstream.configuration.GeneralProperties.ValidatorType
import pl.allegro.tech.mongomigrationstream.infrastructure.mongo.MongoDbClients

internal object ExternalDependenciesValidator {
    fun validateExternalDependencies(
        properties: ApplicationProperties,
        mongoDbClients: MongoDbClients
    ) {
        val validators = properties.generalProperties.databaseValidators.flatMap { validatorType ->
            createValidator(validatorType, mongoDbClients, properties)
        }

        performValidation(validators)
    }

    private fun createValidator(
        validatorType: ValidatorType,
        mongoDbClients: MongoDbClients,
        properties: ApplicationProperties
    ): List<Validator> = when (validatorType) {
        DbAvailabilityValidatorType -> listOf(
            DbAvailabilityValidator(mongoDbClients.sourceDatabase),
            DbAvailabilityValidator(mongoDbClients.destinationDatabase)
        )

        SourceCollectionAvailabilityType -> listOf(
            SourceCollectionAvailabilityValidator(
                mongoDbClients.sourceDatabase,
                properties.collectionsProperties.sourceCollections
            )
        )

        DestinationMissingCollectionType -> listOf(
            DestinationMissingCollectionValidator(
                mongoDbClients.destinationDatabase,
                properties.collectionsProperties.destinationCollections
            )
        )

        MongoToolsValidatorType -> listOf(
            MongoToolsValidator(properties)
        )
    }

    private fun performValidation(validators: List<Validator>) {
        validators.map { it.validate() }
            .filterIsInstance<ValidationFailure>()
            .let {
                if (it.isNotEmpty()) throw ExternalDependencyValidationException(it)
            }
    }
}

internal class ExternalDependencyValidationException(causes: List<ValidationFailure>) :
    RuntimeException(causes.joinToString { it.message })
