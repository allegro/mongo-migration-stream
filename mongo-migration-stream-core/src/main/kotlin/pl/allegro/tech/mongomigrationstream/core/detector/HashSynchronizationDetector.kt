package pl.allegro.tech.mongomigrationstream.core.detector

import com.mongodb.client.MongoDatabase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.bson.Document
import pl.allegro.tech.mongomigrationstream.core.mongo.SourceToDestination
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

private val logger = KotlinLogging.logger { }

internal class HashSynchronizationDetector(
    private val sourceDb: MongoDatabase,
    private val destinationDb: MongoDatabase,
    private val allCollectionsMapping: Set<SourceToDestination>,
    private val executor: Executor
) : SynchronizationDetector {
    override fun detect(): Set<DetectionResult> = runCatching {
        listOf(
            CompletableFuture.supplyAsync({ sourceDb.dbHash() }, executor),
            CompletableFuture.supplyAsync({ destinationDb.dbHash() }, executor)
        ).map { it.join() }
    }.onFailure { logger.warn { "Unable to perform detection with hash. Cause: [${it.message}]" } }
        .map {
            val (sourceCollectionHash: Map<String, String>, destinationCollectionHash: Map<String, String>) = it
            val hashesEquality: Map<SourceToDestination, Boolean> =
                allCollectionsMapping.associateWith {
                    (sourceCollectionHash[it.source.collectionName] == destinationCollectionHash[it.destination.collectionName])
                }

            hashesEquality.map { (sourceToDestination, isEqual) ->
                HashDetectionResult(sourceToDestination, isEqual)
            }.toSet()
        }.getOrElse { emptySet() }

    private fun MongoDatabase.dbHash(): Map<String, String> {
        val rawHashes: Document = runCommand(Document().append("dbHash", 1))["collections"] as Document
        return rawHashes.entries.associate {
            it.key to it.value.toString()
        }
    }
}
