package pl.allegro.tech.mongomigrationstream.core.transfer.command

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import pl.allegro.tech.mongomigrationstream.configuration.MongoProperties
import pl.allegro.tech.mongomigrationstream.core.mongo.DbCollection
import pl.allegro.tech.mongomigrationstream.core.transfer.command.Command.MongoRestoreCommand

internal class RestoreCommandTest : ShouldSpec({
    should("create restore command") {
        // Given:
        val properties = MongoProperties(
            "uri",
            "dbName",
            connectTimeoutInSeconds = 10,
            readTimeoutInSeconds = 10,
            serverSelectionTimeoutInSeconds = 10,
        )
        val collectionToMigrate = "collection"
        val restorePath = "/restorePath"
        val mongoRestoreCommand = MongoRestoreCommand(
            properties,
            DbCollection(properties.dbName, collectionToMigrate),
            "",
            restorePath,
            null
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
            "uri",
            "dbName",
            MongoProperties.MongoAuthenticationProperties(
                "username",
                "password",
                "admin"
            ),
            connectTimeoutInSeconds = 10,
            readTimeoutInSeconds = 10,
            serverSelectionTimeoutInSeconds = 10,
        )
        val collectionToMigrate = "collection"
        val restorePath = "/restorePath"
        val passwordConfigPath = "/tmp/mongomigrationstream/password_config/restore.config"
        val mongoRestoreCommand = MongoRestoreCommand(
            properties,
            DbCollection(properties.dbName, collectionToMigrate),
            "",
            restorePath,
            passwordConfigPath
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
})
