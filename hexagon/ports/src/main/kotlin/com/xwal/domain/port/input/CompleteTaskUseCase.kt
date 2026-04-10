package com.xwal.domain.port.input

import com.xwal.domain.model.TaskId

interface CompleteTaskUseCase {
    fun execute(command: Command)

    data class Command(
        val taskId: TaskId,
        val variables: Map<String, Any> = emptyMap()
    )
}
