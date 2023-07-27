package pl.allegro.tech.mongomigrationstream.core.transfer.command

import pl.allegro.tech.mongomigrationstream.core.concurrency.MigrationExecutors
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.CompletableFuture

internal class CommandRunner(
    private val inputStreamHandler: CommandRunnerInputStreamHandler
) {
    private val commandRunnerLoggingExecutor = MigrationExecutors.createCommandRunnerLoggingExecutor()
    private lateinit var currentProcess: Process

    fun runCommand(command: Command): CommandResult {
        val processBuilder = ProcessBuilder().command(command.prepareCommand())
        currentProcess = processBuilder.start()

        CompletableFuture.runAsync(
            { startHandlingContentOfInputStream(currentProcess.inputStream) },
            commandRunnerLoggingExecutor
        )
        CompletableFuture.runAsync(
            { startHandlingContentOfInputStream(currentProcess.errorStream) },
            commandRunnerLoggingExecutor
        )

        val exitCode = currentProcess.waitFor()
        stopRunningCommand()
        return CommandResult(exitCode)
    }

    fun stopRunningCommand() {
        try {
            if (this::currentProcess.isInitialized) currentProcess.destroy()
            commandRunnerLoggingExecutor.shutdown()
        } catch (_: Throwable) {
            // Do nothing
        }
    }

    private fun startHandlingContentOfInputStream(inputStream: InputStream) {
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        while (true) {
            val line = bufferedReader.readLine() ?: break
            inputStreamHandler.handle(line)
        }
    }
}
