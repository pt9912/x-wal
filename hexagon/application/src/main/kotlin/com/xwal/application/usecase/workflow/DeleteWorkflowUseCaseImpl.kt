package com.xwal.application.usecase.workflow

import com.xwal.domain.exception.WorkflowNotFoundException
import com.xwal.domain.model.WorkflowId
import com.xwal.domain.port.input.DeleteWorkflowUseCase
import com.xwal.domain.port.output.TransactionPort
import com.xwal.domain.port.output.WorkflowRepository

class DeleteWorkflowUseCaseImpl(
    private val workflowRepository: WorkflowRepository,
    private val transactionPort: TransactionPort
) : DeleteWorkflowUseCase {

    override fun execute(workflowId: WorkflowId) {
        transactionPort.executeInTransaction {
            workflowRepository.findById(workflowId)
                ?: throw WorkflowNotFoundException(workflowId.toString())
            workflowRepository.deleteById(workflowId)
        }
    }
}
