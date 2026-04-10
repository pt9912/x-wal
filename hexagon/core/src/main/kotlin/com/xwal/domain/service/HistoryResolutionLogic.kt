package com.xwal.domain.service

import com.xwal.domain.model.InstanceStatus

/**
 * Pure domain logic for resolving instance status from engine-specific delete reasons.
 *
 * Engine delete reason mapping:
 * - null / "completed" -> COMPLETED
 * - "deleted" / "externallyTerminated" / "terminated" -> CANCELLED
 * - starts with "error:" -> FAILED
 */
object HistoryResolutionLogic {

    fun resolveStatus(deleteReason: String?): InstanceStatus {
        if (deleteReason == null) return InstanceStatus.COMPLETED

        val lower = deleteReason.lowercase()
        return when {
            lower == "completed" -> InstanceStatus.COMPLETED
            lower == "deleted" -> InstanceStatus.CANCELLED
            lower == "externallyterminated" -> InstanceStatus.CANCELLED
            lower == "terminated" -> InstanceStatus.CANCELLED
            lower.startsWith("error:") -> InstanceStatus.FAILED
            else -> InstanceStatus.TERMINATED
        }
    }
}
