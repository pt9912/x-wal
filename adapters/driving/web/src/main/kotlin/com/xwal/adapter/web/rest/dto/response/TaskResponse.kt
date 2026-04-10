package com.xwal.adapter.web.rest.dto.response

import io.micronaut.serde.annotation.Serdeable
import java.time.Instant

@Serdeable
data class TaskResponse(
    val id: String,
    val name: String? = null,
    val assignee: String? = null,
    val status: String? = null,
    val workflowId: String? = null,
    val instanceId: String? = null,
    val createdAt: Instant? = null,
    val dueDate: Instant? = null,
    val formData: Map<String, Any>? = null
)
