package com.xwal.domain.service

import com.xwal.domain.model.*
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EngineRoutingLogicExtendedTest {

    private val now = Instant.now()
    private fun adapter(type: EngineType, healthy: Boolean = true, enabled: Boolean = true, priority: Int = 0) =
        EngineAdapterConfig(EngineAdapterId.generate(), "${type.name}-adapter", type, mapOf("url" to "http://localhost"), null,
            enabled, if (healthy) HealthStatus.HEALTHY else HealthStatus.UNHEALTHY, now, priority, now, null, now, null)

    @Test fun `prefers target engine type`() {
        val adapters = listOf(adapter(EngineType.CAMUNDA7), adapter(EngineType.FLOWABLE))
        assertEquals(EngineType.FLOWABLE, EngineRoutingLogic.selectAdapter(adapters, EngineType.FLOWABLE)?.engineType)
    }

    @Test fun `prefers lower priority number`() {
        val adapters = listOf(adapter(EngineType.CAMUNDA7, priority = 10), adapter(EngineType.CAMUNDA7, priority = 1))
        assertEquals(1, EngineRoutingLogic.selectAdapter(adapters)?.priority)
    }

    @Test fun `skips unhealthy`() {
        assertNull(EngineRoutingLogic.selectAdapter(listOf(adapter(EngineType.CAMUNDA7, healthy = false))))
    }

    @Test fun `skips disabled`() {
        assertNull(EngineRoutingLogic.selectAdapter(listOf(adapter(EngineType.CAMUNDA7, enabled = false))))
    }

    @Test fun `empty list returns null`() {
        assertNull(EngineRoutingLogic.selectAdapter(emptyList()))
    }

    @Test fun `fallback when target not available`() {
        val adapters = listOf(adapter(EngineType.CAMUNDA7))
        assertNotNull(EngineRoutingLogic.selectAdapter(adapters, EngineType.TEMPORAL))
    }

    @Test fun `extractTargetEngine from extensions`() {
        assertEquals(EngineType.CAMUNDA7, EngineRoutingLogic.extractTargetEngine("""{"extensions":{"targetEngine":"camunda7"}}"""))
    }

    @Test fun `extractTargetEngine from workflow`() {
        assertEquals(EngineType.FLOWABLE, EngineRoutingLogic.extractTargetEngine("""{"workflow":{"targetEngine":"flowable"}}"""))
    }

    @Test fun `extractTargetEngine case insensitive`() {
        assertEquals(EngineType.CAMUNDA7, EngineRoutingLogic.extractTargetEngine("""{"extensions":{"targetEngine":"CAMUNDA7"}}"""))
    }

    @Test fun `extractTargetEngine returns null for unknown`() {
        assertNull(EngineRoutingLogic.extractTargetEngine("""{"extensions":{"targetEngine":"unknown"}}"""))
    }

    @Test fun `extractTargetEngine returns null for no hint`() {
        assertNull(EngineRoutingLogic.extractTargetEngine("""{"workflow":{"name":"test"}}"""))
    }

    @Test fun `extractTargetEngine handles invalid JSON`() {
        assertNull(EngineRoutingLogic.extractTargetEngine("not json"))
    }
}
