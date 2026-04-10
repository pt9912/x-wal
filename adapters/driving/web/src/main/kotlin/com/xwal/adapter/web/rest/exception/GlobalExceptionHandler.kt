package com.xwal.adapter.web.rest.exception

import com.xwal.adapter.web.rest.dto.response.ErrorResponse
import com.xwal.domain.exception.*
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Produces
import io.micronaut.http.server.exceptions.ExceptionHandler
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton
@Produces
class GlobalExceptionHandler : ExceptionHandler<Exception, HttpResponse<ErrorResponse>> {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    override fun handle(request: HttpRequest<*>, exception: Exception): HttpResponse<ErrorResponse> {
        val path = request.path

        return when (exception) {
            is WorkflowNotFoundException -> {
                val error = ErrorResponse.notFound(exception.message).copy(instance = path)
                HttpResponse.notFound(error)
            }
            is InstanceNotFoundException -> {
                val error = ErrorResponse.notFound(exception.message).copy(instance = path)
                HttpResponse.notFound(error)
            }
            is AdapterNotFoundException -> {
                val error = ErrorResponse.notFound(exception.message).copy(instance = path)
                HttpResponse.notFound(error)
            }
            is WorkflowValidationException -> {
                val validationErrors = exception.errors.map { ErrorResponse.ValidationError(message = it) }
                val error = ErrorResponse.badRequest(exception.message, validationErrors).copy(instance = path)
                HttpResponse.badRequest(error)
            }
            is DuplicateWorkflowException -> {
                val error = ErrorResponse.conflict(exception.message).copy(instance = path)
                HttpResponse.status<ErrorResponse>(io.micronaut.http.HttpStatus.CONFLICT).body(error)
            }
            is InvalidStateTransitionException -> {
                val error = ErrorResponse.conflict(exception.message).copy(instance = path)
                HttpResponse.status<ErrorResponse>(io.micronaut.http.HttpStatus.CONFLICT).body(error)
            }
            is AdapterUnavailableException -> {
                val error = ErrorResponse(title = "Service Unavailable", status = 503, detail = exception.message, instance = path)
                HttpResponse.status<ErrorResponse>(io.micronaut.http.HttpStatus.SERVICE_UNAVAILABLE).body(error)
            }
            is AdapterOperationException -> {
                log.error("Adapter operation failed: {}", exception.message, exception)
                val error = ErrorResponse.internalError(exception.message).copy(instance = path)
                HttpResponse.serverError(error)
            }
            is IllegalArgumentException -> {
                val error = ErrorResponse.badRequest(exception.message ?: "Invalid argument").copy(instance = path)
                HttpResponse.badRequest(error)
            }
            else -> {
                log.error("Unexpected error: {}", exception.message, exception)
                val error = ErrorResponse.internalError("An unexpected error occurred").copy(instance = path)
                HttpResponse.serverError(error)
            }
        }
    }
}
