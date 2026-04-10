package com.xwal.adapter.persistence.adapter

import com.xwal.adapter.persistence.mapper.AdapterEntityMapper
import com.xwal.adapter.persistence.repository.MnAdapterRepository
import com.xwal.domain.model.*
import com.xwal.domain.port.output.EngineAdapterConfigRepository
import jakarta.inject.Singleton

@Singleton
class AdapterConfigRepositoryAdapter(
    private val mnRepo: MnAdapterRepository,
    private val mapper: AdapterEntityMapper
) : EngineAdapterConfigRepository {

    override fun save(config: EngineAdapterConfig): EngineAdapterConfig =
        mapper.toDomain(mnRepo.save(mapper.toEntity(config)))

    override fun update(config: EngineAdapterConfig): EngineAdapterConfig =
        mapper.toDomain(mnRepo.update(mapper.toEntity(config)))

    override fun findById(id: EngineAdapterId): EngineAdapterConfig? =
        mnRepo.findById(id.value).orElse(null)?.let(mapper::toDomain)

    override fun findByName(name: String): EngineAdapterConfig? =
        mnRepo.findByName(name)?.let(mapper::toDomain)

    override fun findAll(): List<EngineAdapterConfig> =
        mnRepo.findAll().map(mapper::toDomain)

    override fun findByEngineType(engineType: EngineType): List<EngineAdapterConfig> =
        mnRepo.findByEngineType(engineType.name).map(mapper::toDomain)

    override fun findByEnabled(enabled: Boolean): List<EngineAdapterConfig> =
        mnRepo.findByEnabled(enabled).map(mapper::toDomain)

    override fun findByHealthStatus(healthStatus: HealthStatus): List<EngineAdapterConfig> =
        mnRepo.findByHealthStatus(healthStatus.name).map(mapper::toDomain)

    override fun deleteById(id: EngineAdapterId) =
        mnRepo.deleteById(id.value)
}
