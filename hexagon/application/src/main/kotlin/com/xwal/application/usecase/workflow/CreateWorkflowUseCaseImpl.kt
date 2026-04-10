package com.xwal.application.usecase.workflow

import com.xwal.domain.exception.DuplicateWorkflowException
import com.xwal.domain.model.*
import com.xwal.domain.port.input.CreateWorkflowUseCase
import com.xwal.domain.port.input.CreateWorkflowUseCase.Command
import com.xwal.domain.port.output.TransactionPort
import com.xwal.domain.port.output.WorkflowRepository
import com.xwal.domain.service.IwmValidationService
import java.time.Instant

class CreateWorkflowUseCaseImpl(
    private val workflowRepository: WorkflowRepository,
    private val transactionPort: TransactionPort,
    private val iwmValidationService: IwmValidationService
) : CreateWorkflowUseCase {

    override fun execute(command: Command): Workflow {
        iwmValidationService.validateOrThrow(command.iwmDefinition)

        return transactionPort.executeInTransaction {
            if (workflowRepository.existsByNameAndVersion(command.name, command.version)) {
                throw DuplicateWorkflowException(command.name, command.version)
            }

            val now = Instant.now()
            val workflow = Workflow(
                id = WorkflowId.generate(),
                name = command.name,
                version = command.version,
                description = command.description,
                iwmDefinition = IwmDefinition(command.iwmDefinition),
                status = WorkflowStatus.ACTIVE,
                createdAt = now,
                createdBy = command.createdBy,
                updatedAt = now,
                updatedBy = null
            )

            workflowRepository.save(workflow)
        }
    }
}
