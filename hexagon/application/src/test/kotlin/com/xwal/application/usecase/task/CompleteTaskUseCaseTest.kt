package com.xwal.application.usecase.task

import com.xwal.application.service.AdapterResolutionService
import com.xwal.domain.exception.AdapterNotFoundException
import com.xwal.domain.model.*
import com.xwal.domain.port.input.CompleteTaskUseCase.Command
import com.xwal.domain.port.output.*
import io.mockk.*
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertFailsWith

class CompleteTaskUseCaseTest {

    private val adapterConfigRepository = mockk<EngineAdapterConfigRepository>()
    private val adapterCache = mockk<AdapterInstanceCachePort>()
    private val adapterFactory = mockk<EngineAdapterFactoryPort>()
    private val adapter = mockk<WorkflowEnginePort>()

    private val adapterResolution = AdapterResolutionService(adapterConfigRepository, adapterCache, adapterFactory)
    private val useCase = CompleteTaskUseCaseImpl(adapterResolution)

    private val now = Instant.now()
    private val adapterId = EngineAdapterId.generate()
    private val adapterConfig = EngineAdapterConfig(
        id = adapterId, name = "camunda", engineType = EngineType.CAMUNDA7,
        config = mapOf("url" to "http://localhost:8080"), capabilities = null,
        enabled = true, healthStatus = HealthStatus.HEALTHY, lastHealthCheck = now,
        priority = 0, createdAt = now, createdBy = null, updatedAt = now, updatedBy = null
    )

    @Test
    fun `completes task via adapter`() {
        every { adapterConfigRepository.findByEngineType(EngineType.CAMUNDA7) } returns listOf(adapterConfig)
        every { adapterCache.get(adapterId) } returns adapter
        every { adapter.completeTask("task-1", any()) } just runs

        val taskId = TaskId.of(EngineType.CAMUNDA7, "task-1")
        useCase.execute(Command(taskId, mapOf("approved" to true)))

        verify { adapter.completeTask("task-1", mapOf("approved" to true)) }
    }

    @Test
    fun `throws when no adapter for engine type`() {
        every { adapterConfigRepository.findByEngineType(EngineType.TEMPORAL) } returns emptyList()

        val taskId = TaskId.of(EngineType.TEMPORAL, "task-1")
        assertFailsWith<AdapterNotFoundException> {
            useCase.execute(Command(taskId))
        }
    }
}
