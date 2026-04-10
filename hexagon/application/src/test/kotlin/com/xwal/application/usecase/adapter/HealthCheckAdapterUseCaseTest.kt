package com.xwal.application.usecase.adapter

import com.xwal.application.service.AdapterResolutionService
import com.xwal.domain.exception.AdapterNotFoundException
import com.xwal.domain.model.*
import com.xwal.domain.port.output.*
import io.mockk.*
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HealthCheckAdapterUseCaseTest {

    private val adapterConfigRepository = mockk<EngineAdapterConfigRepository>()
    private val adapterCache = mockk<AdapterInstanceCachePort>()
    private val adapterFactory = mockk<EngineAdapterFactoryPort>()
    private val adapter = mockk<WorkflowEnginePort>()

    private val adapterResolution = AdapterResolutionService(adapterConfigRepository, adapterCache, adapterFactory)
    private val useCase = HealthCheckAdapterUseCaseImpl(adapterConfigRepository, adapterResolution)

    private val now = Instant.now()
    private val adapterId = EngineAdapterId.generate()
    private val config = EngineAdapterConfig(
        id = adapterId, name = "camunda", engineType = EngineType.CAMUNDA7,
        config = mapOf("url" to "http://localhost:8080"), capabilities = null,
        enabled = true, healthStatus = HealthStatus.UNKNOWN, lastHealthCheck = null,
        priority = 0, createdAt = now, createdBy = null, updatedAt = now, updatedBy = null
    )

    @Test
    fun `marks adapter healthy on successful check`() {
        every { adapterConfigRepository.findById(adapterId) } returns config
        every { adapterCache.get(adapterId) } returns adapter
        every { adapter.checkHealth() } returns true
        every { adapterConfigRepository.update(any()) } answers { firstArg() }

        val result = useCase.executeSingle(adapterId)
        assertEquals(HealthStatus.HEALTHY, result.healthStatus)
    }

    @Test
    fun `marks adapter unhealthy on failed check`() {
        every { adapterConfigRepository.findById(adapterId) } returns config
        every { adapterCache.get(adapterId) } returns adapter
        every { adapter.checkHealth() } returns false
        every { adapterConfigRepository.update(any()) } answers { firstArg() }

        val result = useCase.executeSingle(adapterId)
        assertEquals(HealthStatus.UNHEALTHY, result.healthStatus)
    }

    @Test
    fun `throws when adapter not found`() {
        every { adapterConfigRepository.findById(adapterId) } returns null
        assertFailsWith<AdapterNotFoundException> { useCase.executeSingle(adapterId) }
    }

    @Test
    fun `executeAll checks all enabled adapters`() {
        every { adapterConfigRepository.findByEnabled(true) } returns listOf(config)
        every { adapterCache.get(adapterId) } returns adapter
        every { adapter.checkHealth() } returns true
        every { adapterConfigRepository.update(any()) } answers { firstArg() }

        val results = useCase.executeAll()
        assertEquals(1, results.size)
        assertEquals(HealthStatus.HEALTHY, results[0].healthStatus)
    }
}
