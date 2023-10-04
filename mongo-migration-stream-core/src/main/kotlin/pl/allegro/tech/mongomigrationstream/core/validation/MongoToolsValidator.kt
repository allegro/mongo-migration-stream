package pl.allegro.tech.mongomigrationstream.core.validation

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import pl.allegro.tech.mongomigrationstream.configuration.ApplicationProperties
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isExecutable

private val logger = KotlinLogging.logger { }

internal class MongoToolsValidator(private val applicationProperties: ApplicationProperties) : Validator {

    private fun check(binary: String): Boolean {
        withLoggingContext("binary" to binary) {
            val binaryPath = Paths.get(applicationProperties.performerProperties.mongoToolsPath).resolve(binary)
            return if (binaryPath.exists() && binaryPath.isExecutable()) {
                logger.info { "$binary at $binaryPath exists and is executable" }
                true
            } else {
                logger.error { "$binary at $binaryPath doesn't exist or isn't executable" }
                false
            }
        }
    }

    override fun validate(): ValidationResult {
        val mongoDump = check("mongodump")
        val mongoRestore = check("mongorestore")
        return if (mongoDump && mongoRestore) {
            ValidationSuccess
        } else {
            ValidationFailure("MongoDB tools installation isn't working or doesn't exist")
        }
    }
}
