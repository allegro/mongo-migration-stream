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
        val properties = MongoProperties("uri", "dbName")
        val collectionToMigrate = "collection"
        val dumpPath = "/dumpPath"
        val readPreference = "primary"
        val mongoDumpCommand = MongoDumpCommand(
            dbProperties = properties,
            dbCollection = DbCollection(properties.dbName, collectionToMigrate),
            mongoToolsPath = "",
            dumpPath = dumpPath,
            readPreference = readPreference,
            passwordConfigPath = null,
            isCompressionEnabled = false
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
            uri = "uri",
            dbName = "dbName",
            authenticationProperties = MongoAuthenticationProperties(
                username = "username",
                password = "password",
                authDbName = "admin"
            )
        )
        val collectionToMigrate = "collection"
        val dumpPath = "/dumpPath"
        val readPreference = "primary"
        val passwordConfigPath = "/tmp/mongomigrationstream/password_config/dump.config"
        val mongoDumpCommand = MongoDumpCommand(
            dbProperties = properties,
            dbCollection = DbCollection(properties.dbName, collectionToMigrate),
            mongoToolsPath = "",
            dumpPath = dumpPath,
            readPreference = readPreference,
            passwordConfigPath = passwordConfigPath,
            isCompressionEnabled = false
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

    should("should create dump command with gzip") {
        // given:
        val isCompressionEnabled = true
        val properties = MongoProperties("uri", "dbName")
        val collectionToMigrate = "collection"
        val dumpPath = "/dumpPath"
        val readPreference = "primary"
        val mongoDumpCommand = MongoDumpCommand(
            dbProperties = properties,
            dbCollection = DbCollection(properties.dbName, collectionToMigrate),
            mongoToolsPath = "",
            dumpPath = dumpPath,
            readPreference = readPreference,
            passwordConfigPath = null,
            isCompressionEnabled = isCompressionEnabled
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
            "--gzip"
        )
    }
})
