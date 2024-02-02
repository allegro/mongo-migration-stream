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
            passwordConfigPath = null,
            isCompressionEnabled = false
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
            passwordConfigPath = passwordConfigPath,
            isCompressionEnabled = false
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
            passwordConfigPath = null,
            isCompressionEnabled = isCompressionEnabled
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
})
