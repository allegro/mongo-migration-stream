package pl.allegro.tech.mongomigrationstream.infrastructure.mongo

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoCredential
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsConnectionPoolListener
import pl.allegro.tech.mongomigrationstream.configuration.MongoProperties
import java.util.concurrent.TimeUnit.SECONDS
import com.mongodb.reactivestreams.client.MongoClient as ReactiveMongoClient
import com.mongodb.reactivestreams.client.MongoClients as ReactiveMongoClients

internal object MongoClientFactory {
    internal fun buildClient(
        mongoProperties: MongoProperties,
        meterRegistry: MeterRegistry
    ): MongoClient = MongoClients.create(mongoClientSettings(mongoProperties, meterRegistry))

    internal fun buildReactiveClient(
        mongoProperties: MongoProperties,
        meterRegistry: MeterRegistry
    ): ReactiveMongoClient = ReactiveMongoClients.create(mongoClientSettings(mongoProperties, meterRegistry))

    private fun mongoClientSettings(
        mongoProperties: MongoProperties,
        meterRegistry: MeterRegistry,
    ): MongoClientSettings = MongoClientSettings.builder()
        .applyConnectionString(ConnectionString(mongoProperties.uri))
        .applyToSocketSettings {
            it.connectTimeout(mongoProperties.timeoutProperties.connectTimeout.seconds.toInt(), SECONDS)
            it.readTimeout(mongoProperties.timeoutProperties.readTimeout.seconds.toInt(), SECONDS)
        }
        .applyToClusterSettings {
            it.serverSelectionTimeout(mongoProperties.timeoutProperties.serverSelectionTimeout.seconds, SECONDS)
        }
        .addCommandListener(MongoMetricsCommandListener(meterRegistry))
        .applyToConnectionPoolSettings {
            it.addConnectionPoolListener(MongoMetricsConnectionPoolListener(meterRegistry))
        }
        .let {
            if (mongoProperties.authenticationProperties != null) it.credential(
                MongoCredential.createCredential(
                    mongoProperties.authenticationProperties.username,
                    mongoProperties.authenticationProperties.authDbName,
                    mongoProperties.authenticationProperties.password.toCharArray()
                )
            )
            else it
        }.build()
}
