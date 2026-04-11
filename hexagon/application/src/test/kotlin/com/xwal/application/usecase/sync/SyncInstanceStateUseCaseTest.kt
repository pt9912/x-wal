package com.xwal.application.usecase.sync

import com.xwal.application.service.AdapterResolutionService
import com.xwal.domain.exception.AdapterOperationException
import com.xwal.domain.model.EngineInstanceStatus
import com.xwal.domain.model.EngineAdapterId
import com.xwal.domain.model.InstanceId
import com.xwal.domain.model.InstanceStatus
import com.xwal.domain.model.WorkflowId
import com.xwal.domain.model.WorkflowInstance
import com.xwal.domain.port.output.DistributedLockPort
import com.xwal.domain.port.output.InstanceRepository
import com.xwal.domain.port.output.WorkflowEnginePort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals

class SyncInstanceStateUseCaseTest {

    private val now = Instant.now()
    private val instanceRepository = mockk<InstanceRepository>(relaxed = true)
    private val adapterResolution = mockk<AdapterResolutionService>()
    private val distributedLock = CapturingDistributedLock()

    private lateinit var options: SyncInstanceStateOptions
    private lateinit var useCase: SyncInstanceStateUseCaseImpl

    private val adapterId = EngineAdapterId.generate()
    private val runningAdapterInstanceId = "eng-running"

    private val runningInstance = WorkflowInstance(
        id = InstanceId.generate(),
        workflowId = WorkflowId.generate(),
        engineInstanceId = runningAdapterInstanceId,
        engineAdapterId = adapterId,
        status = InstanceStatus.RUNNING,
        businessKey = null,
        variables = null,
        startedAt = now,
        startedBy = null,
        endedAt = null,
        createdAt = now,
        updatedAt = now
    )

    private val suspendedInstance = runningInstance.copy(status = InstanceStatus.SUSPENDED)
    private val adapter = mockk<WorkflowEnginePort>()

    @BeforeEach
    fun setUp() {
        options = SyncInstanceStateOptions(
            batchSize = 5,
            timeout = Duration.ofMillis(100),
            includeSuspended = false,
            retryEnabled = false,
            retryMaxAttempts = 1,
            retryBackoff = Duration.ofMillis(1)
        )
        useCase = SyncInstanceStateUseCaseImpl(instanceRepository, adapterResolution, distributedLock, options)
    }

    @Test
    fun `wraps sync run in distributed lock`() {
        every { instanceRepository.findForSync(any(), any(), any()) } returns emptyList()

        val result = useCase.execute()

        assertEquals(0, result.synced)
        assertEquals(0, result.failed)
        assertEquals(0, result.skipped)
        assertEquals(1, distributedLock.callCount)
        assertEquals("instance-sync", distributedLock.key)
        assertEquals(options.timeout, distributedLock.timeout)
    }

    @Test
    fun `skips suspended instances when includeSuspended is false`() {
        every { instanceRepository.findForSync(any(), any(), any()) } returnsMany listOf(
            listOf(suspendedInstance),
            emptyList()
        )

        val result = useCase.execute()

        assertEquals(0, result.synced)
        assertEquals(0, result.failed)
        assertEquals(1, result.skipped)
        verify(exactly = 0) { adapterResolution.resolveById(any()) }
        verify(exactly = 0) { adapter.getInstanceStatus(any()) }
    }

    @Test
    fun `syncs suspended instances when includeSuspended is true`() {
        options = options.copy(includeSuspended = true)
        useCase = SyncInstanceStateUseCaseImpl(instanceRepository, adapterResolution, distributedLock, options)

        every { adapterResolution.resolveById(adapterId) } returns adapter
        every { adapter.getInstanceStatus(runningAdapterInstanceId) } returns EngineInstanceStatus(
            instanceId = runningAdapterInstanceId,
            workflowKey = null,
            businessKey = null,
            status = EngineInstanceStatus.Status.RUNNING,
            startedAt = now,
            endedAt = null,
            variables = emptyMap(),
            currentActivityId = null,
            engineInstanceId = runningAdapterInstanceId
        )
        every { instanceRepository.update(any()) } answers { firstArg() }
        every { instanceRepository.findForSync(any(), any(), any()) } returnsMany listOf(
            listOf(suspendedInstance),
            emptyList()
        )

        val result = useCase.execute()

        assertEquals(1, result.synced)
        assertEquals(0, result.failed)
        assertEquals(0, result.skipped)
        verify(exactly = 1) { adapter.getInstanceStatus(runningAdapterInstanceId) }
        verify(exactly = 1) { instanceRepository.update(any()) }
    }

    @Test
    fun `retries engine status lookup and continues on transient failure`() {
        options = options.copy(
            includeSuspended = true,
            retryEnabled = true,
            retryMaxAttempts = 2,
            retryBackoff = Duration.ofMillis(1)
        )
        useCase = SyncInstanceStateUseCaseImpl(instanceRepository, adapterResolution, distributedLock, options)

        var attempt = 0

        every { adapterResolution.resolveById(adapterId) } returns adapter
        every { adapter.getInstanceStatus(runningAdapterInstanceId) } answers {
            attempt++
            if (attempt == 1) {
                throw AdapterOperationException("flowable", "getInstanceStatus", "temporary")
            }
            EngineInstanceStatus(
                instanceId = runningAdapterInstanceId,
                workflowKey = null,
                businessKey = null,
                status = EngineInstanceStatus.Status.COMPLETED,
                startedAt = now,
                endedAt = now,
                variables = emptyMap(),
                currentActivityId = null,
                engineInstanceId = runningAdapterInstanceId
            )
        }
        every { instanceRepository.update(any()) } answers { firstArg() }
        every { instanceRepository.findForSync(any(), any(), any()) } returnsMany listOf(
            listOf(runningInstance),
            emptyList()
        )

        val result = useCase.execute()

        assertEquals(1, result.synced)
        assertEquals(0, result.failed)
        assertEquals(0, result.skipped)
        verify(exactly = 2) { adapter.getInstanceStatus(runningAdapterInstanceId) }
        verify(exactly = 1) { instanceRepository.update(any()) }
    }

    @Test
    fun `does not retry on non-retryable exception`() {
        options = options.copy(includeSuspended = true, retryEnabled = true)
        useCase = SyncInstanceStateUseCaseImpl(instanceRepository, adapterResolution, distributedLock, options)

        every { adapterResolution.resolveById(adapterId) } returns adapter
        every { adapter.getInstanceStatus(runningAdapterInstanceId) } throws IllegalArgumentException("permanent")
        every { instanceRepository.findForSync(any(), any(), any()) } returnsMany listOf(
            listOf(runningInstance),
            emptyList()
        )

        val result = useCase.execute()

        assertEquals(0, result.synced)
        assertEquals(1, result.failed)
        assertEquals(0, result.skipped)
        verify(exactly = 1) { adapter.getInstanceStatus(runningAdapterInstanceId) }
    }

    @Test
    fun `does not retry when retry is disabled`() {
        options = options.copy(includeSuspended = true, retryEnabled = false)
        useCase = SyncInstanceStateUseCaseImpl(instanceRepository, adapterResolution, distributedLock, options)

        every { adapterResolution.resolveById(adapterId) } returns adapter
        every { adapter.getInstanceStatus(runningAdapterInstanceId) } throws RuntimeException("permanent")
        every { instanceRepository.findForSync(any(), any(), any()) } returnsMany listOf(
            listOf(runningInstance),
            emptyList()
        )

        val result = useCase.execute()

        assertEquals(0, result.synced)
        assertEquals(1, result.failed)
        assertEquals(0, result.skipped)
        verify(exactly = 1) { adapter.getInstanceStatus(runningAdapterInstanceId) }
    }

    private class CapturingDistributedLock : DistributedLockPort {
        var key: String? = null
        var timeout: Duration? = null
        var callCount: Int = 0

        override fun <T> withLock(lockKey: String, timeout: Duration, block: () -> T): T {
            this.key = lockKey
            this.timeout = timeout
            callCount++
            return block()
        }

        override fun tryAcquire(lockKey: String, timeout: Duration): Boolean = true
        override fun release(lockKey: String) {}
    }
}
