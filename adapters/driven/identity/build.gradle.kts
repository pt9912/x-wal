plugins {
    alias(libs.plugins.micronaut.library)
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(project(":hexagon:ports"))
    implementation(project(":hexagon:core"))

    ksp("io.micronaut:micronaut-inject-kotlin")
    ksp("io.micronaut.security:micronaut-security-annotations")

    implementation("io.micronaut:micronaut-http")
    implementation("io.micronaut.security:micronaut-security-oauth2")
    implementation("io.micronaut.security:micronaut-security-jwt")
    implementation(libs.grpc.stub)
    implementation("io.grpc:grpc-api:${libs.versions.grpc.get()}")
    implementation("io.projectreactor:reactor-core")
}

micronaut {
    version(providers.gradleProperty("micronautVersion").get())
}
