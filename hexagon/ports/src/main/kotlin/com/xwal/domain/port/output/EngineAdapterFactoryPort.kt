package com.xwal.domain.port.output

import com.xwal.domain.model.EngineType

interface EngineAdapterFactoryPort {
    fun createAdapter(engineType: EngineType, config: Map<String, Any>): WorkflowEnginePort
    fun supportsEngineType(engineType: EngineType): Boolean
    fun getSupportedEngineTypes(): Set<EngineType>
}
