package com.xwal.application.usecase.workflow

import com.xwal.domain.exception.WorkflowNotFoundException
import com.xwal.domain.model.Workflow
import com.xwal.domain.port.input.UpdateWorkflowUseCase
import com.xwal.domain.port.input.UpdateWorkflowUseCase.Command
import com.xwal.domain.port.output.TransactionPort
import com.xwal.domain.port.output.WorkflowRepository
import java.time.Instant

class UpdateWorkflowUseCaseImpl(
    private val workflowRepository: WorkflowRepository,
    private val transactionPort: TransactionPort
) : UpdateWorkflowUseCase {

    override fun execute(command: Command): Workflow {
        return transactionPort.executeInTransaction {
            val workflow = workflowRepository.findById(command.workflowId)
                ?: throw WorkflowNotFoundException(command.workflowId.toString())

            val updated = workflow.copy(
                description = command.description ?: workflow.description,
                status = command.status ?: workflow.status,
                updatedAt = Instant.now(),
                updatedBy = command.updatedBy
            )

            workflowRepository.update(updated)
        }
    }
}
