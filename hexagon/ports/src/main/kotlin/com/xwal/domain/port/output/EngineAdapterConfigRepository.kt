package com.xwal.domain.port.output

import com.xwal.domain.model.EngineAdapterConfig
import com.xwal.domain.model.EngineAdapterId
import com.xwal.domain.model.EngineType
import com.xwal.domain.model.HealthStatus

interface EngineAdapterConfigRepository {
    fun save(config: EngineAdapterConfig): EngineAdapterConfig
    fun update(config: EngineAdapterConfig): EngineAdapterConfig
    fun findById(id: EngineAdapterId): EngineAdapterConfig?
    fun findByName(name: String): EngineAdapterConfig?
    fun findAll(): List<EngineAdapterConfig>
    fun findByEngineType(engineType: EngineType): List<EngineAdapterConfig>
    fun findByEnabled(enabled: Boolean): List<EngineAdapterConfig>
    fun findByHealthStatus(healthStatus: HealthStatus): List<EngineAdapterConfig>
    fun deleteById(id: EngineAdapterId)
}
