package com.xwal.domain.port.input

import com.xwal.domain.model.WorkflowId
import com.xwal.domain.model.WorkflowInstance

interface StartWorkflowUseCase {
    fun execute(command: Command): WorkflowInstance

    data class Command(
        val workflowId: WorkflowId,
        val businessKey: String?,
        val variables: Map<String, Any> = emptyMap(),
        val startedBy: String?
    )
}
