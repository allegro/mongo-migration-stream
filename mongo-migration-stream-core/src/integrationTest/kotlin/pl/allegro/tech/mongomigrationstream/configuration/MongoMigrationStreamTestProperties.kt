package pl.allegro.tech.mongomigrationstream.configuration

internal object MongoMigrationStreamTestProperties {
    const val ROOT_PATH = "/tmp/mongomigrationstream/"
    const val MONGO_TOOLS_PATH = ""
    const val SOURCE_DB_NAME = "sourceDb"
    const val DESTINATION_DB_NAME = "destinationDb"
    val sourceCollections = listOf("collection1", "collection2")
    val destinationCollections = listOf("collection1changedName", "collection2changedName")
}
