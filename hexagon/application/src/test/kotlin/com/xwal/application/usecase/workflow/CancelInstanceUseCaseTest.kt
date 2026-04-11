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
import kotlin.test.assertNotNull

class CancelInstanceUseCaseTest {

    private val instanceRepository = mockk<InstanceRepository>()
    private val adapterConfigRepository = mockk<EngineAdapterConfigRepository>()
    private val adapterCache = mockk<AdapterInstanceCachePort>()
    private val adapterFactory = mockk<EngineAdapterFactoryPort>()
    private val transactionPort = mockk<TransactionPort>()
    private val adapter = mockk<WorkflowEnginePort>()

    private val adapterResolution = AdapterResolutionService(adapterConfigRepository, adapterCache, adapterFactory)
    private val useCase = CancelInstanceUseCaseImpl(instanceRepository, adapterResolution, transactionPort)

    private val adapterId = EngineAdapterId.generate()
    private val instanceId = InstanceId.generate()
    private val now = Instant.now()
    private val runningInstance = WorkflowInstance(instanceId, WorkflowId.generate(), "eng-1", adapterId, InstanceStatus.RUNNING, null, null, now, null, null, now, now)

    @BeforeEach
    fun setup() {
        every { transactionPort.executeInTransaction(any<() -> WorkflowInstance>()) } answers { firstArg<() -> WorkflowInstance>().invoke() }
    }

    @Test
    fun `cancels running instance`() {
        every { instanceRepository.findById(instanceId) } returns runningInstance
        every { adapterCache.get(adapterId) } returns adapter
        every { adapter.cancelInstance("eng-1", any()) } just runs
        every { instanceRepository.update(any()) } answers { firstArg() }

        val result = useCase.execute(instanceId, "test reason")
        assertEquals(InstanceStatus.CANCELLED, result.status)
        assertNotNull(result.endedAt)
    }

    @Test
    fun `throws when already completed`() {
        every { instanceRepository.findById(instanceId) } returns runningInstance.copy(status = InstanceStatus.COMPLETED)
        assertFailsWith<InvalidStateTransitionException> { useCase.execute(instanceId) }
    }

    @Test
    fun `throws when not found`() {
        every { instanceRepository.findById(instanceId) } returns null
        assertFailsWith<InstanceNotFoundException> { useCase.execute(instanceId) }
    }
}
