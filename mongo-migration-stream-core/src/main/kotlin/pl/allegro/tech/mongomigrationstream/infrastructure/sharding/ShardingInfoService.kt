package pl.allegro.tech.mongomigrationstream.infrastructure.sharding

import io.github.oshai.kotlinlogging.KotlinLogging
import org.bson.Document
import pl.allegro.tech.mongomigrationstream.core.mongo.DbCollection
import pl.allegro.tech.mongomigrationstream.core.sharding.ShardingInfo
import pl.allegro.tech.mongomigrationstream.infrastructure.mongo.MongoDbClients

/*
    This class is responsible for extracting sharding keys from destination database
    It does so by querying "config.collections" collection, and parsing the output
    The assumption is that sharding key is the first one in the "key" document
    MongoDB reference: https://www.mongodb.com/docs/manual/reference/config-database/
 */

internal object ShardingInfoService {
    private val logger = KotlinLogging.logger { }

    fun getShardingInfoFromDestinationDatabase(mongoDbClients: MongoDbClients): ShardingInfo {
        val shardingInfoMap = mutableMapOf<DbCollection, String>()
        try {
            mongoDbClients.destinationConfigDatabase.getCollection("collections").find()
                .forEach { document ->
                    val dbCollection: DbCollection? = dbNameDotCollectionNameToDbCollection(document)
                    val shardingKey: String? = getShardingKey(document)
                    if (dbCollection != null && shardingKey != null) {
                        shardingInfoMap[dbCollection] = shardingKey
                    }
                }
        } catch (exception: Exception) {
            logger.error(exception) { "Error when getting sharding info from destination database." }
        }

        logger.info { "Info regarding sharding keys on destination database: $shardingInfoMap" }
        return ShardingInfo(shardingInfoMap)
    }

    private fun dbNameDotCollectionNameToDbCollection(document: Document): DbCollection? {
        try {
            val dbNameDotCollectionName = document.getString("_id")
            val (dbName, collectionName) = dbNameDotCollectionName.split(".")
            return DbCollection(dbName, collectionName)
        } catch (exception: Exception) {
            logger.error(exception) { "Could not parse [${document.toJson()}] to retrieve dbName.collectionName" }
            return null
        }
    }

    private fun getShardingKey(document: Document): String? {
        try {
            val keyDocument: Map<*, *>? = document["key"] as Map<*, *>?
            return keyDocument?.keys?.firstOrNull() as String?
        } catch (exception: Exception) {
            logger.error(exception) { "Could not parse [${document.toJson()}] to retrieve sharding key" }
            return null
        }
    }
}
