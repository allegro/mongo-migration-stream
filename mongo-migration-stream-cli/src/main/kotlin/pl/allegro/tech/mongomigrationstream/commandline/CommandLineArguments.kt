package pl.allegro.tech.mongomigrationstream.commandline

internal object CommandLineArguments {
    const val CONFIGURATION_PATH = "config"
}

internal data class PlainCommandLineArguments(
    val configurationFilesPath: String
)

internal data class ParsedCommandLineArguments(
    val configurationFilesPath: List<String>
) {
    companion object {
        fun fromPlainCommandLineArguments(plainCommandLineArguments: PlainCommandLineArguments) =
            ParsedCommandLineArguments(
                configurationFilesPath = plainCommandLineArguments.configurationFilesPath.split(",")
            )
    }
}
