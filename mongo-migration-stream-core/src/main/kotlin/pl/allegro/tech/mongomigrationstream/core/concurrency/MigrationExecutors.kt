package pl.allegro.tech.mongomigrationstream.core.concurrency

import pl.allegro.tech.mongomigrationstream.core.mongo.DbCollection
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

internal object MigrationExecutors {
    fun createPerformerExecutor(amountOfPerformers: Int): ExecutorService {
        val threadFactory = MigrationThreadFactories.performerThreadFactory()
        return Executors.newFixedThreadPool(amountOfPerformers, threadFactory)
    }

    fun createCommandRunnerLoggingExecutor(): ExecutorService {
        val threadFactory = MigrationThreadFactories.commandRunnerThreadFactory()
        return Executors.newCachedThreadPool(threadFactory)
    }

    fun createSynchronizationDetectorExecutor(): ExecutorService {
        val threadFactory = MigrationThreadFactories.synchronizationDetectorThreadFactory()
        return Executors.newCachedThreadPool(threadFactory)
    }

    fun createScheduledSynchronizationDetectorExecutor(): ScheduledExecutorService {
        val threadFactory = MigrationThreadFactories.synchronizationDetectorThreadFactory()
        return Executors.newSingleThreadScheduledExecutor(threadFactory)
    }

    fun createSourceToLocalExecutor(dbCollection: DbCollection): ExecutorService {
        val sourceToLocalThreadFactory = MigrationThreadFactories.sourceToLocalThreadFactory(dbCollection)
        return Executors.newSingleThreadExecutor(sourceToLocalThreadFactory)
    }

    fun createLocalToDestinationExecutor(dbCollection: DbCollection): ExecutorService {
        val localToDestinationThreadFactory = MigrationThreadFactories.localToDestinationThreadFactory(dbCollection)
        return Executors.newSingleThreadExecutor(localToDestinationThreadFactory)
    }

    fun createMigratorExecutor(dbCollection: DbCollection): ExecutorService {
        val migratorThreadFactory = MigrationThreadFactories.migratorThreadFactory(dbCollection)
        return Executors.newSingleThreadExecutor(migratorThreadFactory)
    }

    fun createIndexClonerExecutor(dbCollection: DbCollection): ExecutorService {
        val indexClonerThreadFactory = MigrationThreadFactories.indexClonerThreadFactory(dbCollection)
        return Executors.newCachedThreadPool(indexClonerThreadFactory)
    }
}
