package com.xwal.scheduler

import com.xwal.config.InstanceSyncConfig
import com.xwal.domain.port.input.SyncInstanceStateUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class SchedulerTest {

    private val syncUseCase = mockk<SyncInstanceStateUseCase>()

    @Test
    fun `scheduler runs sync when enabled`() {
        val config = InstanceSyncConfig().apply { enabled = true }
        val scheduler = InstanceStateSyncScheduler(syncUseCase, config)
        every { syncUseCase.execute() } returns SyncInstanceStateUseCase.SyncResult(2, 0, 5)
        scheduler.syncInstanceStates()
        verify { syncUseCase.execute() }
    }

    @Test
    fun `scheduler skips when disabled`() {
        val config = InstanceSyncConfig().apply { enabled = false }
        val scheduler = InstanceStateSyncScheduler(syncUseCase, config)
        scheduler.syncInstanceStates()
        verify(exactly = 0) { syncUseCase.execute() }
    }

    @Test
    fun `scheduler handles exception`() {
        val config = InstanceSyncConfig().apply { enabled = true }
        val scheduler = InstanceStateSyncScheduler(syncUseCase, config)
        every { syncUseCase.execute() } throws RuntimeException("DB down")
        scheduler.syncInstanceStates() // should not throw
    }

    @Test
    fun `scheduler with zero changes`() {
        val config = InstanceSyncConfig().apply { enabled = true }
        val scheduler = InstanceStateSyncScheduler(syncUseCase, config)
        every { syncUseCase.execute() } returns SyncInstanceStateUseCase.SyncResult(0, 0, 10)
        scheduler.syncInstanceStates()
        verify { syncUseCase.execute() }
    }
}
