package com.xwal.adapter.web.rest.dto.request

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class CompleteTaskRequest(
    val variables: Map<String, Any>? = null,
    val completedBy: String? = null
)
