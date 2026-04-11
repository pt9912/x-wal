package com.xwal.domain.model

import java.util.UUID

/**
 * Composite Task ID encoding engine type and actual engine task ID.
 * Format (legacy): "CAMUNDA7:task-123"
 * Format (engine+adapter): "CAMUNDA7:<uuid>:task-123"
 */
@JvmInline
value class TaskId(val value: String) {

    private val parsed = parse(value)

    init {
        require(parsed.engineTaskId.isNotBlank()) {
            "TaskId must include a non-empty taskId, got: $value"
        }
    }

    val engineType: EngineType
        get() = parsed.engineType

    val adapterId: EngineAdapterId?
        get() = parsed.adapterId

    private fun parse(taskId: String): ParsedTaskId {
        val firstColon = taskId.indexOf(':')
        require(firstColon > 0) {
            "TaskId must be in format 'ENGINE_TYPE:taskId' or 'ENGINE_TYPE:adapterId:taskId', got: $value"
        }

        val engineType = runCatching { EngineType.valueOf(taskId.substring(0, firstColon)) }.getOrElse {
            throw IllegalArgumentException("Unknown engine type in TaskId: ${taskId.substringBefore(':')}")
        }

        val remainder = taskId.substring(firstColon + 1)
        require(remainder.isNotBlank()) {
            "TaskId must include a non-empty taskId, got: $value"
        }

        val secondColon = remainder.indexOf(':')
        if (secondColon < 0) {
            return ParsedTaskId(engineType, null, remainder)
        }

        val possibleAdapterId = remainder.substring(0, secondColon)
        val taskIdPart = remainder.substring(secondColon + 1)
        if (isAdapterId(possibleAdapterId)) {
            require(taskIdPart.isNotBlank()) {
                "TaskId with adapterId must be in format 'ENGINE_TYPE:adapterId:taskId', got: $value"
            }
            return ParsedTaskId(engineType, EngineAdapterId(UUID.fromString(possibleAdapterId)), taskIdPart)
        }

        return ParsedTaskId(engineType, null, remainder)
    }

    private fun isAdapterId(value: String): Boolean = runCatching { UUID.fromString(value) }.isSuccess

    val engineTaskId: String
        get() = parsed.engineTaskId

    companion object {
        fun of(engineType: EngineType, engineTaskId: String): TaskId =
            TaskId("${engineType.name}:$engineTaskId")

        fun of(engineType: EngineType, adapterId: EngineAdapterId, engineTaskId: String): TaskId =
            TaskId("${engineType.name}:${adapterId.value}:$engineTaskId")
    }

    override fun toString(): String = value

    private data class ParsedTaskId(
        val engineType: EngineType,
        val adapterId: EngineAdapterId?,
        val engineTaskId: String
    )
}
