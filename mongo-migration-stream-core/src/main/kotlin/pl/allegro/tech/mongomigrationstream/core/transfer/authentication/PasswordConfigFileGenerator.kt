package pl.allegro.tech.mongomigrationstream.core.transfer.authentication

import io.github.oshai.kotlinlogging.KotlinLogging
import pl.allegro.tech.mongomigrationstream.core.paths.MigrationPaths
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission.OWNER_READ
import java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText

private val logger = KotlinLogging.logger { }

internal object PasswordConfigFileGenerator {
    private val generatedPasswordConfigFiles = mutableListOf<Path>()

    fun generatePasswordConfigFile(rootPath: String, db: String, password: String): Path {
        try {
            val file = createTempFile(rootPath, db)
            fillFileWithConfiguration(file, password)
            generatedPasswordConfigFiles.add(file)
            return file
        } catch (exception: Exception) {
            logger.error(exception) { "Error when creating password config file for db: [$db]" }
            throw PasswordConfigFileGenerationException(db, exception)
        }
    }

    private fun createTempFile(rootPath: String, fileName: String): Path {
        val dirAbsolutePath = Paths.get(rootPath, MigrationPaths.PASSWORD_CONFIG_DIR)
        Files.createDirectories(dirAbsolutePath)
        return Files.createTempFile(
            dirAbsolutePath,
            fileName,
            ".config",
            PosixFilePermissions.asFileAttribute(
                setOf(OWNER_READ, OWNER_WRITE)
            )
        )
    }

    private fun fillFileWithConfiguration(file: Path, password: String) {
        file.writeText("password: $password")
    }

    fun removeAll(): Boolean {
        generatedPasswordConfigFiles.forEach { it.deleteIfExists() }
        generatedPasswordConfigFiles.clear()
        return true
    }
}

internal class PasswordConfigFileGenerationException(dbName: String, cause: Throwable) :
    RuntimeException("Exception when creating password config file for db: [$dbName]", cause)
