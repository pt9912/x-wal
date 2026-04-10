package com.xwal.domain.model

import java.time.Instant

data class Workflow(
    val id: WorkflowId,
    val name: String,
    val version: String,
    val description: String?,
    val iwmDefinition: IwmDefinition,
    val status: WorkflowStatus,
    val createdAt: Instant,
    val createdBy: String?,
    val updatedAt: Instant,
    val updatedBy: String?
)
