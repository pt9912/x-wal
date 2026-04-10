package com.xwal.domain.model

import java.time.Instant

/**
 * History data for completed/failed instances.
 *
 * deleteReason mapping (engine-specific):
 * - null / "completed" -> COMPLETED
 * - "deleted" / "externallyTerminated" / "terminated" -> CANCELLED
 * - "error:..." -> FAILED
 */
data class HistoricInstanceInfo(
    val instanceId: String,
    val processDefinitionId: String?,
    val businessKey: String?,
    val startTime: Instant?,
    val endTime: Instant?,
    val durationInMillis: Long?,
    val deleteReason: String?,
    val variables: Map<String, Any> = emptyMap()
)
