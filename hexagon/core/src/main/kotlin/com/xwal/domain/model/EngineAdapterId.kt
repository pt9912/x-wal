package com.xwal.domain.model

import java.util.UUID

@JvmInline
value class EngineAdapterId(val value: UUID) {
    companion object {
        fun generate(): EngineAdapterId = EngineAdapterId(UUID.randomUUID())
    }

    override fun toString(): String = value.toString()
}
