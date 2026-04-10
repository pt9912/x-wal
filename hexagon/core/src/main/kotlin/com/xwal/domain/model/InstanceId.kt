package com.xwal.domain.model

import java.util.UUID

@JvmInline
value class InstanceId(val value: UUID) {
    companion object {
        fun generate(): InstanceId = InstanceId(UUID.randomUUID())
    }

    override fun toString(): String = value.toString()
}
