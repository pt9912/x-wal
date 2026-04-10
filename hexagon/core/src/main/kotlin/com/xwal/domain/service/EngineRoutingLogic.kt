package com.xwal.domain.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.xwal.domain.model.EngineAdapterConfig
import com.xwal.domain.model.EngineType
import com.xwal.domain.model.HealthStatus

/**
 * Pure domain logic for selecting the best adapter for a workflow.
 * No dependencies on output ports — operates on data passed in.
 */
object EngineRoutingLogic {

    private val objectMapper = ObjectMapper()

    /**
     * Selects the best adapter from the available list based on:
     * 1. Target engine type hint from IWM definition
     * 2. Health status
     * 3. Priority
     */
    fun selectAdapter(
        availableAdapters: List<EngineAdapterConfig>,
        targetEngineType: EngineType? = null
    ): EngineAdapterConfig? {
        val healthy = availableAdapters
            .filter { it.enabled && it.healthStatus == HealthStatus.HEALTHY }

        if (healthy.isEmpty()) return null

        // Prefer matching engine type
        if (targetEngineType != null) {
            val matching = healthy.filter { it.engineType == targetEngineType }
            if (matching.isNotEmpty()) {
                return matching.minByOrNull { it.priority }
            }
        }

        // Fallback: highest priority (lowest number)
        return healthy.minByOrNull { it.priority }
    }

    /**
     * Extracts target engine type hint from IWM JSON definition.
     * Looks for extensions.targetEngine or workflow.targetEngine.
     */
    fun extractTargetEngine(iwmDefinitionJson: String): EngineType? {
        return try {
            val root = objectMapper.readTree(iwmDefinitionJson)
            val targetValue = root.path("extensions").path("targetEngine").asText(null)
                ?: root.path("workflow").path("targetEngine").asText(null)
                ?: return null
            EngineType.entries.firstOrNull { it.name.equals(targetValue, ignoreCase = true) }
        } catch (_: Exception) {
            null
        }
    }
}
