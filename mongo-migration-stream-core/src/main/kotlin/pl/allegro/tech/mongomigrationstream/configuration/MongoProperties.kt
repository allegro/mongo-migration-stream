package pl.allegro.tech.mongomigrationstream.configuration

import java.time.Duration

private val DEFAULT_MONGO_TIMEOUT = Duration.ofSeconds(30)

data class MongoProperties(
    val uri: String,
    val dbName: String,
    val authenticationProperties: MongoAuthenticationProperties? = null,
    val timeoutProperties: MongoTimeoutProperties = MongoTimeoutProperties.DEFAULT, // TODO: Add possibility to read those values from properties file
) {
    override fun toString(): String = uri

    data class MongoAuthenticationProperties(
        val username: String,
        val password: String,
        val authDbName: String
    )

    data class MongoTimeoutProperties(
        val connectTimeout: Duration,
        val readTimeout: Duration,
        val serverSelectionTimeout: Duration,
    ) {
        companion object {
            val DEFAULT = MongoTimeoutProperties(
                connectTimeout = DEFAULT_MONGO_TIMEOUT,
                readTimeout = DEFAULT_MONGO_TIMEOUT,
                serverSelectionTimeout = DEFAULT_MONGO_TIMEOUT,
            )
        }
    }
}
