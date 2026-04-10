// Standalone CLI tool — no Micronaut application context needed.
// Accepted hexagonal exception: directly depends on engine adapter transformers.
dependencies {
    implementation(project(":hexagon:ports"))
    implementation(project(":hexagon:core"))
    implementation(project(":adapters:driven:engine"))

    implementation(libs.picocli)
}
