package com.xwal.adapter.engine.config

import io.micronaut.http.client.HttpClient
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Factory for HTTP clients with connection pooling.
 * Reuses clients per base URL to avoid resource leaks.
 */
@Singleton
class HttpClientFactory {

    private val log = LoggerFactory.getLogger(HttpClientFactory::class.java)
    private val clients = ConcurrentHashMap<String, HttpClient>()

    fun getOrCreateClient(baseUrl: String): HttpClient {
        return clients.computeIfAbsent(baseUrl) { url ->
            log.info("Creating pooled HTTP client for {}", url)
            HttpClient.create(URL(url))
        }
    }

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
