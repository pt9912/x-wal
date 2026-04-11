package com.xwal.decorator

import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer

/**
 * Generic tracing decorator that wraps any use case execution with an OpenTelemetry span.
 */
class TracedUseCaseDecorator<CMD, RESULT>(
    private val tracer: Tracer,
    private val operationName: String,
    private val delegate: (CMD) -> RESULT
) {
    fun execute(command: CMD): RESULT {
        val span = tracer.spanBuilder(operationName).startSpan()
        val scope = span.makeCurrent()
        return try {
            delegate(command)
        } catch (e: Exception) {
            span.setStatus(StatusCode.ERROR, e.message ?: "unknown error")
            span.recordException(e)
            throw e
        } finally {
            scope.close()
            span.end()
        }
    }
}
