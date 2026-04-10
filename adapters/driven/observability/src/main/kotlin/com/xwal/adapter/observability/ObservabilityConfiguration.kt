package com.xwal.adapter.observability

import io.micronaut.context.annotation.Factory
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Tracer
import jakarta.inject.Singleton

@Factory
class ObservabilityConfiguration {

    @Singleton
    fun tracer(openTelemetry: OpenTelemetry): Tracer =
        openTelemetry.getTracer("com.xwal.api", "2.0.0")

    @Singleton
    fun meter(openTelemetry: OpenTelemetry): Meter =
        openTelemetry.getMeter("com.xwal.api")
}
