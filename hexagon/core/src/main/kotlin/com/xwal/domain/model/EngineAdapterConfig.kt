package com.xwal.domain.model

import java.time.Instant

data class EngineAdapterConfig(
    val id: EngineAdapterId,
    val name: String,
    val engineType: EngineType,
    val config: Map<String, Any> = emptyMap(),
    val capabilities: AdapterCapabilities? = null,
    val enabled: Boolean,
    val healthStatus: HealthStatus,
    val lastHealthCheck: Instant?,
    val priority: Int,
    val createdAt: Instant,
    val createdBy: String?,
    val updatedAt: Instant,
    val updatedBy: String?
) {
    val endpointUrl: String?
        get() = config["url"] as? String
}
