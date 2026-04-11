package com.xwal.application.usecase.sync

import com.xwal.application.service.AdapterResolutionService
import com.xwal.domain.exception.AdapterOperationException
import com.xwal.domain.model.EngineInstanceStatus
import com.xwal.domain.model.InstanceId
import com.xwal.domain.model.InstanceStatus
import com.xwal.domain.port.input.SyncInstanceStateUseCase
import com.xwal.domain.port.input.SyncInstanceStateUseCase.SyncResult
import com.xwal.domain.port.output.DistributedLockPort
import com.xwal.domain.port.output.InstanceRepository
import java.io.IOException
import org.slf4j.LoggerFactory
import kotlin.math.max
import java.util.concurrent.TimeoutException
import java.time.Instant

class SyncInstanceStateUseCaseImpl(
    private val instanceRepository: InstanceRepository,
    private val adapterResolution: AdapterResolutionService,
    private val distributedLock: DistributedLockPort,
    private val options: SyncInstanceStateOptions
) : SyncInstanceStateUseCase {

    private val log = LoggerFactory.getLogger(SyncInstanceStateUseCaseImpl::class.java)

    override fun execute(): SyncResult {
        return distributedLock.withLock("instance-sync", options.timeout) {
            var synced = 0
            var failed = 0
            var skipped = 0
            var lastId: InstanceId? = null
            val cutoff = Instant.now().minusSeconds(30)

            while (true) {
                val batch = instanceRepository.findForSync(
                    updatedBefore = cutoff,
                    lastId = lastId,
                    limit = options.batchSize
                )
                if (batch.isEmpty()) break

                for (instance in batch) {
                    lastId = instance.id

                    if (!isSyncable(instance.status)) {
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
                        val engineStatus = executeWithRetry {
                            adapter.getInstanceStatus(engineInstanceId)
                        }
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
            SyncResult(synced, failed, skipped)
        }
    }

    private fun isSyncable(status: InstanceStatus): Boolean {
        return when (status) {
            InstanceStatus.RUNNING -> true
            InstanceStatus.SUSPENDED -> options.includeSuspended
            else -> false
        }
    }

    private fun <T> executeWithRetry(block: () -> T): T {
        if (!options.retryEnabled) {
            return block()
        }

        val maxAttempts = max(1, options.retryMaxAttempts)
        var attempt = 1
        var lastError: Exception? = null

        while (attempt <= maxAttempts) {
            try {
                return block()
            } catch (e: Exception) {
                if (!isRetryableException(e)) {
                    throw e
                }

                lastError = e
                if (attempt >= maxAttempts) {
                    throw e
                }

                log.warn(
                    "Engine status lookup failed (attempt {}/{}), retrying in {}",
                    attempt,
                    maxAttempts,
                    options.retryBackoff
                )
                Thread.sleep(options.retryBackoff.toMillis())
                attempt++
            }
        }

        throw lastError ?: IllegalStateException("Retry failed after $maxAttempts attempts")
    }

    private fun isRetryableException(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            if (current is AdapterOperationException ||
                current is IOException ||
                current is TimeoutException) {
                return true
            }
            current = current.cause
        }
        return false
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
        private val TERMINAL_STATUSES = setOf(
            InstanceStatus.COMPLETED, InstanceStatus.CANCELLED,
            InstanceStatus.FAILED, InstanceStatus.TERMINATED
        )
    }
}
