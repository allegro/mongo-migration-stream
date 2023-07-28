package pl.allegro.tech.mongomigrationstream.core.validation

import com.mongodb.client.MongoDatabase
import mu.KotlinLogging
import org.bson.Document

private val logger = KotlinLogging.logger { }

internal class DbAvailabilityValidator(private val dbClient: MongoDatabase) : Validator {
    private val errorMessage = "Database is not available"
    override fun validate(): ValidationResult = runCatching { dbClient.runCommand(Document().append("ping", 1)) }
        .onFailure { logger.error(it) { errorMessage } }
        .fold({ ValidationSuccess }, { ValidationFailure(errorMessage) })
}
