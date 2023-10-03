package pl.allegro.tech.mongomigrationstream.core.validation

import com.mongodb.client.MongoDatabase
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger { }

internal class DestinationMissingCollectionValidator(
    private val destinationDbClient: MongoDatabase,
    private val destinationCollections: List<String>
) : Validator {
    override fun validate(): ValidationResult = runCatching {
        val allCollectionsFromDb = destinationDbClient.listCollectionNames().toSet()
        val nonMissingCollections = allCollectionsFromDb.filter { destinationCollections.contains(it) }
        if (nonMissingCollections.isNotEmpty()) {
            val errorMessage = "Destination DB has collections which should be missing: [$nonMissingCollections]"
            logger.error { errorMessage }
            ValidationFailure(errorMessage)
        } else ValidationSuccess
    }.fold({ it }, { ValidationFailure("Cannot perform validation of destination collection missing, cause: [$it]") })
}
