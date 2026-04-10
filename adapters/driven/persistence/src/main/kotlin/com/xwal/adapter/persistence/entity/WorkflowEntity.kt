package com.xwal.adapter.persistence.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.time.Instant
import java.util.UUID

@MappedEntity("iwm_workflows")
data class WorkflowEntity(
    @field:Id
    val id: UUID,
    val name: String,
    val version: String,
    val description: String? = null,
    @field:TypeDef(type = DataType.JSON)
    val iwmDefinition: String,
    val status: String = "DRAFT",
    val createdAt: Instant,
    val createdBy: String? = null,
    val updatedAt: Instant,
    val updatedBy: String? = null
)
