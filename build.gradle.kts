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
    apply(plugin = "jacoco")

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
        finalizedBy(tasks.withType<JacocoReport>())
    }

    tasks.withType<JacocoReport> {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    tasks.withType<JacocoCoverageVerification> {
        val minCoverage = when (project.path) {
            ":app" -> "0.60"                     // Wiring/Factories — lower threshold
            ":adapters:driving:web" -> "0.0"     // Controllers tested via integration
            ":adapters:driving:cli" -> "0.0"     // Standalone CLI
            ":adapters:driven:identity" -> "0.0" // Security config — tested at runtime
            ":adapters:driven:observability" -> "0.0" // Config beans
            else -> "0.80"                       // Hexagon + driven adapters
        }
        violationRules {
            rule {
                limit {
                    minimum = minCoverage.toBigDecimal()
                }
            }
        }
    }

    tasks.named("check") {
        dependsOn(tasks.withType<JacocoCoverageVerification>())
    }
}
