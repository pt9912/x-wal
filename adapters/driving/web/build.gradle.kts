plugins {
    alias(libs.plugins.micronaut.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.protobuf)
}

dependencies {
    implementation(project(":hexagon:ports"))
    // hexagon:core types are available transitively via hexagon:ports (api dependency)

    ksp("io.micronaut:micronaut-inject-kotlin")
    ksp("io.micronaut.serde:micronaut-serde-processor")
    ksp("io.micronaut.security:micronaut-security-annotations")
    ksp("io.micronaut.openapi:micronaut-openapi")

    implementation("io.micronaut:micronaut-http-server-netty")
    implementation("io.projectreactor:reactor-core")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("io.micronaut.validation:micronaut-validation")
    implementation("io.micronaut.security:micronaut-security-oauth2")
    implementation("io.micronaut.security:micronaut-security-jwt")
    implementation("io.micronaut.grpc:micronaut-grpc-server-runtime")
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.netty)
    implementation(libs.swagger.annotations)
    implementation("javax.annotation:javax.annotation-api:1.3.2")
}

micronaut {
    version(providers.gradleProperty("micronautVersion").get())
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc")
            }
        }
    }
}
