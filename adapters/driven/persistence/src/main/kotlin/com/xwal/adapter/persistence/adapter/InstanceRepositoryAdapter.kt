package com.xwal.adapter.persistence.adapter

import com.xwal.adapter.persistence.mapper.InstanceEntityMapper
import com.xwal.adapter.persistence.repository.MnInstanceRepository
import com.xwal.domain.model.*
import com.xwal.domain.port.output.InstanceRepository
import jakarta.inject.Singleton
import java.time.Instant

@Singleton
class InstanceRepositoryAdapter(
    private val mnRepo: MnInstanceRepository,
    private val mapper: InstanceEntityMapper
) : InstanceRepository {

    override fun save(instance: WorkflowInstance): WorkflowInstance =
        mapper.toDomain(mnRepo.save(mapper.toEntity(instance)))

    override fun update(instance: WorkflowInstance): WorkflowInstance =
        mapper.toDomain(mnRepo.update(mapper.toEntity(instance)))

    override fun findById(id: InstanceId): WorkflowInstance? =
        mnRepo.findById(id.value).orElse(null)?.let(mapper::toDomain)

    override fun findByEngineInstanceId(engineInstanceId: String): WorkflowInstance? =
        mnRepo.findByEngineInstanceId(engineInstanceId)?.let(mapper::toDomain)

    override fun findByWorkflowId(workflowId: WorkflowId): List<WorkflowInstance> =
        mnRepo.findByWorkflowId(workflowId.value).map(mapper::toDomain)

    override fun findByStatus(status: InstanceStatus): List<WorkflowInstance> =
        mnRepo.findByStatus(status.name).map(mapper::toDomain)

    override fun findByEngineAdapterId(adapterId: EngineAdapterId): List<WorkflowInstance> =
        mnRepo.findByEngineAdapterId(adapterId.value).map(mapper::toDomain)

    override fun findByBusinessKey(businessKey: String): List<WorkflowInstance> =
        mnRepo.findByBusinessKey(businessKey).map(mapper::toDomain)

    override fun countByStatus(status: InstanceStatus): Long =
        mnRepo.countByStatus(status.name)

    override fun findForSync(updatedBefore: Instant, lastId: InstanceId?, limit: Int): List<WorkflowInstance> =
        mnRepo.findForSync(updatedBefore, lastId?.value, limit).map(mapper::toDomain)
}
