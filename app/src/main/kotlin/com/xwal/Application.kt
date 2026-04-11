package com.xwal

import io.micronaut.runtime.Micronaut
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.info.License
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.servers.Server
import org.slf4j.LoggerFactory

@OpenAPIDefinition(
    info = Info(
        title = "x-wal API",
        version = "2.0.0",
        description = "Cross-Workflow Abstraction Layer — unified API for BPMN workflow engines",
        license = License(name = "MIT")
    ),
    servers = [
        Server(url = "http://localhost:8080", description = "Development"),
        Server(url = "https://api.xwal.example.com", description = "Production")
    ],
    security = [SecurityRequirement(name = "bearer")]
)
object Application {
    private val log = LoggerFactory.getLogger(Application::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        log.info("Starting x-wal v2.0.0 (Kotlin/Hexagonal)")
        Micronaut.run(Application::class.java, *args)
    }
}
