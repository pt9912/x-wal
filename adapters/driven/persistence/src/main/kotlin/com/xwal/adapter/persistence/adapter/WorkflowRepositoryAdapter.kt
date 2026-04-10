package com.xwal.adapter.persistence.adapter

import com.xwal.adapter.persistence.mapper.WorkflowEntityMapper
import com.xwal.adapter.persistence.repository.MnWorkflowRepository
import com.xwal.domain.model.Workflow
import com.xwal.domain.model.WorkflowId
import com.xwal.domain.model.WorkflowStatus
import com.xwal.domain.port.output.WorkflowRepository
import jakarta.inject.Singleton

@Singleton
class WorkflowRepositoryAdapter(
    private val mnRepo: MnWorkflowRepository,
    private val mapper: WorkflowEntityMapper
) : WorkflowRepository {

    override fun save(workflow: Workflow): Workflow =
        mapper.toDomain(mnRepo.save(mapper.toEntity(workflow)))

    override fun update(workflow: Workflow): Workflow =
        mapper.toDomain(mnRepo.update(mapper.toEntity(workflow)))

    override fun findById(id: WorkflowId): Workflow? =
        mnRepo.findById(id.value).orElse(null)?.let(mapper::toDomain)

    override fun findByNameAndVersion(name: String, version: String): Workflow? =
        mnRepo.findByNameAndVersion(name, version)?.let(mapper::toDomain)

    override fun findAll(): List<Workflow> =
        mnRepo.findAll().map(mapper::toDomain)

    override fun findAllByStatus(status: WorkflowStatus): List<Workflow> =
        mnRepo.findByStatus(status.name).map(mapper::toDomain)

    override fun existsByNameAndVersion(name: String, version: String): Boolean =
        mnRepo.existsByNameAndVersion(name, version)

    override fun countByStatus(status: WorkflowStatus): Long =
        mnRepo.countByStatus(status.name)

    override fun deleteById(id: WorkflowId) =
        mnRepo.deleteById(id.value)
}
