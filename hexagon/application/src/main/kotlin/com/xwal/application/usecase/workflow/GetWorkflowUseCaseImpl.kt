package com.xwal.application.usecase.workflow

import com.xwal.domain.exception.WorkflowNotFoundException
import com.xwal.domain.model.Workflow
import com.xwal.domain.model.WorkflowId
import com.xwal.domain.port.input.GetWorkflowUseCase
import com.xwal.domain.port.output.WorkflowRepository

class GetWorkflowUseCaseImpl(
    private val workflowRepository: WorkflowRepository
) : GetWorkflowUseCase {

    override fun execute(workflowId: WorkflowId): Workflow {
        return workflowRepository.findById(workflowId)
            ?: throw WorkflowNotFoundException(workflowId.toString())
    }
}
