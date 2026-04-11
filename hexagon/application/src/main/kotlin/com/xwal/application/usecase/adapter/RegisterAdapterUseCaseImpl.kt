package com.xwal.application.usecase.adapter

import com.xwal.application.service.AdapterResolutionService
import com.xwal.domain.model.*
import com.xwal.domain.port.input.RegisterAdapterUseCase
import com.xwal.domain.port.input.RegisterAdapterUseCase.Command
import com.xwal.domain.port.output.AdapterInstanceCachePort
import com.xwal.domain.port.output.EngineAdapterConfigRepository
import com.xwal.domain.port.output.TransactionPort
import org.slf4j.LoggerFactory
import java.time.Instant

class RegisterAdapterUseCaseImpl(
    private val adapterConfigRepository: EngineAdapterConfigRepository,
    private val adapterResolution: AdapterResolutionService,
    private val adapterCache: AdapterInstanceCachePort,
    private val transactionPort: TransactionPort
) : RegisterAdapterUseCase {

    private val log = LoggerFactory.getLogger(RegisterAdapterUseCaseImpl::class.java)

    override fun execute(command: Command): EngineAdapterConfig {
        val now = Instant.now()
        val config = EngineAdapterConfig(
            id = EngineAdapterId.generate(),
            name = command.name,
            engineType = command.engineType,
            config = mapOf("url" to command.config),
            capabilities = null,
            enabled = true,
            healthStatus = HealthStatus.UNKNOWN,
            lastHealthCheck = null,
            priority = command.priority,
            createdAt = now,
            createdBy = command.createdBy,
            updatedAt = now,
            updatedBy = null
        )

        // Save + initial health check in one transaction
        return transactionPort.executeInTransaction {
            val saved = adapterConfigRepository.save(config)

            try {
                val adapter = adapterResolution.resolveByConfig(saved)
                val healthy = adapter.checkHealth()
                val updated = if (healthy) {
                    saved.copy(healthStatus = HealthStatus.HEALTHY, lastHealthCheck = Instant.now())
                } else {
                    adapterCache.evict(saved.id)
                    saved.copy(enabled = false, healthStatus = HealthStatus.UNHEALTHY, lastHealthCheck = Instant.now())
                }
                adapterConfigRepository.update(updated)
            } catch (e: Exception) {
                log.warn("Initial health check failed for {}: {}", saved.name, e.message)
                adapterCache.evict(saved.id)
                val updated = saved.copy(
                    enabled = false,
                    healthStatus = HealthStatus.UNHEALTHY,
                    lastHealthCheck = Instant.now()
                )
                adapterConfigRepository.update(updated)
            }
        }
    }
}
