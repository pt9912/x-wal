package com.xwal.domain.port.input

import com.xwal.domain.model.Workflow
import com.xwal.domain.model.WorkflowStatus

interface ListWorkflowsUseCase {
    fun execute(status: WorkflowStatus? = null): List<Workflow>
}
