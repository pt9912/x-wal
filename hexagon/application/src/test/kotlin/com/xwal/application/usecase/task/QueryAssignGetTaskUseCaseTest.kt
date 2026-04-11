package com.xwal.application.usecase.task

import com.xwal.application.service.AdapterResolutionService
import com.xwal.domain.model.*
import com.xwal.domain.port.output.*
import io.mockk.*
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals

class QueryAssignGetTaskUseCaseTest {

    private val adapterConfigRepository = mockk<EngineAdapterConfigRepository>()
    private val adapterCache = mockk<AdapterInstanceCachePort>()
    private val adapterFactory = mockk<EngineAdapterFactoryPort>()
    private val adapter = mockk<WorkflowEnginePort>()

    private val adapterResolution = AdapterResolutionService(adapterConfigRepository, adapterCache, adapterFactory)

    private val queryUseCase = QueryTasksUseCaseImpl(adapterConfigRepository, adapterResolution)
    private val assignUseCase = AssignTaskUseCaseImpl(adapterResolution)
    private val getTaskUseCase = GetTaskUseCaseImpl(adapterResolution)

    private val now = Instant.now()
    private val adapterId = EngineAdapterId.generate()
    private val config = EngineAdapterConfig(adapterId, "camunda", EngineType.CAMUNDA7, mapOf("url" to "http://localhost"), null, true, HealthStatus.HEALTHY, now, 0, now, null, now, null)
    private val task = Task(TaskId.of(EngineType.CAMUNDA7, "t1"), "Task 1", "userTask", null, null, null, null, null, TaskStatus.CREATED, now, null, null)

    @Test
    fun `query returns tasks from healthy adapters`() {
        every { adapterConfigRepository.findByHealthStatus(HealthStatus.HEALTHY) } returns listOf(config)
        every { adapterCache.get(adapterId) } returns adapter
        every { adapter.queryTasks(any()) } returns listOf(task)
        val result = queryUseCase.execute(TaskFilter())
        assertEquals(1, result.size)
    }

    @Test
    fun `query returns empty when no healthy adapters`() {
        every { adapterConfigRepository.findByHealthStatus(HealthStatus.HEALTHY) } returns emptyList()
        assertEquals(0, queryUseCase.execute(TaskFilter()).size)
    }

    @Test
    fun `assign delegates to adapter`() {
        every { adapterConfigRepository.findByEngineType(EngineType.CAMUNDA7) } returns listOf(config)
        every { adapterCache.get(adapterId) } returns adapter
        every { adapter.assignTask("t1", "alice") } just runs
        assignUseCase.execute(TaskId.of(EngineType.CAMUNDA7, "t1"), "alice")
        verify { adapter.assignTask("t1", "alice") }
    }

    @Test
    fun `get task delegates to adapter`() {
        every { adapterConfigRepository.findByEngineType(EngineType.CAMUNDA7) } returns listOf(config)
        every { adapterCache.get(adapterId) } returns adapter
        every { adapter.getTask("t1") } returns task
        val result = getTaskUseCase.execute(TaskId.of(EngineType.CAMUNDA7, "t1"))
        assertEquals("Task 1", result.name)
    }
}
