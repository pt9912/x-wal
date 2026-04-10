package com.xwal.domain.service

import com.xwal.domain.exception.WorkflowValidationException
import com.xwal.domain.validation.IwmValidator
import com.xwal.domain.validation.ValidationResult

/**
 * Domain service for IWM validation with schema versioning support.
 */
class IwmValidationService(
    private val validator: IwmValidator = IwmValidator()
) {
    fun validate(iwmJson: String): ValidationResult =
        validator.validate(iwmJson)

    fun validateOrThrow(iwmJson: String) {
        val result = validator.validate(iwmJson)
        if (!result.valid) {
            throw WorkflowValidationException(
                message = result.message,
                errors = result.errors
            )
        }
    }
}
