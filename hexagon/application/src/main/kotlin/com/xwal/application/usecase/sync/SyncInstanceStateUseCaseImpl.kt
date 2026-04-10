package com.xwal.application.usecase.sync

import com.xwal.application.service.AdapterResolutionService
import com.xwal.domain.model.EngineInstanceStatus
import com.xwal.domain.model.InstanceId
import com.xwal.domain.model.InstanceStatus
import com.xwal.domain.port.input.SyncInstanceStateUseCase
import com.xwal.domain.port.input.SyncInstanceStateUseCase.SyncResult
import com.xwal.domain.port.output.InstanceRepository
import org.slf4j.LoggerFactory
import java.time.Instant

class SyncInstanceStateUseCaseImpl(
    private val instanceRepository: InstanceRepository,
    private val adapterResolution: AdapterResolutionService,
    private val batchSize: Int = 100
) : SyncInstanceStateUseCase {

    private val log = LoggerFactory.getLogger(SyncInstanceStateUseCaseImpl::class.java)

    override fun execute(): SyncResult {
        var synced = 0
        var failed = 0
        var skipped = 0
        var lastId: InstanceId? = null
        val cutoff = Instant.now().minusSeconds(30) // Compute once before loop (L-4 fix)

        while (true) {
            val batch = instanceRepository.findForSync(
                updatedBefore = cutoff,
                lastId = lastId,
                limit = batchSize
            )
            if (batch.isEmpty()) break

            for (instance in batch) {
                lastId = instance.id

                if (instance.status !in SYNCABLE_STATUSES) {
                    skipped++
                    continue
                }

                try {
                    val engineInstanceId = instance.engineInstanceId ?: run {
                        log.warn("Instance {} has no engineInstanceId, skipping sync", instance.id)
                        skipped++
                        continue
                    }
                    val adapterId = instance.engineAdapterId ?: run {
                        log.warn("Instance {} has no engineAdapterId, skipping sync", instance.id)
                        skipped++
                        continue
                    }

                    val adapter = adapterResolution.resolveById(adapterId)
                    val engineStatus = adapter.getInstanceStatus(engineInstanceId)
                    val resolvedStatus = mapEngineStatus(engineStatus.status)

                    if (resolvedStatus != instance.status) {
                        val updated = instance.copy(
                            status = resolvedStatus,
                            endedAt = if (resolvedStatus in TERMINAL_STATUSES) Instant.now() else instance.endedAt,
                            updatedAt = Instant.now()
                        )
                        instanceRepository.update(updated)
                        synced++
                    } else {
                        skipped++
                    }
                } catch (e: Exception) {
                    log.warn("Sync failed for instance {}: {}", instance.id, e.message)
                    failed++
                }
            }
        }

        log.info("Instance sync completed: synced={}, failed={}, skipped={}", synced, failed, skipped)
        return SyncResult(synced, failed, skipped)
    }

    private fun mapEngineStatus(engineStatus: EngineInstanceStatus.Status): InstanceStatus {
        return when (engineStatus) {
            EngineInstanceStatus.Status.RUNNING -> InstanceStatus.RUNNING
            EngineInstanceStatus.Status.SUSPENDED -> InstanceStatus.SUSPENDED
            EngineInstanceStatus.Status.COMPLETED -> InstanceStatus.COMPLETED
            EngineInstanceStatus.Status.CANCELLED -> InstanceStatus.CANCELLED
            EngineInstanceStatus.Status.FAILED -> InstanceStatus.FAILED
            EngineInstanceStatus.Status.UNKNOWN -> InstanceStatus.UNKNOWN
        }
    }

    companion object {
        private val SYNCABLE_STATUSES = setOf(InstanceStatus.RUNNING, InstanceStatus.SUSPENDED)
        private val TERMINAL_STATUSES = setOf(
            InstanceStatus.COMPLETED, InstanceStatus.CANCELLED,
            InstanceStatus.FAILED, InstanceStatus.TERMINATED
        )
    }
}
