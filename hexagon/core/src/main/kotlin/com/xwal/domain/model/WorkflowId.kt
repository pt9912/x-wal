package com.xwal.domain.model

import java.util.UUID

@JvmInline
value class WorkflowId(val value: UUID) {
    companion object {
        fun generate(): WorkflowId = WorkflowId(UUID.randomUUID())
    }

    override fun toString(): String = value.toString()
}
