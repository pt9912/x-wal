package com.xwal.adapter.persistence.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.xwal.adapter.persistence.entity.InstanceEntity
import com.xwal.domain.model.EngineAdapterId
import com.xwal.domain.model.InstanceId
import com.xwal.domain.model.InstanceStatus
import com.xwal.domain.model.WorkflowId
import com.xwal.domain.model.WorkflowInstance
import jakarta.inject.Singleton
import java.time.Instant

@Singleton
class InstanceEntityMapper(
    private val objectMapper: ObjectMapper
) {

    fun toDomain(entity: InstanceEntity): WorkflowInstance = WorkflowInstance(
        id = InstanceId(entity.id),
        workflowId = WorkflowId(entity.workflowId),
        engineInstanceId = entity.engineInstanceId,
        engineAdapterId = entity.engineAdapterId?.let { EngineAdapterId(it) },
        status = InstanceStatus.valueOf(entity.status),
        businessKey = entity.businessKey,
        variables = entity.variables?.let { objectMapper.readValue<Map<String, Any>>(it) },
        startedAt = entity.startedAt,
        startedBy = entity.startedBy,
        endedAt = entity.endedAt,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt
    )

    fun toEntity(domain: WorkflowInstance): InstanceEntity = InstanceEntity(
        id = domain.id.value,
        workflowId = domain.workflowId.value,
        engineInstanceId = domain.engineInstanceId,
        engineAdapterId = domain.engineAdapterId?.value,
        status = domain.status.name,
        businessKey = domain.businessKey,
        variables = domain.variables?.let { objectMapper.writeValueAsString(it) },
        startedAt = domain.startedAt,
        startedBy = domain.startedBy,
        endedAt = domain.endedAt,
        createdAt = domain.createdAt,
        updatedAt = domain.updatedAt
    )
}
