package com.xwal.adapter.persistence.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.time.Instant
import java.util.UUID

@MappedEntity("engine_adapters")
data class EngineAdapterEntity(
    @field:Id
    val id: UUID,
    val name: String,
    val engineType: String,
    @field:TypeDef(type = DataType.JSON)
    val config: String,
    @field:TypeDef(type = DataType.JSON)
    val capabilities: String? = null,
    val enabled: Boolean = true,
    val healthStatus: String = "UNKNOWN",
    val lastHealthCheck: Instant? = null,
    val priority: Int = 0,
    val createdAt: Instant,
    val createdBy: String? = null,
    val updatedAt: Instant,
    val updatedBy: String? = null
)
