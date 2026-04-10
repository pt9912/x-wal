package com.xwal.domain.model

import java.time.Instant

data class WorkflowInstance(
    val id: InstanceId,
    val workflowId: WorkflowId,
    val engineInstanceId: String?,
    val engineAdapterId: EngineAdapterId?,
    val status: InstanceStatus,
    val businessKey: String?,
    val variables: Map<String, Any>? = null,
    val startedAt: Instant?,
    val startedBy: String?,
    val endedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
)
