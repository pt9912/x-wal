package com.xwal.application.usecase.adapter

import com.xwal.domain.exception.AdapterNotFoundException
import com.xwal.domain.model.EngineAdapterId
import com.xwal.domain.port.input.DeregisterAdapterUseCase
import com.xwal.domain.port.output.AdapterInstanceCachePort
import com.xwal.domain.port.output.EngineAdapterConfigRepository
import com.xwal.domain.port.output.InstanceRepository
import com.xwal.domain.port.output.TransactionPort
import com.xwal.domain.model.InstanceStatus
import org.slf4j.LoggerFactory
import java.time.Instant

class DeregisterAdapterUseCaseImpl(
    private val adapterConfigRepository: EngineAdapterConfigRepository,
    private val adapterCache: AdapterInstanceCachePort,
    private val instanceRepository: InstanceRepository,
    private val transactionPort: TransactionPort
) : DeregisterAdapterUseCase {

    private val log = LoggerFactory.getLogger(DeregisterAdapterUseCaseImpl::class.java)

    override fun execute(adapterId: EngineAdapterId) {
        val config = adapterConfigRepository.findById(adapterId)
            ?: throw AdapterNotFoundException(adapterId.toString())

        // Warn if active instances reference this adapter
        val activeInstances = instanceRepository.findByEngineAdapterId(adapterId)
            .filter { it.status in listOf(InstanceStatus.RUNNING, InstanceStatus.SUSPENDED) }
        if (activeInstances.isNotEmpty()) {
            log.warn(
                "Deregistering adapter {} with {} active instances — these will become unmanageable",
                config.name, activeInstances.size
            )
        }

        transactionPort.executeInTransaction {
            val disabled = config.copy(enabled = false, updatedAt = Instant.now())
            adapterConfigRepository.update(disabled)
        }
        adapterCache.evict(adapterId)
    }
}
