package com.xwal.domain.port.input

import com.xwal.domain.model.Task
import com.xwal.domain.model.TaskId

interface GetTaskUseCase {
    fun execute(taskId: TaskId): Task
}
