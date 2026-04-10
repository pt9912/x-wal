package com.xwal.adapter.web.rest.dto.response

import io.micronaut.serde.annotation.Serdeable
import java.time.Instant
import java.util.UUID

@Serdeable
data class WorkflowResponse(
    val id: UUID,
    val name: String,
    val version: String,
    val description: String? = null,
    val status: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
