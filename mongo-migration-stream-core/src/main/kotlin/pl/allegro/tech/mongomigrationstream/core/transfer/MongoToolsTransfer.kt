package pl.allegro.tech.mongomigrationstream.core.transfer

import io.github.oshai.kotlinlogging.KotlinLogging
import pl.allegro.tech.mongomigrationstream.configuration.ApplicationProperties
import pl.allegro.tech.mongomigrationstream.core.concurrency.MigrationExecutors
import pl.allegro.tech.mongomigrationstream.core.mongo.DbCollection
import pl.allegro.tech.mongomigrationstream.core.mongo.SourceToDestination
import pl.allegro.tech.mongomigrationstream.core.paths.MigrationPaths
import pl.allegro.tech.mongomigrationstream.core.performer.Transfer
import pl.allegro.tech.mongomigrationstream.core.performer.TransferFailure
import pl.allegro.tech.mongomigrationstream.core.performer.TransferSuccess
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.DumpFinishEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.DumpStartEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.FailedEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.RestoreFinishEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateEvent.RestoreStartEvent
import pl.allegro.tech.mongomigrationstream.core.state.StateInfo
import pl.allegro.tech.mongomigrationstream.core.transfer.authentication.PasswordConfigFiles
import pl.allegro.tech.mongomigrationstream.core.transfer.command.Command
import pl.allegro.tech.mongomigrationstream.core.transfer.command.Command.MongoDumpCommand
import pl.allegro.tech.mongomigrationstream.core.transfer.command.Command.MongoRestoreCommand
import pl.allegro.tech.mongomigrationstream.core.transfer.command.CommandResult
import pl.allegro.tech.mongomigrationstream.core.transfer.command.CommandRunner
import pl.allegro.tech.mongomigrationstream.core.transfer.command.CommandRunnerInputStreamHandler
import pl.allegro.tech.mongomigrationstream.core.transfer.command.DumpCommandInputStreamHandler
import pl.allegro.tech.mongomigrationstream.core.transfer.command.RestoreCommandInputStreamHandler
import java.net.URLEncoder
import java.nio.file.Path
import kotlin.io.path.absolutePathString

private val logger = KotlinLogging.logger { }

internal class MongoToolsTransfer(
    properties: ApplicationProperties,
    private val sourceToDestination: SourceToDestination,
    private val passwordConfigFiles: PasswordConfigFiles,
    private val stateInfo: StateInfo,
) : Transfer {
    private val sourceDb = properties.sourceDbProperties
    private val destinationDb = properties.destinationDbProperties
    private val sourceDbCollection = sourceToDestination.source
    private val destinationDbCollection = sourceToDestination.destination
    private val mongoToolsPath = properties.performerProperties.mongoToolsPath
    private val absoluteDumpPath = Path.of(properties.performerProperties.rootPath, MigrationPaths.DUMPS_DIR).toAbsolutePath()
    private val dumpReadPreference = properties.performerProperties.dumpReadPreference
    private val isCompressionEnabled = properties.performerProperties.isCompressionEnabled
    private val executor = MigrationExecutors.createMigratorExecutor(sourceDbCollection)
    private val commandRunners: MutableList<CommandRunner> = mutableListOf()

    override fun performTransfer() = runCatching {
        logger.info { "Starting transfer of collection [$sourceDbCollection] from [$sourceDb] to [$destinationDb]" }

        stateInfo.notifyStateChange(DumpStartEvent(sourceToDestination))
        val dumpResult = runDump()
        if (!dumpResult.isSuccessful()) return@runCatching TransferFailure()
        stateInfo.notifyStateChange(DumpFinishEvent(sourceToDestination))
        stateInfo.notifyStateChange(RestoreStartEvent(sourceToDestination))
        val restoreResult = runRestore()
        val transferResult = if (restoreResult.isSuccessful()) TransferSuccess else TransferFailure()
        if (transferResult is TransferSuccess) stateInfo.notifyStateChange(RestoreFinishEvent(sourceToDestination))

        logger.info { "Finished transfer of collection [$sourceDbCollection from [$sourceDb] to [$destinationDb]" }

        transferResult
    }
        .onFailure { logger.error(it) { "Failed to transfer collections using MongoToolsTransfer" } }
        .getOrElse { TransferFailure(it) }
        .apply {
            if (this is TransferFailure) stateInfo.notifyStateChange(FailedEvent(sourceToDestination, cause))
        }

    private fun runCommand(
        command: Command,
        dbCollection: DbCollection,
        commandRunnerInputStreamHandler: CommandRunnerInputStreamHandler
    ): CommandResult {
        logger.info { "Start transfer [${command.commandName()}] of database: [${dbCollection.dbName}], collection: [${dbCollection.collectionName}]" }
        val commandRunner = CommandRunner(commandRunnerInputStreamHandler).also {
            commandRunners.add(it)
        }
        val dumpResult = commandRunner.runCommand(command)
        logger.info { "Finished transfer [${command.commandName()}] of database: [${dbCollection.dbName}], collection: [${dbCollection.collectionName}]" }
        return dumpResult
    }

    override fun stop() {
        logger.info { "Trying to shut down MongoToolsTransfer gracefully..." }
        try {
            commandRunners.forEach { it.stopRunningCommand() }
            commandRunners.clear()
            executor.shutdown()
        } catch (throwable: Throwable) {
            logger.warn(throwable) { "Exception while shutting down MongoToolsTransfer" }
        } finally {
            logger.info { "Shut down MongoToolsTransfer" }
        }
    }

    private fun runDump(): CommandResult = runCommand(
        prepareDumpCommand(),
        sourceDbCollection,
        DumpCommandInputStreamHandler(stateInfo, sourceToDestination)
    )

    private fun prepareDumpCommand(): Command = MongoDumpCommand(
        dbProperties = sourceDb,
        dbCollection = sourceDbCollection,
        mongoToolsPath = mongoToolsPath,
        dumpPath = absoluteDumpPath.toString(),
        readPreference = dumpReadPreference.name,
        passwordConfigPath = passwordConfigFiles.sourceConfigPath,
        isCompressionEnabled = isCompressionEnabled
    )

    private fun runRestore(): CommandResult = runCommand(
        prepareRestoreCommand(),
        destinationDbCollection,
        RestoreCommandInputStreamHandler(stateInfo, sourceToDestination),
    )

    private fun prepareRestoreCommand(): Command = MongoRestoreCommand(
        dbProperties = destinationDb,
        dbCollection = destinationDbCollection,
        mongoToolsPath = mongoToolsPath,
        dumpPath = resolveCreatedDumpPath(absoluteDumpPath),
        passwordConfigPath = passwordConfigFiles.destinationConfigPath,
        isCompressionEnabled = isCompressionEnabled
    )

    private fun resolveCreatedDumpPath(absoluteDumpPath: Path): String = absoluteDumpPath
        .resolve(sourceDbCollection.dbName)
        // Due to issue with mongodump: https://www.mongodb.com/community/forums/t/mongodump-escaping-special-collection-characters/9233
        .resolve("${URLEncoder.encode(sourceDbCollection.collectionName, "UTF-8")}.bson")
        .absolutePathString()

    private fun CommandResult.isSuccessful(): Boolean = this.exitCode == 0
}
