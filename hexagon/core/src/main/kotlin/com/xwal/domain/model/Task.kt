package com.xwal.domain.model

import java.time.Instant

data class Task(
    val taskId: TaskId,
    val name: String?,
    val type: String?,
    val description: String?,
    val assignee: String?,
    val owner: String?,
    val processInstanceId: String?,
    val processDefinitionKey: String?,
    val status: TaskStatus?,
    val createdAt: Instant?,
    val dueDate: Instant?,
    val followUpDate: Instant?,
    val priority: Int = 50,
    val variables: Map<String, Any> = emptyMap()
)
