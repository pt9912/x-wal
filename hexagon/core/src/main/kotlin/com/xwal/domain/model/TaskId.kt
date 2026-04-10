package com.xwal.domain.model

/**
 * Composite Task ID encoding engine type and actual engine task ID.
 * Format: "CAMUNDA7:task-123"
 */
@JvmInline
value class TaskId(val value: String) {

    init {
        require(value.contains(':')) { "TaskId must be in format 'ENGINE_TYPE:taskId', got: $value" }
        require(runCatching { EngineType.valueOf(value.substringBefore(':')) }.isSuccess) {
            "Unknown engine type in TaskId: ${value.substringBefore(':')}"
        }
    }

    val engineType: EngineType
        get() = EngineType.valueOf(value.substringBefore(':'))

    val engineTaskId: String
        get() = value.substringAfter(':')

    companion object {
        fun of(engineType: EngineType, engineTaskId: String): TaskId =
            TaskId("${engineType.name}:$engineTaskId")
    }

    override fun toString(): String = value
}
