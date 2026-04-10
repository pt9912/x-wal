plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.micronaut.application) apply false
    alias(libs.plugins.micronaut.library) apply false
    alias(libs.plugins.micronaut.test.resources) apply false
}

group = "com.xwal"
version = "2.0.0-SNAPSHOT"

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories { mavenCentral() }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    dependencies {
        val libs = rootProject.libs
        "implementation"(libs.slf4j.api)
        "testImplementation"(kotlin("test"))
        "testImplementation"(libs.junit.api)
        "testRuntimeOnly"(libs.junit.engine)
        "testImplementation"(libs.mockk)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
