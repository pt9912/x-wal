package com.xwal.domain.port.input

import com.xwal.domain.model.EngineAdapterConfig
import com.xwal.domain.model.EngineType

interface RegisterAdapterUseCase {
    fun execute(command: Command): EngineAdapterConfig

    data class Command(
        val name: String,
        val engineType: EngineType,
        val config: String,
        val capabilities: String? = null,
        val priority: Int = 0,
        val createdBy: String?
    )
}
