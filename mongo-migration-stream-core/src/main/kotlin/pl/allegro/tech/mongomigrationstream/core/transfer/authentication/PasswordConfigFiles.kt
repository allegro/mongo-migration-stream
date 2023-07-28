package pl.allegro.tech.mongomigrationstream.core.transfer.authentication

internal data class PasswordConfigFiles(
    val sourceConfigPath: String?,
    val destinationConfigPath: String?
)
