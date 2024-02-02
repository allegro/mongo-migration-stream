package pl.allegro.tech.mongomigrationstream.core.transfer.command

import pl.allegro.tech.mongomigrationstream.configuration.MongoProperties
import pl.allegro.tech.mongomigrationstream.configuration.MongoProperties.MongoAuthenticationProperties
import pl.allegro.tech.mongomigrationstream.core.mongo.DbCollection

internal sealed class Command {
    abstract fun prepareCommand(): List<String>

    /**
     * Code name of the running command for logging/metric purposes
     */
    abstract fun commandName(): String

    internal class MongoDumpCommand(
        private val dbProperties: MongoProperties,
        private val dbCollection: DbCollection,
        private val mongoToolsPath: String,
        private val dumpPath: String,
        private val readPreference: String,
        private val passwordConfigPath: String?,
        private val isCompressionEnabled: Boolean?,
    ) : Command() {
        override fun prepareCommand(): List<String> = listOf(
            mongoToolsPath + "mongodump",
            "--uri", dbProperties.uri,
            "--db", dbCollection.dbName,
            "--collection", dbCollection.collectionName,
            "--out", dumpPath,
            "--readPreference", readPreference
        ) + credentialsIfNotNull(
            dbProperties.authenticationProperties, passwordConfigPath
        ) + gzipIfNotNull(isCompressionEnabled)

        override fun commandName(): String {
            return "dump"
        }
    }

    internal class MongoRestoreCommand(
        private val dbProperties: MongoProperties,
        private val dbCollection: DbCollection,
        private val mongoToolsPath: String,
        private val dumpPath: String,
        private val passwordConfigPath: String?,
        private val isCompressionEnabled: Boolean?,
    ) : Command() {
        override fun prepareCommand(): List<String> = listOf(
            mongoToolsPath + "mongorestore",
            "--uri", dbProperties.uri,
            "--db", dbCollection.dbName,
            "--collection", dbCollection.collectionName,
            "--dir", dumpPath,
            "--noIndexRestore"
        ) + credentialsIfNotNull(
            dbProperties.authenticationProperties, passwordConfigPath
        ) + gzipIfNotNull(isCompressionEnabled)

        override fun commandName(): String {
            return "restore"
        }
    }

    protected fun credentialsIfNotNull(
        authenticationProperties: MongoAuthenticationProperties?,
        passwordConfigPath: String?
    ): List<String> =
        if (authenticationProperties != null && passwordConfigPath != null) {
            listOf(
                "--username", authenticationProperties.username,
                "--config", passwordConfigPath,
                "--authenticationDatabase", authenticationProperties.authDbName
            )
        } else emptyList()

    protected fun gzipIfNotNull(isCompressionEnabled: Boolean?): List<String> = if (isCompressionEnabled == true) listOf("--gzip") else emptyList()
}
