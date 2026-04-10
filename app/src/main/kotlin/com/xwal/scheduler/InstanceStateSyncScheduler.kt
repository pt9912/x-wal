package com.xwal.scheduler

import com.xwal.domain.port.input.SyncInstanceStateUseCase
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton
class InstanceStateSyncScheduler(
    private val syncInstanceState: SyncInstanceStateUseCase
) {

    private val log = LoggerFactory.getLogger(InstanceStateSyncScheduler::class.java)

    @Scheduled(fixedDelay = "\${xwal.instance-sync.scheduler.fixed-delay:30s}",
               initialDelay = "\${xwal.instance-sync.scheduler.initial-delay:10s}")
    fun syncInstanceStates() {
        log.debug("Starting instance state sync")
        try {
            val result = syncInstanceState.execute()
            if (result.synced > 0 || result.failed > 0) {
                log.info("Instance sync: synced={}, failed={}, skipped={}", result.synced, result.failed, result.skipped)
            }
        } catch (e: Exception) {
            log.error("Instance sync failed: {}", e.message, e)
        }
    }
}
