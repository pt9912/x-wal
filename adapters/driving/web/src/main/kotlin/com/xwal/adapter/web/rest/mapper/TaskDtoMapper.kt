package com.xwal.adapter.web.rest.mapper

import com.xwal.adapter.web.rest.dto.response.TaskResponse
import com.xwal.domain.model.Task

object TaskDtoMapper {
    fun toResponse(task: Task) = TaskResponse(
        id = task.taskId.value,
        name = task.name,
        assignee = task.assignee,
        status = task.status?.name ?: "CREATED",
        workflowId = task.processDefinitionKey,
        instanceId = task.processInstanceId,
        createdAt = task.createdAt,
        dueDate = task.dueDate,
        formData = task.variables.takeIf { it.isNotEmpty() }
    )

    fun toResponseList(tasks: List<Task>) = tasks.map(::toResponse)
}
