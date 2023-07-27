package pl.allegro.tech.mongomigrationstream.core.cleanup

import pl.allegro.tech.mongomigrationstream.core.mongo.SourceToDestination
import pl.allegro.tech.mongomigrationstream.core.paths.MigrationPaths.DUMPS_DIR
import pl.allegro.tech.mongomigrationstream.core.paths.MigrationPaths.QUEUES_DIR
import pl.allegro.tech.mongomigrationstream.core.queue.EventQueue
import pl.allegro.tech.mongomigrationstream.core.synchronization.ChangeEvent
import pl.allegro.tech.mongomigrationstream.core.transfer.authentication.PasswordConfigFileGenerator
import java.io.File

internal class MigrationCleanup(
    private val rootPath: String,
    private val sourceDbName: String,
    private val queues: Map<SourceToDestination, EventQueue<ChangeEvent>>
) {
    fun cleanupAfterMigration() {
        val rootDirectory = File(rootPath)
        cleanupDumps(rootDirectory.resolve(DUMPS_DIR.removePrefix("/")))
        cleanupQueues(rootDirectory.resolve(QUEUES_DIR.removePrefix("/")))
        cleanupPasswordConfigs()
    }

    private fun cleanupDumps(dumpRootDirectory: File) {
        dumpRootDirectory.resolve(sourceDbName).deleteRecursively()
    }

    private fun cleanupQueues(queueRootDirectory: File) {
        queues.values.forEach { it.removeAll() }
        queueRootDirectory.listFiles { file -> file.name.startsWith(sourceDbName) }
            ?.forEach { it.deleteRecursively() }
    }

    private fun cleanupPasswordConfigs() {
        PasswordConfigFileGenerator.removeAll()
    }
}
