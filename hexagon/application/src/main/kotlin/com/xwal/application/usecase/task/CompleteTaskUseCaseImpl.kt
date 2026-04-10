package com.xwal.application.usecase.task

import com.xwal.application.service.AdapterResolutionService
import com.xwal.domain.port.input.CompleteTaskUseCase
import com.xwal.domain.port.input.CompleteTaskUseCase.Command

class CompleteTaskUseCaseImpl(
    private val adapterResolution: AdapterResolutionService
) : CompleteTaskUseCase {

    override fun execute(command: Command) {
        val adapter = adapterResolution.resolveByEngineType(command.taskId.engineType)
        adapter.completeTask(command.taskId.engineTaskId, command.variables)
    }
}
