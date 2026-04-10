package com.xwal.domain.port.input

import com.xwal.domain.model.Workflow
import com.xwal.domain.model.WorkflowId
import com.xwal.domain.model.WorkflowStatus

interface UpdateWorkflowUseCase {
    fun execute(command: Command): Workflow

    data class Command(
        val workflowId: WorkflowId,
        val description: String? = null,
        val status: WorkflowStatus? = null,
        val updatedBy: String?
    )
}
