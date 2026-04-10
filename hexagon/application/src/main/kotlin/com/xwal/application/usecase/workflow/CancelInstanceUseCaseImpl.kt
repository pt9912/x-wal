package com.xwal.application.usecase.workflow

import com.xwal.application.service.AdapterResolutionService
import com.xwal.domain.exception.InstanceNotFoundException
import com.xwal.domain.exception.InvalidStateTransitionException
import com.xwal.domain.model.InstanceId
import com.xwal.domain.model.InstanceStatus
import com.xwal.domain.model.WorkflowInstance
import com.xwal.domain.port.input.CancelInstanceUseCase
import com.xwal.domain.port.output.InstanceRepository
import com.xwal.domain.port.output.TransactionPort
import java.time.Instant

class CancelInstanceUseCaseImpl(
    private val instanceRepository: InstanceRepository,
    private val adapterResolution: AdapterResolutionService,
    private val transactionPort: TransactionPort
) : CancelInstanceUseCase {

    override fun execute(instanceId: InstanceId, reason: String?): WorkflowInstance {
        val instance = instanceRepository.findById(instanceId)
            ?: throw InstanceNotFoundException(instanceId.toString())

        if (instance.status in TERMINAL_STATUSES) {
            throw InvalidStateTransitionException(instance.status.name, "CANCELLED", instanceId.toString())
        }

        val engineInstanceId = adapterResolution.getEngineInstanceId(instance, "cancel")
        val adapter = adapterResolution.resolveForInstance(instance, "cancel")
        adapter.cancelInstance(engineInstanceId, reason ?: "Cancelled by user")

        val now = Instant.now()
        return transactionPort.executeInTransaction {
            val updated = instance.copy(status = InstanceStatus.CANCELLED, endedAt = now, updatedAt = now)
            instanceRepository.update(updated)
        }
    }

    companion object {
        private val TERMINAL_STATUSES = setOf(
            InstanceStatus.COMPLETED, InstanceStatus.CANCELLED,
            InstanceStatus.FAILED, InstanceStatus.TERMINATED
        )
    }
}
