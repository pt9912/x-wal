package com.xwal.domain.port.input

import com.xwal.domain.model.InstanceId
import com.xwal.domain.model.WorkflowInstance

interface CancelInstanceUseCase {
    fun execute(instanceId: InstanceId, reason: String? = null): WorkflowInstance
}
