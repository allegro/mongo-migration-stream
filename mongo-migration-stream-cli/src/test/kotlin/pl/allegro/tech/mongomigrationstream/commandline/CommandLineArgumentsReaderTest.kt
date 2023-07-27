package pl.allegro.tech.mongomigrationstream.commandline

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import pl.allegro.tech.mongomigrationstream.commandline.CommandLineArgumentsReader.MissingCommandLineArgumentException

internal class CommandLineArgumentsReaderTest : ShouldSpec({

    should("throw MissingCommandLineArgumentException when no command line arguments provided") {
        val arguments = emptyArray<String>()
        shouldThrow<MissingCommandLineArgumentException> {
            CommandLineArgumentsReader.readArguments(arguments)
        }
    }

    should("Should throw MissingCommandLineArgumentException when single argument provided") {
        val arguments = arrayOf("--config")
        shouldThrow<MissingCommandLineArgumentException> {
            CommandLineArgumentsReader.readArguments(arguments)
        }
    }

    should("Should throw MissingCommandLineArgumentException when invalid arguments provided") {
        val arguments = arrayOf("--configuration")
        shouldThrow<MissingCommandLineArgumentException> {
            CommandLineArgumentsReader.readArguments(arguments)
        }
    }

    should("Should parse command line arguments when all required arguments provided") {
        val arguments = arrayOf("--config", "./path/to/application.properties")
        val result = shouldNotThrowAny {
            CommandLineArgumentsReader.readArguments(arguments)
        }
        result.configurationFilesPath.shouldContainExactly(
            "./path/to/application.properties"
        )
    }

    should("Should correctly parse config argument") {
        val arguments = arrayOf(
            "--config",
            "./config/application.properties,./config/custom.properties," +
                "./config/destination.properties,./config/source.properties"
        )
        val result = CommandLineArgumentsReader.readArguments(arguments)
        result.configurationFilesPath.shouldContainExactly(
            "./config/application.properties",
            "./config/custom.properties",
            "./config/destination.properties",
            "./config/source.properties"
        )
    }
})
