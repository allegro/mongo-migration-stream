package pl.allegro.tech.mongomigrationstream.configuration

data class MongoProperties(
    val uri: String,
    val dbName: String,
    val authenticationProperties: MongoAuthenticationProperties? = null
) {
    override fun toString(): String = uri

    data class MongoAuthenticationProperties(
        val username: String,
        val password: String,
        val authDbName: String
    )
}
