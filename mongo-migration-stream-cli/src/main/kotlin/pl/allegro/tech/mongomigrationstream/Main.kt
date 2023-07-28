package pl.allegro.tech.mongomigrationstream

import pl.allegro.tech.mongomigrationstream.commandline.CommandLineArgumentsReader
import pl.allegro.tech.mongomigrationstream.configuration.ApplicationProperties
import pl.allegro.tech.mongomigrationstream.logger.LoggerConfiguration
import pl.allegro.tech.mongomigrationstream.metrics.JmxConfiguration
import pl.allegro.tech.mongomigrationstream.properties.ApplicationPropertiesReader

fun main(args: Array<String>) {

    val commandLineArguments = CommandLineArgumentsReader.readArguments(args)
    val properties: ApplicationProperties = ApplicationPropertiesReader.readApplicationProperties(
        commandLineArguments.configurationFilesPath
    )
    LoggerConfiguration.configureLogger(properties.performerProperties.rootPath)
    val mongoMigrationStream = MongoMigrationStream(properties, JmxConfiguration.jmxMeterRegistry)
    mongoMigrationStream.start()
}
