package com.xwal.application.usecase.workflow

import com.xwal.application.service.AdapterResolutionService
import com.xwal.domain.exception.InstanceNotFoundException
import com.xwal.domain.exception.InvalidStateTransitionException
import com.xwal.domain.model.InstanceId
import com.xwal.domain.model.InstanceStatus
import com.xwal.domain.model.WorkflowInstance
import com.xwal.domain.port.input.ResumeInstanceUseCase
import com.xwal.domain.port.output.InstanceRepository
import com.xwal.domain.port.output.TransactionPort
import java.time.Instant

class ResumeInstanceUseCaseImpl(
    private val instanceRepository: InstanceRepository,
    private val adapterResolution: AdapterResolutionService,
    private val transactionPort: TransactionPort
) : ResumeInstanceUseCase {

    override fun execute(instanceId: InstanceId): WorkflowInstance {
        val instance = instanceRepository.findById(instanceId)
            ?: throw InstanceNotFoundException(instanceId.toString())

        if (instance.status != InstanceStatus.SUSPENDED) {
            throw InvalidStateTransitionException(instance.status.name, "RUNNING", instanceId.toString())
        }

        val engineInstanceId = adapterResolution.getEngineInstanceId(instance, "resume")
        val adapter = adapterResolution.resolveForInstance(instance, "resume")
        adapter.resumeInstance(engineInstanceId)

        return transactionPort.executeInTransaction {
            val updated = instance.copy(status = InstanceStatus.RUNNING, updatedAt = Instant.now())
            instanceRepository.update(updated)
        }
    }
}
