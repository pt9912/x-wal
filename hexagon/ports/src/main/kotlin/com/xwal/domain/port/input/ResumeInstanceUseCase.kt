package com.xwal.domain.port.input

import com.xwal.domain.model.InstanceId
import com.xwal.domain.model.WorkflowInstance

interface ResumeInstanceUseCase {
    fun execute(instanceId: InstanceId): WorkflowInstance
}
