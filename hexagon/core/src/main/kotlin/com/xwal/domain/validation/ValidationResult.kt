package com.xwal.domain.validation

data class ValidationResult(
    val valid: Boolean,
    val source: String?,
    val message: String,
    val errors: List<String> = emptyList()
) {
    companion object {
        fun success(source: String): ValidationResult =
            ValidationResult(valid = true, source = source, message = "Validation successful")

        fun failure(source: String, errors: List<String>): ValidationResult =
            ValidationResult(
                valid = false,
                source = source,
                message = "Validation failed: $source (${errors.size} errors)",
                errors = errors
            )

        fun failure(message: String): ValidationResult =
            ValidationResult(valid = false, source = null, message = message)

        fun error(message: String): ValidationResult =
            ValidationResult(valid = false, source = null, message = message)
    }
}
