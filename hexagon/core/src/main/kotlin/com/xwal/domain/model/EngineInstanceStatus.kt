package com.xwal.domain.model

import java.time.Instant

/**
 * Runtime status of a workflow instance as reported by the engine adapter.
 */
data class EngineInstanceStatus(
    val instanceId: String,
    val workflowKey: String?,
    val businessKey: String?,
    val status: Status,
    val startedAt: Instant?,
    val endedAt: Instant?,
    val variables: Map<String, Any> = emptyMap(),
    val currentActivityId: String?,
    val engineInstanceId: String?
) {
    enum class Status(val displayName: String) {
        RUNNING("Running"),
        SUSPENDED("Suspended"),
        COMPLETED("Completed"),
        CANCELLED("Cancelled"),
        FAILED("Failed"),
        UNKNOWN("Unknown")
    }
}
