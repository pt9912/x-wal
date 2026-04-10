package com.xwal.domain.port.output

import com.xwal.domain.model.Workflow
import com.xwal.domain.model.WorkflowId
import com.xwal.domain.model.WorkflowStatus

interface WorkflowRepository {
    fun save(workflow: Workflow): Workflow
    fun update(workflow: Workflow): Workflow
    fun findById(id: WorkflowId): Workflow?
    fun findByNameAndVersion(name: String, version: String): Workflow?
    fun findAll(): List<Workflow>
    fun findAllByStatus(status: WorkflowStatus): List<Workflow>
    fun existsByNameAndVersion(name: String, version: String): Boolean
    fun countByStatus(status: WorkflowStatus): Long
    fun deleteById(id: WorkflowId)
}
