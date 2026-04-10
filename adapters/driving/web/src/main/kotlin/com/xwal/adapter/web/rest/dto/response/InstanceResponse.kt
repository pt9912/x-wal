package com.xwal.adapter.web.rest.dto.response

import io.micronaut.serde.annotation.Serdeable
import java.time.Instant
import java.util.UUID

@Serdeable
data class InstanceResponse(
    val id: UUID,
    val workflowId: UUID,
    val engineInstanceId: String? = null,
    val status: String,
    val businessKey: String? = null,
    val startedAt: Instant? = null,
    val endedAt: Instant? = null
)
