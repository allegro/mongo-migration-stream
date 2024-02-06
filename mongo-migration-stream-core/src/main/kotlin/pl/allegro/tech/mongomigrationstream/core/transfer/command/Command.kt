package pl.allegro.tech.mongomigrationstream.core.transfer.command

import pl.allegro.tech.mongomigrationstream.configuration.MongoProperties
import pl.allegro.tech.mongomigrationstream.configuration.MongoProperties.MongoAuthenticationProperties
import pl.allegro.tech.mongomigrationstream.core.mongo.DbCollection

// https://www.mongodb.com/docs/database-tools/mongorestore/#std-option-mongorestore.--numInsertionWorkersPerCollection
private const val DEFAULT_INSERTION_WORKERS_PER_COLLECTION = 1

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
        private val isCompressionEnabled: Boolean,
        private val passwordConfigPath: String?,
    ) : Command() {
        override fun prepareCommand(): List<String> = listOf(
            mongoToolsPath + "mongodump",
            "--uri", dbProperties.uri,
            "--db", dbCollection.dbName,
            "--collection", dbCollection.collectionName,
            "--out", dumpPath,
            "--readPreference", readPreference
        ) + gzipIfCompressionEnabled(isCompressionEnabled) + credentialsIfNotNull(
            dbProperties.authenticationProperties,
            passwordConfigPath
        )

        override fun commandName(): String {
            return "dump"
        }
    }

    internal class MongoRestoreCommand(
        private val dbProperties: MongoProperties,
        private val dbCollection: DbCollection,
        private val mongoToolsPath: String,
        private val dumpPath: String,
        private val isCompressionEnabled: Boolean,
        private val insertionWorkersPerCollection: Int,
        private val passwordConfigPath: String?,
    ) : Command() {
        override fun prepareCommand(): List<String> = listOf(
            mongoToolsPath + "mongorestore",
            "--uri", dbProperties.uri,
            "--db", dbCollection.dbName,
            "--collection", dbCollection.collectionName,
            "--dir", dumpPath,
            "--noIndexRestore"
        ) + gzipIfCompressionEnabled(
            isCompressionEnabled
        ) + insertionWorkersPerCollectionIfOtherThanDefault(
            insertionWorkersPerCollection
        ) + credentialsIfNotNull(
            dbProperties.authenticationProperties,
            passwordConfigPath
        )

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

    protected fun gzipIfCompressionEnabled(isCompressionEnabled: Boolean): List<String> =
        if (isCompressionEnabled) listOf("--gzip") else emptyList()

    protected fun insertionWorkersPerCollectionIfOtherThanDefault(insertionWorkersPerCollection: Int): List<String> =
        if (insertionWorkersPerCollection != DEFAULT_INSERTION_WORKERS_PER_COLLECTION) listOf(
            "--numInsertionWorkersPerCollection",
            insertionWorkersPerCollection.toString()
        ) else emptyList()
}
