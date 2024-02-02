package pl.allegro.tech.mongomigrationstream.core.transfer.command

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import pl.allegro.tech.mongomigrationstream.configuration.MongoProperties
import pl.allegro.tech.mongomigrationstream.configuration.MongoProperties.MongoAuthenticationProperties
import pl.allegro.tech.mongomigrationstream.core.mongo.DbCollection
import pl.allegro.tech.mongomigrationstream.core.transfer.command.Command.MongoRestoreCommand

internal class RestoreCommandTest : ShouldSpec({
    should("create restore command") {
        // Given:
        val properties = MongoProperties("uri", "dbName")
        val collectionToMigrate = "collection"
        val restorePath = "/restorePath"
        val mongoRestoreCommand = MongoRestoreCommand(
            dbProperties = properties,
            dbCollection = DbCollection(properties.dbName, collectionToMigrate),
            mongoToolsPath = "",
            dumpPath = restorePath,
            isCompressionEnabled = false,
            insertionWorkersPerCollection = 1,
            passwordConfigPath = null,
        )
        // when:
        val terminalCommand = mongoRestoreCommand.prepareCommand()
        // then:
        terminalCommand.shouldContainExactly(
            "mongorestore",
            "--uri", properties.uri,
            "--db", properties.dbName,
            "--collection", collectionToMigrate,
            "--dir", restorePath,
            "--noIndexRestore",
        )
    }

    should("create restore command with auth data") {
        // given:
        val properties = MongoProperties(
            uri = "uri",
            dbName = "dbName",
            authenticationProperties = MongoAuthenticationProperties(
                username = "username",
                password = "password",
                authDbName = "admin"
            )
        )
        val collectionToMigrate = "collection"
        val restorePath = "/restorePath"
        val passwordConfigPath = "/tmp/mongomigrationstream/password_config/restore.config"
        val mongoRestoreCommand = MongoRestoreCommand(
            dbProperties = properties,
            dbCollection = DbCollection(properties.dbName, collectionToMigrate),
            mongoToolsPath = "",
            dumpPath = restorePath,
            isCompressionEnabled = false,
            insertionWorkersPerCollection = 1,
            passwordConfigPath = passwordConfigPath,
        )
        // when:
        val terminalCommand = mongoRestoreCommand.prepareCommand()
        // then:
        terminalCommand.shouldContainExactly(
            "mongorestore",
            "--uri", properties.uri,
            "--db", properties.dbName,
            "--collection", collectionToMigrate,
            "--dir", restorePath,
            "--noIndexRestore",
            "--username", properties.authenticationProperties!!.username,
            "--config", passwordConfigPath,
            "--authenticationDatabase", properties.authenticationProperties!!.authDbName,
        )
    }

    should("create restore command with gzip") {
        // given:
        val isCompressionEnabled = true
        val properties = MongoProperties("uri", "dbName")
        val collectionToMigrate = "collection"
        val restorePath = "/restorePath"
        val mongoRestoreCommand = MongoRestoreCommand(
            dbProperties = properties,
            dbCollection = DbCollection(properties.dbName, collectionToMigrate),
            mongoToolsPath = "",
            dumpPath = restorePath,
            isCompressionEnabled = isCompressionEnabled,
            insertionWorkersPerCollection = 1,
            passwordConfigPath = null,
        )
        // when:
        val terminalCommand = mongoRestoreCommand.prepareCommand()
        // then:
        terminalCommand.shouldContainExactly(
            "mongorestore",
            "--uri", properties.uri,
            "--db", properties.dbName,
            "--collection", collectionToMigrate,
            "--dir", restorePath,
            "--noIndexRestore",
            "--gzip"
        )
    }

    should("create restore command with number of insertion workers per collection") {
        // given:
        val numberOfInsertionWorkersPerCollection = 5
        val properties = MongoProperties("uri", "dbName")
        val collectionToMigrate = "collection"
        val restorePath = "/restorePath"
        val mongoRestoreCommand = MongoRestoreCommand(
            dbProperties = properties,
            dbCollection = DbCollection(properties.dbName, collectionToMigrate),
            mongoToolsPath = "",
            dumpPath = restorePath,
            isCompressionEnabled = false,
            insertionWorkersPerCollection = numberOfInsertionWorkersPerCollection,
            passwordConfigPath = null,
        )
        // when:
        val terminalCommand = mongoRestoreCommand.prepareCommand()
        // then:
        terminalCommand.shouldContainExactly(
            "mongorestore",
            "--uri", properties.uri,
            "--db", properties.dbName,
            "--collection", collectionToMigrate,
            "--dir", restorePath,
            "--noIndexRestore",
            "--numInsertionWorkersPerCollection", numberOfInsertionWorkersPerCollection.toString()
        )
    }
})
