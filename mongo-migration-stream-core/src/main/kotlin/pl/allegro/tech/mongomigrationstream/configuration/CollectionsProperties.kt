package pl.allegro.tech.mongomigrationstream.configuration

data class CollectionsProperties(
    val sourceCollections: List<String>,
    val destinationCollections: List<String>
) {
    init {
        if (sourceCollections.size != destinationCollections.size)
            throw InvalidCollectionMappingException("Source and destination collection size should be equal. Source size: [${sourceCollections.size}], destination size: [${destinationCollections.size}]")
    }

    internal class InvalidCollectionMappingException(override val message: String) : RuntimeException(message)
}
