plugins {
    alias(libs.plugins.micronaut.library)
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(project(":hexagon:ports"))
    implementation(project(":hexagon:core"))
    implementation(project(":adapters:driven:engine"))

    ksp("io.micronaut:micronaut-inject-kotlin")

    implementation(libs.picocli)
    ksp(libs.picocli.codegen)
}

micronaut {
    version(providers.gradleProperty("micronautVersion").get())
}
