package com.xwal.domain.port.input

import com.xwal.domain.model.EngineAdapterId

interface DeregisterAdapterUseCase {
    fun execute(adapterId: EngineAdapterId)
}
