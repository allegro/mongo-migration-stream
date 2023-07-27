package pl.allegro.tech.mongomigrationstream.core.validation

internal interface Validator {
    fun validate(): ValidationResult
}

sealed class ValidationResult
object ValidationSuccess : ValidationResult()
data class ValidationFailure(val message: String) : ValidationResult()
