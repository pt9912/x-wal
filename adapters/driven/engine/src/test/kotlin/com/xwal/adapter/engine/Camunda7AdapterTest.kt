package com.xwal.adapter.engine

import com.xwal.adapter.engine.camunda7.Camunda7Adapter
import com.xwal.domain.model.*
import io.micronaut.http.client.HttpClient
import io.mockk.*
import org.junit.jupiter.api.Test
import kotlin.test.*

class Camunda7AdapterTest {

    @Test
    fun `engine metadata`() {
        val client = mockk<HttpClient>(relaxed = true)
        val adapter = Camunda7Adapter(client, "http://localhost:8080")
        assertEquals("Camunda 7", adapter.engineName)
        assertEquals(EngineType.CAMUNDA7, adapter.engineType)
        assertNotNull(adapter.engineVersion)
    }

    @Test
    fun `capabilities include BPMN features`() {
        val client = mockk<HttpClient>(relaxed = true)
        val adapter = Camunda7Adapter(client, "http://localhost:8080")
        val caps = adapter.getCapabilities()
        assertTrue(caps.supportsUserTasks)
        assertTrue(caps.supportsServiceTasks)
        assertTrue(caps.supportsExternalTasks)
        assertTrue(caps.supportsDMN)
        assertTrue(caps.supportsScriptTasks)
        assertTrue(caps.supportsMultiInstance)
        assertTrue(caps.supportedScriptLanguages.contains("groovy"))
    }

    @Test
    fun `health details returns map`() {
        val client = mockk<HttpClient>(relaxed = true)
        val adapter = Camunda7Adapter(client, "http://localhost:8080")
        val details = adapter.getHealthDetails()
        assertEquals("Camunda 7", details["engineName"])
        assertEquals("CAMUNDA7", details["engineType"])
        assertNotNull(details["timestamp"])
    }
}
