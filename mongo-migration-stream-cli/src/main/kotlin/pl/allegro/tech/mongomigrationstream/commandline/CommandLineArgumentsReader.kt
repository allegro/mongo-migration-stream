package pl.allegro.tech.mongomigrationstream.commandline

import pl.allegro.tech.mongomigrationstream.commandline.CommandLineArguments.CONFIGURATION_PATH

internal object CommandLineArgumentsReader {
    fun readArguments(args: Array<String>): ParsedCommandLineArguments {
        val map = args.toList()
            .map { it.trim() }
            .windowed(2, 2)
            .associate { it[0].removePrefix("--") to it[1] }

        return ParsedCommandLineArguments.fromPlainCommandLineArguments(
            PlainCommandLineArguments(
                configurationFilesPath = map[CONFIGURATION_PATH]
                    ?: throw MissingCommandLineArgumentException(CONFIGURATION_PATH)
            )
        )
    }

    internal class MissingCommandLineArgumentException(argumentName: String) :
        RuntimeException("Missing command line argument: [$argumentName]")
}
