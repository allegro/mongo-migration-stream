package pl.allegro.tech.mongomigrationstream.configuration

import pl.allegro.tech.mongomigrationstream.core.mongo.DbCollection
import pl.allegro.tech.mongomigrationstream.core.mongo.SourceToDestination

data class ApplicationProperties(
    val generalProperties: GeneralProperties,
    val sourceDbProperties: MongoProperties,
    val destinationDbProperties: MongoProperties,
    val collectionsProperties: CollectionsProperties,
    val performerProperties: PerformerProperties,
    val stateConfig: StateConfig
) {
    val sourceToDestinationMapping: Set<SourceToDestination> =
        extractSourceToDestinationProperties(sourceDbProperties, destinationDbProperties)

    private fun extractSourceToDestinationProperties(
        source: MongoProperties,
        destination: MongoProperties
    ): Set<SourceToDestination> = collectionsProperties.sourceCollections.zip(collectionsProperties.destinationCollections)
        .map {
            SourceToDestination(
                DbCollection(source.dbName, it.first),
                DbCollection(destination.dbName, it.second)
            )
        }.toSet()
}
