package pl.allegro.tech.mongomigrationstream.core.sharding

import pl.allegro.tech.mongomigrationstream.core.mongo.DbCollection

internal data class ShardingInfo(
    val collectionShardingKey: Map<DbCollection, String>
)
