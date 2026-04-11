package com.xwal.application.service

import com.xwal.domain.model.EngineAdapterConfig
import com.xwal.domain.model.EngineAdapterId
import com.xwal.domain.model.EngineType
import com.xwal.domain.model.HealthStatus
import com.xwal.domain.port.output.AdapterInstanceCachePort
import com.xwal.domain.port.output.EngineAdapterConfigRepository
import com.xwal.domain.port.output.EngineAdapterFactoryPort
import com.xwal.domain.port.output.WorkflowEnginePort
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertSame

class AdapterResolutionServiceTest {
    private val adapterConfigRepository = mockk<EngineAdapterConfigRepository>()
    private val adapterCache = mockk<AdapterInstanceCachePort>()
    private val adapterFactory = mockk<EngineAdapterFactoryPort>()

    private val resolutionService = AdapterResolutionService(adapterConfigRepository, adapterCache, adapterFactory)
    private val now = Instant.now()

    @Test
    fun `resolveByEngineType is deterministic when priority is equal`() {
        val adapterA = EngineAdapterId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
        val adapterB = EngineAdapterId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
        val portA = mockk<WorkflowEnginePort>()
        val portB = mockk<WorkflowEnginePort>()

        val configA = createConfig(adapterA, "first-listed")
        val configB = createConfig(adapterB, "second-listed")

        every { adapterConfigRepository.findByEngineType(EngineType.CAMUNDA7) } returns listOf(configA, configB)
        every { adapterCache.get(adapterA) } returns null
        every { adapterCache.put(any(), any()) } just Runs
        every { adapterFactory.createAdapter(EngineType.CAMUNDA7, configA.config) } returns portA

        val selected = resolutionService.resolveByEngineType(EngineType.CAMUNDA7)

        assertSame(portA, selected)
    }

    private fun createConfig(id: EngineAdapterId, name: String): EngineAdapterConfig = EngineAdapterConfig(
        id = id,
        name = name,
        engineType = EngineType.CAMUNDA7,
        config = mapOf("url" to "http://localhost"),
        capabilities = null,
        enabled = true,
        healthStatus = HealthStatus.HEALTHY,
        lastHealthCheck = now,
        priority = 0,
        createdAt = now,
        createdBy = null,
        updatedAt = now,
        updatedBy = null
    )
}
