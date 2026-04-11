plugins {
    alias(libs.plugins.micronaut.library)
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(project(":hexagon:ports"))
    implementation(project(":hexagon:core"))

    ksp("io.micronaut:micronaut-inject-kotlin")
    ksp("io.micronaut.data:micronaut-data-processor")

    implementation("io.micronaut.data:micronaut-data-jdbc")
    implementation("io.micronaut.sql:micronaut-jdbc-hikari")
    implementation("io.micronaut.flyway:micronaut-flyway")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    implementation(libs.jackson.kotlin)
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("io.micronaut.test:micronaut-test-junit5")
    testImplementation("io.micronaut.serde:micronaut-serde-jackson")
    testRuntimeOnly(libs.logback)
    testRuntimeOnly("org.postgresql:postgresql")
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgres)
    testImplementation(libs.assertj)
}

micronaut {
    version(providers.gradleProperty("micronautVersion").get())
}
