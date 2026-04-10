package com.xwal.adapter.web.rest.mapper

import com.xwal.adapter.web.rest.dto.response.WorkflowResponse
import com.xwal.domain.model.Workflow

object WorkflowDtoMapper {
    fun toResponse(workflow: Workflow) = WorkflowResponse(
        id = workflow.id.value,
        name = workflow.name,
        version = workflow.version,
        description = workflow.description,
        status = workflow.status.name,
        createdAt = workflow.createdAt,
        updatedAt = workflow.updatedAt
    )

    fun toResponseList(workflows: List<Workflow>) = workflows.map(::toResponse)
}
