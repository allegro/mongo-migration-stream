package pl.allegro.tech.mongomigrationstream.core.index

import com.mongodb.client.MongoDatabase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.bson.Document
import pl.allegro.tech.mongomigrationstream.core.concurrency.MigrationExecutors
import pl.allegro.tech.mongomigrationstream.core.mongo.SourceToDestination
import pl.allegro.tech.mongomigrationstream.core.performer.IndexCloner
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.IndexRebuildFinishEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.IndexRebuildStartEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateInfo

private val logger = KotlinLogging.logger {}

internal class IndexClonerImpl(
    private val sourceToDestination: SourceToDestination,
    private val sourceDb: MongoDatabase,
    private val destinationDb: MongoDatabase,
    private val stateInfo: StateInfo
) : IndexCloner {
    private val executor = MigrationExecutors.createIndexClonerExecutor(sourceToDestination.source)

    override fun cloneIndexes() {
        stateInfo.notifyStateChange(IndexRebuildStartEvent(sourceToDestination))
        executor.execute {
            logger.info { "Cloning all indexes for collection: [${sourceToDestination.source.collectionName}]" }
            getRawSourceIndexes(sourceToDestination).forEach { createIndexOnDestinationCollection(sourceToDestination, it) }
        }
        stateInfo.notifyStateChange(IndexRebuildFinishEvent(sourceToDestination))
    }

    private fun createIndexOnDestinationCollection(
        sourceToDestination: SourceToDestination,
        indexDefinition: Document
    ) {
        try {
            logger.info { "Creating index [${indexDefinition.toJson()}] on destination collection ${sourceToDestination.destination.collectionName}" }
            val createIndexesCommand: Document = Document()
                .append("createIndexes", sourceToDestination.destination.collectionName)
                .append("indexes", listOf(indexDefinition))
            destinationDb.runCommand(createIndexesCommand)
        } catch (t: Throwable) {
            // Swallowing exception to allow other indexes to be created
            logger.error(t) { "Error when creating index [${indexDefinition.toJson()}] - skipping this index creation for collection [${sourceToDestination.source.collectionName}]" }
        }
    }

    private fun getRawSourceIndexes(sourceToDestination: SourceToDestination): List<Document> {
        return try {
            sourceDb.getCollection(sourceToDestination.source.collectionName).listIndexes()
                .toList()
                .filterNot { it.get("key", Document::class.java) == Document().append("_id", 1) }
                .map {
                    it.remove("ns")
                    it.remove("v")
                    it["background"] = true
                    it
                }
        } catch (t: Throwable) {
            logger.error(t) { "Can't get indexes for source collection [${sourceToDestination.source.collectionName}]" }
            emptyList()
        }
    }

    override fun stop() {
        logger.info { "Trying to shut down IndexCloner gracefully..." }
        try {
            executor.shutdown()
        } catch (throwable: Throwable) {
            logger.warn(throwable) { "Exception while shutting down IndexCloner" }
        } finally {
            logger.info { "Shut down IndexCloner" }
        }
    }
}
