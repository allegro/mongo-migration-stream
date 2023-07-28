package pl.allegro.tech.mongomigrationstream.infrastructure.controller

class MongoMigrationStreamStartException(
    override val message: String,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)

class MongoMigrationStreamStopException(
    override val message: String,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)
