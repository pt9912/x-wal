package com.xwal.adapter.web.rest.dto.response

import io.micronaut.serde.annotation.Serdeable
import java.time.Instant

@Serdeable
data class ErrorResponse(
    val type: String = "about:blank",
    val title: String,
    val status: Int,
    val detail: String? = null,
    val instance: String? = null,
    val timestamp: Instant = Instant.now(),
    val errors: List<ValidationError> = emptyList()
) {
    @Serdeable
    data class ValidationError(
        val field: String? = null,
        val message: String? = null
    )

    companion object {
        fun badRequest(detail: String, errors: List<ValidationError> = emptyList()) = ErrorResponse(
            title = "Bad Request", status = 400, detail = detail, errors = errors
        )
        fun notFound(detail: String) = ErrorResponse(title = "Not Found", status = 404, detail = detail)
        fun conflict(detail: String) = ErrorResponse(title = "Conflict", status = 409, detail = detail)
        fun internalError(detail: String) = ErrorResponse(title = "Internal Server Error", status = 500, detail = detail)
    }
}
