package com.xwal.application.usecase.workflow

import com.xwal.application.service.AdapterResolutionService
import com.xwal.domain.exception.InstanceNotFoundException
import com.xwal.domain.model.*
import com.xwal.domain.port.output.*
import io.mockk.*
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ListGetInstanceUseCaseTest {

    private val workflowRepository = mockk<WorkflowRepository>()
    private val instanceRepository = mockk<InstanceRepository>()
    private val adapterConfigRepository = mockk<EngineAdapterConfigRepository>()
    private val adapterCache = mockk<AdapterInstanceCachePort>()
    private val adapterFactory = mockk<EngineAdapterFactoryPort>()
    private val adapter = mockk<WorkflowEnginePort>()

    private val adapterResolution = AdapterResolutionService(adapterConfigRepository, adapterCache, adapterFactory)

    private val listUseCase = ListWorkflowsUseCaseImpl(workflowRepository)
    private val getInstanceUseCase = GetInstanceUseCaseImpl(instanceRepository)
    private val getVariablesUseCase = GetInstanceVariablesUseCaseImpl(instanceRepository, adapterResolution)

    private val now = Instant.now()
    private val adapterId = EngineAdapterId.generate()
    private val instanceId = InstanceId.generate()
    private val instance = WorkflowInstance(instanceId, WorkflowId.generate(), "eng-1", adapterId, InstanceStatus.RUNNING, null, null, now, null, null, now, now)

    @Test
    fun `list returns all workflows`() {
        val wf = Workflow(WorkflowId.generate(), "test", "1.0", null, IwmDefinition("""{"workflow":{}}"""), WorkflowStatus.ACTIVE, now, null, now, null)
        every { workflowRepository.findAll() } returns listOf(wf)
        assertEquals(1, listUseCase.execute().size)
    }

    @Test
    fun `list by status`() {
        every { workflowRepository.findAllByStatus(WorkflowStatus.ACTIVE) } returns emptyList()
        assertEquals(0, listUseCase.execute(WorkflowStatus.ACTIVE).size)
    }

    @Test
    fun `get instance returns instance`() {
        every { instanceRepository.findById(instanceId) } returns instance
        assertEquals(instance, getInstanceUseCase.execute(instanceId))
    }

    @Test
    fun `get instance throws when not found`() {
        every { instanceRepository.findById(instanceId) } returns null
        assertFailsWith<InstanceNotFoundException> { getInstanceUseCase.execute(instanceId) }
    }

    @Test
    fun `get variables delegates to adapter`() {
        every { instanceRepository.findById(instanceId) } returns instance
        every { adapterCache.get(adapterId) } returns adapter
        every { adapter.getInstanceVariables("eng-1") } returns mapOf("key" to "val")
        assertEquals(mapOf("key" to "val"), getVariablesUseCase.execute(instanceId))
    }
}
