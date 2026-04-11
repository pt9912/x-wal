package com.xwal.application.usecase.workflow

import com.xwal.application.service.AdapterResolutionService
import com.xwal.domain.exception.InvalidStateTransitionException
import com.xwal.domain.model.*
import com.xwal.domain.port.output.*
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ResumeInstanceUseCaseTest {

    private val instanceRepository = mockk<InstanceRepository>()
    private val adapterConfigRepository = mockk<EngineAdapterConfigRepository>()
    private val adapterCache = mockk<AdapterInstanceCachePort>()
    private val adapterFactory = mockk<EngineAdapterFactoryPort>()
    private val transactionPort = mockk<TransactionPort>()
    private val adapter = mockk<WorkflowEnginePort>()

    private val adapterResolution = AdapterResolutionService(adapterConfigRepository, adapterCache, adapterFactory)
    private val useCase = ResumeInstanceUseCaseImpl(instanceRepository, adapterResolution, transactionPort)

    private val adapterId = EngineAdapterId.generate()
    private val instanceId = InstanceId.generate()
    private val now = Instant.now()
    private val suspendedInstance = WorkflowInstance(instanceId, WorkflowId.generate(), "eng-1", adapterId, InstanceStatus.SUSPENDED, null, null, now, null, null, now, now)

    @BeforeEach
    fun setup() { every { transactionPort.executeInTransaction(any<() -> WorkflowInstance>()) } answers { firstArg<() -> WorkflowInstance>().invoke() } }

    @Test
    fun `resumes suspended instance`() {
        every { instanceRepository.findById(instanceId) } returns suspendedInstance
        every { adapterCache.get(adapterId) } returns adapter
        every { adapter.resumeInstance("eng-1") } just runs
        every { instanceRepository.update(any()) } answers { firstArg() }

        assertEquals(InstanceStatus.RUNNING, useCase.execute(instanceId).status)
    }

    @Test
    fun `throws when not suspended`() {
        every { instanceRepository.findById(instanceId) } returns suspendedInstance.copy(status = InstanceStatus.RUNNING)
        assertFailsWith<InvalidStateTransitionException> { useCase.execute(instanceId) }
    }
}
