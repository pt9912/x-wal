package com.xwal.application.usecase.task

import com.xwal.application.service.AdapterResolutionService
import com.xwal.domain.model.Task
import com.xwal.domain.model.TaskId
import com.xwal.domain.port.input.GetTaskUseCase

class GetTaskUseCaseImpl(
    private val adapterResolution: AdapterResolutionService
) : GetTaskUseCase {

    override fun execute(taskId: TaskId): Task {
        val adapter = adapterResolution.resolveForTask(taskId)
        val task = adapter.getTask(taskId.engineTaskId)
        return task.copy(taskId = taskId)
    }
}
