plugins {
    alias(libs.plugins.micronaut.library)
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(project(":hexagon:ports"))
    implementation(project(":hexagon:core"))

    ksp("io.micronaut:micronaut-inject-kotlin")

    implementation("io.micronaut:micronaut-http-client")
    implementation(libs.jackson.kotlin)
    implementation(libs.camunda7.rest)
    implementation(libs.zeebe.client)
    implementation(libs.flowable.engine)
    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.retry)
    implementation(libs.otel.api)
    implementation(libs.jgrapht)

    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.assertj)
}

micronaut {
    version(providers.gradleProperty("micronautVersion").get())
}
