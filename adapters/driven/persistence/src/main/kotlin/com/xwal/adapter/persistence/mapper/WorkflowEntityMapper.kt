package com.xwal.adapter.persistence.mapper

import com.xwal.adapter.persistence.entity.WorkflowEntity
import com.xwal.domain.model.IwmDefinition
import com.xwal.domain.model.Workflow
import com.xwal.domain.model.WorkflowId
import com.xwal.domain.model.WorkflowStatus
import jakarta.inject.Singleton
import java.time.Instant

@Singleton
class WorkflowEntityMapper {

    fun toDomain(entity: WorkflowEntity): Workflow = Workflow(
        id = WorkflowId(entity.id),
        name = entity.name,
        version = entity.version,
        description = entity.description,
        iwmDefinition = IwmDefinition(entity.iwmDefinition),
        status = WorkflowStatus.valueOf(entity.status),
        createdAt = entity.createdAt,
        createdBy = entity.createdBy,
        updatedAt = entity.updatedAt,
        updatedBy = entity.updatedBy
    )

    fun toEntity(domain: Workflow): WorkflowEntity = WorkflowEntity(
        id = domain.id.value,
        name = domain.name,
        version = domain.version,
        description = domain.description,
        iwmDefinition = domain.iwmDefinition.json,
        status = domain.status.name,
        createdAt = domain.createdAt,
        createdBy = domain.createdBy,
        updatedAt = domain.updatedAt,
        updatedBy = domain.updatedBy
    )
}
