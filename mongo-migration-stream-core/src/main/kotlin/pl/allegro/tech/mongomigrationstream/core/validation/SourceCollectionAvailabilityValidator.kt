package pl.allegro.tech.mongomigrationstream.core.validation

import com.mongodb.client.MongoDatabase
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger { }

internal class SourceCollectionAvailabilityValidator(
    private val dbClient: MongoDatabase,
    private val collectionsToMigrate: List<String>
) : Validator {

    override fun validate(): ValidationResult = runCatching {
        val allCollectionsFromDb = dbClient.listCollectionNames().toSet()
        val collectionsUnavailableOnDb = collectionsToMigrate.minus(allCollectionsFromDb)
        if (collectionsUnavailableOnDb.isNotEmpty()) {
            val errorMessage = "Non-existing collections: [$collectionsUnavailableOnDb]"
            logger.error { errorMessage }
            ValidationFailure(errorMessage)
        } else ValidationSuccess
    }.fold({ it }, { ValidationFailure("Cannot perform validation of source collections availability, cause: [$it]") })
}
