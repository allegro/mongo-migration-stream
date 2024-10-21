package pl.allegro.tech.mongomigrationstream.infrastructure.mongo

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import io.micrometer.core.instrument.MeterRegistry
import pl.allegro.tech.mongomigrationstream.configuration.ApplicationProperties
import pl.allegro.tech.mongomigrationstream.configuration.MongoProperties
import com.mongodb.reactivestreams.client.MongoClient as ReactiveMongoClient
import com.mongodb.reactivestreams.client.MongoDatabase as ReactiveMongoDatabase

private const val CONFIG_DB = "config"

internal class MongoDbClients(
    properties: ApplicationProperties,
    meterRegistry: MeterRegistry,
) {
    private val sourceClient: MongoClient =
        createDbClient(properties.sourceDbProperties, meterRegistry)
    private val destinationClient: MongoClient =
        createDbClient(properties.destinationDbProperties, meterRegistry)
    private val reactiveSourceClient: ReactiveMongoClient =
        createReactiveDbClient(properties.sourceDbProperties, meterRegistry)
    private val reactiveDestinationClient: ReactiveMongoClient =
        createReactiveDbClient(properties.destinationDbProperties, meterRegistry)

    val sourceDatabase: MongoDatabase =
        sourceClient.getDatabase(properties.sourceDbProperties.dbName)
    val destinationDatabase: MongoDatabase =
        destinationClient.getDatabase(properties.destinationDbProperties.dbName)
    val destinationConfigDatabase: MongoDatabase = destinationClient.getDatabase(CONFIG_DB)
    val reactiveSourceDatabase: ReactiveMongoDatabase =
        reactiveSourceClient.getDatabase(properties.sourceDbProperties.dbName)
    val reactiveDestinationDatabase: ReactiveMongoDatabase =
        reactiveDestinationClient.getDatabase(properties.destinationDbProperties.dbName)

    private fun createDbClient(
        mongoProperties: MongoProperties,
        meterRegistry: MeterRegistry,
    ): MongoClient = MongoClientFactory.buildClient(mongoProperties, meterRegistry)

    private fun createReactiveDbClient(
        mongoProperties: MongoProperties,
        meterRegistry: MeterRegistry,
    ): ReactiveMongoClient = MongoClientFactory.buildReactiveClient(mongoProperties, meterRegistry)

    fun closeClients() {
        sourceClient.close()
        reactiveSourceClient.close()
        destinationClient.close()
        reactiveDestinationClient.close()
    }
}
