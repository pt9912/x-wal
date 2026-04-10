package com.xwal.adapter.persistence.repository

import com.xwal.adapter.persistence.entity.EngineAdapterEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.UUID

@JdbcRepository(dialect = Dialect.POSTGRES)
interface MnAdapterRepository : CrudRepository<EngineAdapterEntity, UUID> {
    fun findByName(name: String): EngineAdapterEntity?
    fun findByEngineType(engineType: String): List<EngineAdapterEntity>
    fun findByEnabled(enabled: Boolean): List<EngineAdapterEntity>
    fun findByHealthStatus(healthStatus: String): List<EngineAdapterEntity>
}
