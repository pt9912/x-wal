package com.xwal.domain.model

import java.time.Instant

data class Incident(
    val id: String,
    val processInstanceId: String?,
    val executionId: String?,
    val activityId: String?,
    val type: IncidentType,
    val message: String?,
    val cause: String?,
    val createdTime: Instant?,
    val resolved: Boolean = false
)
