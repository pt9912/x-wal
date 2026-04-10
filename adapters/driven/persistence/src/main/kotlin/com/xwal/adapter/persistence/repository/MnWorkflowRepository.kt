package com.xwal.adapter.persistence.repository

import com.xwal.adapter.persistence.entity.WorkflowEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface MnWorkflowRepository : CrudRepository<WorkflowEntity, UUID> {
    fun findByNameAndVersion(name: String, version: String): WorkflowEntity?
    fun findByStatus(status: String): List<WorkflowEntity>
    fun existsByNameAndVersion(name: String, version: String): Boolean
    fun countByStatus(status: String): Long
}
