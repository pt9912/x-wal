package com.xwal.application.usecase.workflow

import com.xwal.application.service.AdapterResolutionService
import com.xwal.domain.exception.InstanceNotFoundException
import com.xwal.domain.exception.InvalidStateTransitionException
import com.xwal.domain.model.*
import com.xwal.domain.port.output.*
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SuspendInstanceUseCaseTest {

    private val instanceRepository = mockk<InstanceRepository>()
    private val adapterConfigRepository = mockk<EngineAdapterConfigRepository>()
    private val adapterCache = mockk<AdapterInstanceCachePort>()
    private val adapterFactory = mockk<EngineAdapterFactoryPort>()
    private val transactionPort = mockk<TransactionPort>()
    private val adapter = mockk<WorkflowEnginePort>()

    private val adapterResolution = AdapterResolutionService(adapterConfigRepository, adapterCache, adapterFactory)
    private val useCase = SuspendInstanceUseCaseImpl(instanceRepository, adapterResolution, transactionPort)

    private val adapterId = EngineAdapterId.generate()
    private val instanceId = InstanceId.generate()
    private val now = Instant.now()

    private val runningInstance = WorkflowInstance(
        id = instanceId, workflowId = WorkflowId.generate(),
        engineInstanceId = "eng-1", engineAdapterId = adapterId,
        status = InstanceStatus.RUNNING, businessKey = null, variables = null,
        startedAt = now, startedBy = null, endedAt = null,
        createdAt = now, updatedAt = now
    )

    @BeforeEach
    fun setup() {
        every { transactionPort.executeInTransaction(any<() -> WorkflowInstance>()) } answers {
            firstArg<() -> WorkflowInstance>().invoke()
        }
    }

    @Test
    fun `suspends running instance`() {
        every { instanceRepository.findById(instanceId) } returns runningInstance
        every { adapterCache.get(adapterId) } returns adapter
        every { adapter.suspendInstance("eng-1") } just runs
        every { instanceRepository.update(any()) } answers { firstArg() }

        val result = useCase.execute(instanceId)
        assertEquals(InstanceStatus.SUSPENDED, result.status)
        verify { adapter.suspendInstance("eng-1") }
    }

    @Test
    fun `throws when instance not found`() {
        every { instanceRepository.findById(instanceId) } returns null
        assertFailsWith<InstanceNotFoundException> { useCase.execute(instanceId) }
    }

    @Test
    fun `throws when instance not running`() {
        val completed = runningInstance.copy(status = InstanceStatus.COMPLETED)
        every { instanceRepository.findById(instanceId) } returns completed
        assertFailsWith<InvalidStateTransitionException> { useCase.execute(instanceId) }
    }
}
