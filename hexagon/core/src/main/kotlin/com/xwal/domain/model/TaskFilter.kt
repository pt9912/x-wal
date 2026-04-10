package com.xwal.domain.model

data class TaskFilter(
    val assignee: String? = null,
    val candidateGroup: String? = null,
    val processInstanceId: String? = null,
    val status: TaskStatus? = null,
    val limit: Int = 50,
    val offset: Int = 0
)
