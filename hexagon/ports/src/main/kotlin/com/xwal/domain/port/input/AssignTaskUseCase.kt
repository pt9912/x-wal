package com.xwal.domain.port.input

import com.xwal.domain.model.TaskId

interface AssignTaskUseCase {
    fun execute(taskId: TaskId, assignee: String)
}
