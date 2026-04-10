package com.xwal.adapter.persistence.repository

import com.xwal.adapter.persistence.entity.InstanceEntity
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.time.Instant
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface MnInstanceRepository : CrudRepository<InstanceEntity, UUID> {
    fun findByEngineInstanceId(engineInstanceId: String): InstanceEntity?
    fun findByWorkflowId(workflowId: UUID): List<InstanceEntity>
    fun findByStatus(status: String): List<InstanceEntity>
    fun findByEngineAdapterId(engineAdapterId: UUID): List<InstanceEntity>
    fun findByBusinessKey(businessKey: String): List<InstanceEntity>
    fun countByStatus(status: String): Long

    @Query("SELECT * FROM iwm_instances WHERE updated_at < :updatedBefore AND (:lastId IS NULL OR (updated_at, id) < (SELECT updated_at, id FROM iwm_instances WHERE id = :lastId)) ORDER BY updated_at DESC, id DESC LIMIT :limit")
    fun findForSync(updatedBefore: Instant, lastId: UUID?, limit: Int): List<InstanceEntity>
}
