package com.xwal.application.usecase.workflow

import com.xwal.domain.model.Workflow
import com.xwal.domain.model.WorkflowStatus
import com.xwal.domain.port.input.ListWorkflowsUseCase
import com.xwal.domain.port.output.WorkflowRepository

class ListWorkflowsUseCaseImpl(
    private val workflowRepository: WorkflowRepository
) : ListWorkflowsUseCase {

    override fun execute(status: WorkflowStatus?): List<Workflow> {
        return if (status != null) {
            workflowRepository.findAllByStatus(status)
        } else {
            workflowRepository.findAll()
        }
    }
}
