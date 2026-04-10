package com.xwal.application.usecase.adapter

import com.xwal.application.service.AdapterResolutionService
import com.xwal.domain.exception.AdapterNotFoundException
import com.xwal.domain.model.EngineAdapterConfig
import com.xwal.domain.model.EngineAdapterId
import com.xwal.domain.model.HealthStatus
import com.xwal.domain.port.input.HealthCheckAdapterUseCase
import com.xwal.domain.port.output.EngineAdapterConfigRepository
import org.slf4j.LoggerFactory
import java.time.Instant

class HealthCheckAdapterUseCaseImpl(
    private val adapterConfigRepository: EngineAdapterConfigRepository,
    private val adapterResolution: AdapterResolutionService
) : HealthCheckAdapterUseCase {

    private val log = LoggerFactory.getLogger(HealthCheckAdapterUseCaseImpl::class.java)

    override fun executeSingle(adapterId: EngineAdapterId): EngineAdapterConfig {
        val config = adapterConfigRepository.findById(adapterId)
            ?: throw AdapterNotFoundException(adapterId.toString())
        return performHealthCheck(config)
    }

    override fun executeAll(): List<EngineAdapterConfig> {
        return adapterConfigRepository.findByEnabled(true).map { config ->
            try {
                performHealthCheck(config)
            } catch (e: Exception) {
                log.warn("Health check failed for {}: {}", config.name, e.message)
                markUnhealthy(config)
            }
        }
    }

    private fun performHealthCheck(config: EngineAdapterConfig): EngineAdapterConfig {
        return try {
            val adapter = adapterResolution.resolveByConfig(config)
            val healthy = adapter.checkHealth()
            val status = if (healthy) HealthStatus.HEALTHY else HealthStatus.UNHEALTHY
            val updated = config.copy(healthStatus = status, lastHealthCheck = Instant.now())
            adapterConfigRepository.update(updated)
        } catch (e: Exception) {
            log.warn("Health check error for {}: {}", config.name, e.message)
            markUnhealthy(config)
        }
    }

    private fun markUnhealthy(config: EngineAdapterConfig): EngineAdapterConfig {
        val updated = config.copy(healthStatus = HealthStatus.UNHEALTHY, lastHealthCheck = Instant.now())
        return adapterConfigRepository.update(updated)
    }
}
