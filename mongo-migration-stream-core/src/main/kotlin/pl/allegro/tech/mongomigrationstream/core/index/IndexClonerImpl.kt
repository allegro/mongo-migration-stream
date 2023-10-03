package pl.allegro.tech.mongomigrationstream.core.index

import com.mongodb.MongoException
import com.mongodb.client.MongoDatabase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.bson.Document
import pl.allegro.tech.mongomigrationstream.core.concurrency.MigrationExecutors
import pl.allegro.tech.mongomigrationstream.core.mongo.SourceToDestination
import pl.allegro.tech.mongomigrationstream.core.performer.IndexCloner
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.IndexRebuildFinishEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.IndexRebuildStartEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateInfo
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

internal class IndexClonerImpl(
    private val sourceToDestination: SourceToDestination,
    private val sourceDb: MongoDatabase,
    private val destinationDb: MongoDatabase,
    private val stateInfo: StateInfo
) : IndexCloner {
    private val executor = MigrationExecutors.createIndexClonerExecutor(sourceToDestination.source)

    override fun cloneIndexes() {
        logger.info { "Cloning all indexes for collection: [${sourceToDestination.source.collectionName}]" }
        stateInfo.notifyStateChange(IndexRebuildStartEvent(sourceToDestination))

        executor.execute {
            getRawSourceIndexes(sourceToDestination).map {
                try { // TODO: this try-catch should be in createIndexOnDestinationCollection method
                    CompletableFuture.supplyAsync({ createIndexOnDestinationCollection(sourceToDestination, it) }, executor)
                } catch (exception: MongoException) {
                    logger.error(exception) { "Error when creating index [${it.toJson()}] - skipping this index creation for collection [${sourceToDestination.source.collectionName}]" }
                    CompletableFuture.completedFuture(Unit)
                }
            }.map { it.join() }

            stateInfo.notifyStateChange(IndexRebuildFinishEvent(sourceToDestination))
        }
    }

    private fun createIndexOnDestinationCollection(
        sourceToDestination: SourceToDestination,
        indexDefinition: Document
    ) {
        destinationDb.runCommand(
            Document().append("createIndexes", sourceToDestination.destination.collectionName)
                .append("indexes", listOf(indexDefinition))
        )
    }

    private fun getRawSourceIndexes(sourceToDestination: SourceToDestination): List<Document> =
        sourceDb.getCollection(sourceToDestination.source.collectionName).listIndexes()
            .toList()
            .filterNot { it.get("key", Document::class.java) == Document().append("_id", 1) }
            .map {
                it.remove("ns")
                it.remove("v")
                it["background"] = true
                it
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
