package com.xwal.application.usecase.workflow

import com.xwal.application.service.AdapterResolutionService
import com.xwal.domain.exception.AdapterUnavailableException
import com.xwal.domain.exception.WorkflowNotFoundException
import com.xwal.domain.model.*
import com.xwal.domain.port.input.StartWorkflowUseCase
import com.xwal.domain.port.output.*
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StartWorkflowUseCaseTest {

    private val workflowRepository = mockk<WorkflowRepository>()
    private val instanceRepository = mockk<InstanceRepository>()
    private val adapterConfigRepository = mockk<EngineAdapterConfigRepository>()
    private val adapterCache = mockk<AdapterInstanceCachePort>()
    private val adapterFactory = mockk<EngineAdapterFactoryPort>()
    private val transactionPort = mockk<TransactionPort>()
    private val adapter = mockk<WorkflowEnginePort>()

    private val adapterResolution = AdapterResolutionService(adapterConfigRepository, adapterCache, adapterFactory)
    private val useCase = StartWorkflowUseCaseImpl(workflowRepository, instanceRepository, adapterConfigRepository, adapterResolution, transactionPort)

    private val workflowId = WorkflowId.generate()
    private val adapterId = EngineAdapterId.generate()
    private val now = Instant.now()
    private val workflow = Workflow(workflowId, "test", "1.0", null, IwmDefinition("""{"workflow":{"name":"test","version":"1.0"},"tasks":[],"edges":[]}"""), WorkflowStatus.ACTIVE, now, null, now, null)
    private val adapterConfig = EngineAdapterConfig(adapterId, "camunda", EngineType.CAMUNDA7, mapOf("url" to "http://localhost:8080"), null, true, HealthStatus.HEALTHY, now, 0, now, null, now, null)

    @BeforeEach
    fun setup() {
        every { transactionPort.executeInTransaction(any<() -> WorkflowInstance>()) } answers { firstArg<() -> WorkflowInstance>().invoke() }
    }

    @Test
    fun `starts workflow successfully`() {
        every { workflowRepository.findById(workflowId) } returns workflow
        every { adapterConfigRepository.findByEnabled(true) } returns listOf(adapterConfig)
        every { adapterCache.get(adapterId) } returns adapter
        every { adapter.deployWorkflow(any(), any(), any()) } returns "deploy-1"
        every { adapter.startInstance(any(), any(), any()) } returns "instance-1"
        every { instanceRepository.save(any()) } answers { firstArg() }
        every { instanceRepository.update(any()) } answers { firstArg() }

        val result = useCase.execute(StartWorkflowUseCase.Command(workflowId, "BK-1", emptyMap(), "user"))
        assertEquals(InstanceStatus.RUNNING, result.status)
    }

    @Test
    fun `throws when workflow not found`() {
        every { workflowRepository.findById(workflowId) } returns null
        assertFailsWith<WorkflowNotFoundException> {
            useCase.execute(StartWorkflowUseCase.Command(workflowId, null, emptyMap(), null))
        }
    }

    @Test
    fun `throws when no healthy adapter`() {
        every { workflowRepository.findById(workflowId) } returns workflow
        every { adapterConfigRepository.findByEnabled(true) } returns emptyList()
        assertFailsWith<AdapterUnavailableException> {
            useCase.execute(StartWorkflowUseCase.Command(workflowId, null, emptyMap(), null))
        }
    }
}
