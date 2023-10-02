package pl.allegro.tech.mongomigrationstream.core.transfer.command

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import pl.allegro.tech.mongomigrationstream.configuration.MongoProperties
import pl.allegro.tech.mongomigrationstream.configuration.MongoProperties.MongoAuthenticationProperties
import pl.allegro.tech.mongomigrationstream.core.mongo.DbCollection
import pl.allegro.tech.mongomigrationstream.core.transfer.command.Command.MongoDumpCommand

internal class DumpCommandTest : ShouldSpec({
    should("create dump command") {
        // Given:
        val properties = MongoProperties(
            "uri",
            "dbName",
            connectTimeoutInSeconds = 10,
            readTimeoutInSeconds = 10,
            serverSelectionTimeoutInSeconds = 10,
        )
        val collectionToMigrate = "collection"
        val dumpPath = "/dumpPath"
        val readPreference = "primary"
        val mongoDumpCommand = MongoDumpCommand(
            properties,
            DbCollection(properties.dbName, collectionToMigrate),
            "",
            dumpPath,
            readPreference,
            null
        )
        // when:
        val terminalCommand = mongoDumpCommand.prepareCommand()
        // then:
        terminalCommand.shouldContainExactly(
            "mongodump",
            "--uri", properties.uri,
            "--db", properties.dbName,
            "--collection", collectionToMigrate,
            "--out", dumpPath,
            "--readPreference", readPreference,
        )
    }

    should("should create dump command with auth data") {
        // given:
        val properties = MongoProperties(
            "uri",
            "dbName",
            MongoAuthenticationProperties(
                "username",
                "password",
                "admin"
            ),
            connectTimeoutInSeconds = 10,
            readTimeoutInSeconds = 10,
            serverSelectionTimeoutInSeconds = 10,
        )
        val collectionToMigrate = "collection"
        val dumpPath = "/dumpPath"
        val readPreference = "primary"
        val passwordConfigPath = "/tmp/mongomigrationstream/password_config/dump.config"
        val mongoDumpCommand = MongoDumpCommand(
            properties,
            DbCollection(properties.dbName, collectionToMigrate),
            "",
            dumpPath,
            readPreference,
            passwordConfigPath,
        )
        // when:
        val terminalCommand = mongoDumpCommand.prepareCommand()
        // then:
        terminalCommand.shouldContainExactly(
            "mongodump",
            "--uri", properties.uri,
            "--db", properties.dbName,
            "--collection", collectionToMigrate,
            "--out", dumpPath,
            "--readPreference", readPreference,
            "--username", properties.authenticationProperties!!.username,
            "--config", passwordConfigPath,
            "--authenticationDatabase", properties.authenticationProperties!!.authDbName,
        )
    }
})
