package com.xwal.adapter.observability.metrics

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.Meter
import jakarta.inject.Singleton

/**
 * Engine adapter-specific metrics: call latency, success rate, error counts.
 */
@Singleton
class AdapterMetrics(meter: Meter) {

    private val callCounter = meter.counterBuilder("xwal.adapter.calls.total")
        .setDescription("Total adapter calls").build()

    private val errorCounter = meter.counterBuilder("xwal.adapter.calls.errors")
        .setDescription("Failed adapter calls").build()

    private val callDuration = meter.histogramBuilder("xwal.adapter.calls.duration_ms")
        .setDescription("Adapter call duration in milliseconds").ofLongs().build()

    fun recordCall(engineType: String, operation: String, durationMs: Long, success: Boolean) {
        val attrs = Attributes.of(
            AttributeKey.stringKey("engine.type"), engineType,
            AttributeKey.stringKey("operation"), operation
        )
        callCounter.add(1, attrs)
        callDuration.record(durationMs, attrs)
        if (!success) {
            errorCounter.add(1, attrs)
        }
    }
}
