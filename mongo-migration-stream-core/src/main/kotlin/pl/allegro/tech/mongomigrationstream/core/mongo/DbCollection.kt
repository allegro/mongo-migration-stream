package pl.allegro.tech.mongomigrationstream.core.mongo

data class DbCollection(
    val dbName: String,
    val collectionName: String
) {
    override fun toString(): String = "$dbName.$collectionName"
}
