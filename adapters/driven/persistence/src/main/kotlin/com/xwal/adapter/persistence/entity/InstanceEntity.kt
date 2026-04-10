package com.xwal.adapter.persistence.entity

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.model.DataType
import java.time.Instant
import java.util.UUID

@MappedEntity("iwm_instances")
data class InstanceEntity(
    @field:Id
    val id: UUID,
    val workflowId: UUID,
    val engineInstanceId: String? = null,
    val engineAdapterId: UUID? = null,
    val status: String = "RUNNING",
    val businessKey: String? = null,
    @field:TypeDef(type = DataType.JSON)
    val variables: String? = null,
    val startedAt: Instant? = null,
    val startedBy: String? = null,
    val endedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)
