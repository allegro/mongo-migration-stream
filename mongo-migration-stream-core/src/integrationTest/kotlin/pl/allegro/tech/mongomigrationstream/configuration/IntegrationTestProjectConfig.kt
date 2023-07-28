package pl.allegro.tech.mongomigrationstream.configuration

import io.kotest.core.config.AbstractProjectConfig
import pl.allegro.tech.mongomigrationstream.configuration.MongoMigrationStreamTestProperties.DESTINATION_DB_NAME
import pl.allegro.tech.mongomigrationstream.configuration.MongoMigrationStreamTestProperties.SOURCE_DB_NAME

object IntegrationTestProjectConfig : AbstractProjectConfig() {
    val mongoExtension = MongoExtension(
        SOURCE_DB_NAME,
        DESTINATION_DB_NAME
    )

    override fun extensions() = listOf(
        mongoExtension
    )
}
