package pl.allegro.tech.mongomigrationstream.core.mongo

data class SourceToDestination(
    val source: DbCollection,
    val destination: DbCollection
) {
    override fun toString() = "source: $source, destination: $destination"
}
