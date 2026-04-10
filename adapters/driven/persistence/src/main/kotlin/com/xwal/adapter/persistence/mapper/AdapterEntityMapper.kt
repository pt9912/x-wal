package com.xwal.adapter.persistence.mapper

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.xwal.adapter.persistence.entity.EngineAdapterEntity
import com.xwal.domain.model.*
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton
class AdapterEntityMapper(
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(AdapterEntityMapper::class.java)

    fun toDomain(entity: EngineAdapterEntity): EngineAdapterConfig = EngineAdapterConfig(
        id = EngineAdapterId(entity.id),
        name = entity.name,
        engineType = EngineType.valueOf(entity.engineType),
        config = entity.config.let { objectMapper.readValue<Map<String, Any>>(it) },
        capabilities = entity.capabilities?.let { parseCapabilities(it) },
        enabled = entity.enabled,
        healthStatus = HealthStatus.valueOf(entity.healthStatus),
        lastHealthCheck = entity.lastHealthCheck,
        priority = entity.priority,
        createdAt = entity.createdAt,
        createdBy = entity.createdBy,
        updatedAt = entity.updatedAt,
        updatedBy = entity.updatedBy
    )

    fun toEntity(domain: EngineAdapterConfig): EngineAdapterEntity = EngineAdapterEntity(
        id = domain.id.value,
        name = domain.name,
        engineType = domain.engineType.name,
        config = objectMapper.writeValueAsString(domain.config),
        capabilities = domain.capabilities?.let { objectMapper.writeValueAsString(it) },
        enabled = domain.enabled,
        healthStatus = domain.healthStatus.name,
        lastHealthCheck = domain.lastHealthCheck,
        priority = domain.priority,
        createdAt = domain.createdAt,
        createdBy = domain.createdBy,
        updatedAt = domain.updatedAt,
        updatedBy = domain.updatedBy
    )

    private fun parseCapabilities(json: String): AdapterCapabilities? {
        return try {
            objectMapper.readValue<AdapterCapabilities>(json)
        } catch (e: JsonProcessingException) {
            log.warn("Failed to deserialize AdapterCapabilities from JSON: {}", e.message)
            null
        }
    }
}
