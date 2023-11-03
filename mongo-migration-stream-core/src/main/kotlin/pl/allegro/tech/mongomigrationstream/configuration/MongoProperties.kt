package pl.allegro.tech.mongomigrationstream.configuration

import com.mongodb.ReadPreference

data class MongoProperties(
    val uri: String,
    val dbName: String,
    val authenticationProperties: MongoAuthenticationProperties? = null,
    val connectTimeoutInSeconds: Int,
    val readTimeoutInSeconds: Int,
    val serverSelectionTimeoutInSeconds: Int,
    val readPreference: ReadPreference? = null,
) {
    override fun toString(): String = uri

    data class MongoAuthenticationProperties(
        val username: String,
        val password: String,
        val authDbName: String
    )
}
