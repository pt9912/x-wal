package com.xwal.application.usecase.workflow

import com.xwal.domain.exception.InstanceNotFoundException
import com.xwal.domain.model.InstanceId
import com.xwal.domain.model.WorkflowInstance
import com.xwal.domain.port.input.GetInstanceUseCase
import com.xwal.domain.port.output.InstanceRepository

class GetInstanceUseCaseImpl(
    private val instanceRepository: InstanceRepository
) : GetInstanceUseCase {

    override fun execute(instanceId: InstanceId): WorkflowInstance {
        return instanceRepository.findById(instanceId)
            ?: throw InstanceNotFoundException(instanceId.toString())
    }
}
