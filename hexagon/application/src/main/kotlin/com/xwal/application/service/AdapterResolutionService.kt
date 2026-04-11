package com.xwal.application.service

import com.xwal.domain.exception.AdapterNotFoundException
import com.xwal.domain.exception.AdapterOperationException
import com.xwal.domain.model.EngineAdapterConfig
import com.xwal.domain.model.EngineAdapterId
import com.xwal.domain.model.EngineType
import com.xwal.domain.model.TaskId
import com.xwal.domain.model.WorkflowInstance
import com.xwal.domain.port.output.AdapterInstanceCachePort
import com.xwal.domain.port.output.EngineAdapterConfigRepository
import com.xwal.domain.port.output.EngineAdapterFactoryPort
import com.xwal.domain.port.output.WorkflowEnginePort

/**
 * Shared service for resolving WorkflowEnginePort instances.
 * Eliminates duplication across use cases and provides consistent
 * null-safety and error handling.
 */
class AdapterResolutionService(
    private val adapterConfigRepository: EngineAdapterConfigRepository,
    private val adapterCache: AdapterInstanceCachePort,
    private val adapterFactory: EngineAdapterFactoryPort
) {

    /** Resolve adapter from a WorkflowInstance. Validates nullable fields. */
    fun resolveForInstance(instance: WorkflowInstance, operation: String): WorkflowEnginePort {
        val adapterId = instance.engineAdapterId
            ?: throw AdapterOperationException(
                engineType = "unknown", operation = operation,
                message = "Instance ${instance.id} has no engineAdapterId"
            )
        return resolveById(adapterId)
    }

    /** Get engine instance ID with null-safety. */
    fun getEngineInstanceId(instance: WorkflowInstance, operation: String): String {
        return instance.engineInstanceId
            ?: throw AdapterOperationException(
                engineType = "unknown", operation = operation,
                message = "Instance ${instance.id} has no engineInstanceId"
            )
    }

    /** Resolve adapter by ID. Cache-first, then factory. */
    fun resolveById(adapterId: EngineAdapterId): WorkflowEnginePort {
        return adapterCache.get(adapterId) ?: run {
            val config = adapterConfigRepository.findById(adapterId)
                ?: throw AdapterNotFoundException(adapterId.toString())
            createAndCache(config)
        }
    }

    /** Resolve adapter by config directly. Cache-first, then factory. */
    fun resolveByConfig(config: EngineAdapterConfig): WorkflowEnginePort {
        return adapterCache.get(config.id) ?: createAndCache(config)
    }

    /** Find first enabled adapter for an engine type. */
    fun resolveByEngineType(engineType: EngineType): WorkflowEnginePort {
        val config = adapterConfigRepository.findByEngineType(engineType)
            .filter { it.enabled }
            .minWithOrNull(compareBy({ it.priority }, { it.id.value }))
            ?: throw AdapterNotFoundException("No enabled adapter for engine type: ${engineType.name}")
        return resolveByConfig(config)
    }

    fun resolveForTask(taskId: TaskId): WorkflowEnginePort {
        return taskId.adapterId?.let { resolveById(it) } ?: resolveByEngineType(taskId.engineType)
    }

    private fun createAndCache(config: EngineAdapterConfig): WorkflowEnginePort {
        val adapter = adapterFactory.createAdapter(config.engineType, config.config)
        adapterCache.put(config.id, adapter)
        return adapter
    }
}
