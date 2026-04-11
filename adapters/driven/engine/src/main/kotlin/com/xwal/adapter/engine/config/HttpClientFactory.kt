package com.xwal.adapter.engine.config

import io.micronaut.http.client.HttpClient
import jakarta.annotation.PreDestroy
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/**
 * Factory for HTTP clients with connection pooling.
 * Reuses clients per base URL to avoid resource leaks.
 * Implements @PreDestroy for graceful shutdown.
 */
@Singleton
class HttpClientFactory {

    private val log = LoggerFactory.getLogger(HttpClientFactory::class.java)
    private val clients = ConcurrentHashMap<String, HttpClient>()

    fun getOrCreateClient(baseUrl: String): HttpClient {
        return clients.computeIfAbsent(baseUrl) { url ->
            log.info("Creating pooled HTTP client for {}", url)
            HttpClient.create(URI.create(url).toURL())
        }
    }

    @PreDestroy
    fun closeAll() {
        clients.values.forEach { client ->
            try {
                client.close()
            } catch (e: Exception) {
                log.warn("Error closing HTTP client: {}", e.message)
            }
        }
        clients.clear()
    }
}
