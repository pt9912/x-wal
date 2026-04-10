package com.xwal

import io.micronaut.runtime.Micronaut
import org.slf4j.LoggerFactory

object Application {
    private val log = LoggerFactory.getLogger(Application::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
        log.info("Starting x-wal v2.0.0 (Kotlin/Hexagonal)")
        Micronaut.run(Application::class.java, *args)
    }
}
