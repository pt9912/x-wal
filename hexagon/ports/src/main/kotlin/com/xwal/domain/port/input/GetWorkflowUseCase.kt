package com.xwal.domain.port.input

import com.xwal.domain.model.Workflow
import com.xwal.domain.model.WorkflowId

interface GetWorkflowUseCase {
    fun execute(workflowId: WorkflowId): Workflow
}
