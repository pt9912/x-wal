plugins {
    alias(libs.plugins.micronaut.library)
    alias(libs.plugins.ksp)
}

dependencies {
    ksp("io.micronaut:micronaut-inject-kotlin")

    implementation("io.micronaut.tracing:micronaut-tracing-opentelemetry")
    implementation(libs.otel.api)
    implementation(libs.otel.sdk)
    implementation(libs.otel.exporter.otlp)
    implementation("io.micrometer:micrometer-core")
}

micronaut {
    version(providers.gradleProperty("micronautVersion").get())
}
