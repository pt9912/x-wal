package com.xwal.adapter.web.rest.mapper

import com.xwal.adapter.web.rest.dto.response.InstanceResponse
import com.xwal.domain.model.WorkflowInstance

object InstanceDtoMapper {
    fun toResponse(instance: WorkflowInstance) = InstanceResponse(
        id = instance.id.value,
        workflowId = instance.workflowId.value,
        engineInstanceId = instance.engineInstanceId,
        status = instance.status.name,
        businessKey = instance.businessKey,
        startedAt = instance.startedAt,
        endedAt = instance.endedAt
    )
}
