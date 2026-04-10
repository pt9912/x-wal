package com.xwal.domain.port.input

import com.xwal.domain.model.InstanceId

interface GetInstanceVariablesUseCase {
    fun execute(instanceId: InstanceId): Map<String, Any>
}
