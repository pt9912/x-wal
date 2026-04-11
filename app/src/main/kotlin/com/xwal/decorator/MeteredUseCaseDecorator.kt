package com.xwal.decorator

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.Meter

/**
 * Generic metrics decorator that records counter + duration for any use case execution.
 * Used in @Factory classes to wrap use case implementations transparently.
 */
class MeteredUseCaseDecorator<CMD, RESULT>(
    meter: Meter,
    private val operationName: String,
    private val delegate: (CMD) -> RESULT
) {
    private val counter = meter.counterBuilder("${operationName}.total").build()
    private val errorCounter = meter.counterBuilder("${operationName}.errors").build()
    private val duration = meter.histogramBuilder("${operationName}.duration_ms").ofLongs().build()

    fun execute(command: CMD): RESULT {
        val start = System.currentTimeMillis()
        counter.add(1)
        return try {
            delegate(command)
        } catch (e: Exception) {
            errorCounter.add(1, Attributes.of(AttributeKey.stringKey("error.type"), e.javaClass.simpleName))
            throw e
        } finally {
            duration.record(System.currentTimeMillis() - start)
        }
    }
}
