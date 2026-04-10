package com.xwal.domain.service

import com.xwal.domain.model.*
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EngineRoutingLogicTest {

    private val now = Instant.now()

    private fun adapter(
        type: EngineType,
        healthy: Boolean = true,
        enabled: Boolean = true,
        priority: Int = 0
    ) = EngineAdapterConfig(
        id = EngineAdapterId.generate(),
        name = "${type.name}-adapter",
        engineType = type,
        config = mapOf("url" to "http://localhost:8080"),
        capabilities = null,
        enabled = enabled,
        healthStatus = if (healthy) HealthStatus.HEALTHY else HealthStatus.UNHEALTHY,
        lastHealthCheck = now,
        priority = priority,
        createdAt = now,
        createdBy = null,
        updatedAt = now,
        updatedBy = null
    )

    @Test
    fun `selects matching engine type when available`() {
        val adapters = listOf(adapter(EngineType.CAMUNDA7), adapter(EngineType.FLOWABLE))
        val result = EngineRoutingLogic.selectAdapter(adapters, EngineType.FLOWABLE)
        assertNotNull(result)
        assertEquals(EngineType.FLOWABLE, result.engineType)
    }

    @Test
    fun `falls back to highest priority when target type not available`() {
        val adapters = listOf(
            adapter(EngineType.CAMUNDA7, priority = 10),
            adapter(EngineType.FLOWABLE, priority = 1)
        )
        val result = EngineRoutingLogic.selectAdapter(adapters, EngineType.ACTIVITI)
        assertNotNull(result)
        assertEquals(EngineType.FLOWABLE, result.engineType)
    }

    @Test
    fun `returns null when no healthy adapters`() {
        val adapters = listOf(adapter(EngineType.CAMUNDA7, healthy = false))
        assertNull(EngineRoutingLogic.selectAdapter(adapters))
    }

    @Test
    fun `skips disabled adapters`() {
        val adapters = listOf(adapter(EngineType.CAMUNDA7, enabled = false))
        assertNull(EngineRoutingLogic.selectAdapter(adapters))
    }

    @Test
    fun `extractTargetEngine finds camunda7`() {
        val json = """{"workflow":{"name":"test"},"extensions":{"targetEngine":"camunda7"}}"""
        assertEquals(EngineType.CAMUNDA7, EngineRoutingLogic.extractTargetEngine(json))
    }

    @Test
    fun `extractTargetEngine returns null for unknown`() {
        assertNull(EngineRoutingLogic.extractTargetEngine("""{"workflow":{}}"""))
    }
}
