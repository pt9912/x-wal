package com.xwal.domain.port.input

import com.xwal.domain.model.WorkflowId

interface DeleteWorkflowUseCase {
    fun execute(workflowId: WorkflowId)
}
