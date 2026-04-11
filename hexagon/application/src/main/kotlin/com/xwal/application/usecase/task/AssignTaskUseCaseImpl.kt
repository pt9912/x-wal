package com.xwal.application.usecase.task

import com.xwal.application.service.AdapterResolutionService
import com.xwal.domain.model.TaskId
import com.xwal.domain.port.input.AssignTaskUseCase

class AssignTaskUseCaseImpl(
    private val adapterResolution: AdapterResolutionService
) : AssignTaskUseCase {

    override fun execute(taskId: TaskId, assignee: String) {
        val adapter = adapterResolution.resolveForTask(taskId)
        adapter.assignTask(taskId.engineTaskId, assignee)
    }
}
