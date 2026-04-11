package com.xwal.adapter.engine

import com.xwal.adapter.engine.flowable.FlowableAdapter
import com.xwal.domain.model.*
import io.micronaut.http.client.HttpClient
import io.mockk.*
import org.junit.jupiter.api.Test
import kotlin.test.*

class FlowableAdapterTest {

    @Test
    fun `engine metadata`() {
        val client = mockk<HttpClient>(relaxed = true)
        val adapter = FlowableAdapter(client, "http://localhost:8080")
        assertEquals("Flowable", adapter.engineName)
        assertEquals(EngineType.FLOWABLE, adapter.engineType)
        assertEquals("7.0.0", adapter.engineVersion)
    }

    @Test
    fun `capabilities differ from camunda`() {
        val client = mockk<HttpClient>(relaxed = true)
        val adapter = FlowableAdapter(client, "http://localhost:8080")
        val caps = adapter.getCapabilities()
        assertTrue(caps.supportsUserTasks)
        assertTrue(caps.supportsServiceTasks)
        assertFalse(caps.supportsExternalTasks) // Key difference from Camunda7
        assertTrue(caps.supportsDMN)
        assertTrue(caps.supportsCMMN)
        assertTrue(caps.supportedScriptLanguages.contains("javascript"))
    }

    @Test
    fun `health details`() {
        val client = mockk<HttpClient>(relaxed = true)
        val adapter = FlowableAdapter(client, "http://localhost:8080")
        val details = adapter.getHealthDetails()
        assertEquals("Flowable", details["engineName"])
        assertEquals("FLOWABLE", details["engineType"])
    }
}
