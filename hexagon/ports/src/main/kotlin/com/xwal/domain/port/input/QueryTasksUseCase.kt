package com.xwal.domain.port.input

import com.xwal.domain.model.Task
import com.xwal.domain.model.TaskFilter

interface QueryTasksUseCase {
    fun execute(filter: TaskFilter): List<Task>
}
