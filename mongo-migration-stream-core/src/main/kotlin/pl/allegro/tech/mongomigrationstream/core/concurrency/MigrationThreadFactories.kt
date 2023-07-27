package pl.allegro.tech.mongomigrationstream.core.concurrency

import org.apache.commons.lang3.concurrent.BasicThreadFactory
import pl.allegro.tech.mongomigrationstream.core.mongo.DbCollection

internal object MigrationThreadFactories {
    fun localToDestinationThreadFactory(source: DbCollection): BasicThreadFactory =
        nonDaemonThreadFactory("localToDestination-$source")

    fun migratorThreadFactory(source: DbCollection): BasicThreadFactory =
        nonDaemonThreadFactory("migrator-$source")

    fun indexClonerThreadFactory(source: DbCollection): BasicThreadFactory =
        nonDaemonThreadFactory("index-cloner-$source")

    fun sourceToLocalThreadFactory(source: DbCollection): BasicThreadFactory =
        nonDaemonThreadFactory("performer-$source")

    fun performerThreadFactory(): BasicThreadFactory =
        nonDaemonThreadFactory("performer-%d")

    fun commandRunnerThreadFactory(): BasicThreadFactory =
        daemonThreadFactory("commandRunnerLogger-%d")

    fun synchronizationDetectorThreadFactory(): BasicThreadFactory =
        daemonThreadFactory("synchronizationDetector-%d")

    private fun nonDaemonThreadFactory(pattern: String): BasicThreadFactory =
        BasicThreadFactory.Builder()
            .namingPattern(pattern)
            .daemon(false)
            .priority(Thread.MAX_PRIORITY)
            .build()

    private fun daemonThreadFactory(pattern: String): BasicThreadFactory =
        BasicThreadFactory.Builder()
            .namingPattern(pattern)
            .daemon(true)
            .priority(Thread.NORM_PRIORITY)
            .build()
}
