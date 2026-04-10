package com.xwal.application.usecase.workflow

import com.xwal.application.service.AdapterResolutionService
import com.xwal.domain.exception.InstanceNotFoundException
import com.xwal.domain.model.InstanceId
import com.xwal.domain.port.input.GetInstanceVariablesUseCase
import com.xwal.domain.port.output.InstanceRepository

class GetInstanceVariablesUseCaseImpl(
    private val instanceRepository: InstanceRepository,
    private val adapterResolution: AdapterResolutionService
) : GetInstanceVariablesUseCase {

    override fun execute(instanceId: InstanceId): Map<String, Any> {
        val instance = instanceRepository.findById(instanceId)
            ?: throw InstanceNotFoundException(instanceId.toString())

        val engineInstanceId = adapterResolution.getEngineInstanceId(instance, "getVariables")
        val adapter = adapterResolution.resolveForInstance(instance, "getVariables")
        return adapter.getInstanceVariables(engineInstanceId)
    }
}
