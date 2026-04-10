package com.xwal.domain.port.input

import com.xwal.domain.model.EngineAdapterConfig
import com.xwal.domain.model.EngineAdapterId

interface HealthCheckAdapterUseCase {
    fun executeSingle(adapterId: EngineAdapterId): EngineAdapterConfig
    fun executeAll(): List<EngineAdapterConfig>
}
