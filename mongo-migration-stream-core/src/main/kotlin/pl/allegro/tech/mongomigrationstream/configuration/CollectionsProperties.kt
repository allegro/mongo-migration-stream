package pl.allegro.tech.mongomigrationstream.configuration

data class CollectionsProperties(
    val sourceCollections: List<String>,
    val destinationCollections: List<String>
) {
    init {
        if (sourceCollections.size != destinationCollections.size)
            throw InvalidCollectionMappingException("Source and destination collection size should be equal. Source size: [${sourceCollections.size}], destination size: [${destinationCollections.size}]")
        if (sourceCollections.isEmpty())
            throw InvalidCollectionMappingException("Property 'collections.source' seems to be missing - please list all collections that should be migrated.")
    }

    internal class InvalidCollectionMappingException(override val message: String) : RuntimeException(message)
}
