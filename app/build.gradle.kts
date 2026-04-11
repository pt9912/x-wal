plugins {
    alias(libs.plugins.micronaut.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.micronaut.test.resources)
    jacoco
}

dependencies {
    implementation(project(":hexagon:core"))
    implementation(project(":hexagon:ports"))
    implementation(project(":hexagon:application"))
    implementation(project(":adapters:driving:web"))
    implementation(project(":adapters:driven:persistence"))
    implementation(project(":adapters:driven:engine"))
    implementation(project(":adapters:driven:identity"))
    implementation(project(":adapters:driven:observability"))

    ksp("io.micronaut:micronaut-inject-kotlin")
    ksp("io.micronaut.openapi:micronaut-openapi")

    implementation(libs.otel.api)
    implementation(libs.swagger.annotations)

    runtimeOnly(libs.logback)
    runtimeOnly(libs.logstash.encoder)

    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgres)
    testImplementation(libs.assertj)
}

application {
    mainClass.set("com.xwal.Application")
}

micronaut {
    version(providers.gradleProperty("micronautVersion").get())
    runtime("netty")
    testRuntime("junit5")
}
