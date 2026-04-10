package com.xwal.domain.port.input

import com.xwal.domain.model.Workflow

interface CreateWorkflowUseCase {
    fun execute(command: Command): Workflow

    data class Command(
        val name: String,
        val version: String,
        val description: String?,
        val iwmDefinition: String,
        val createdBy: String?
    )
}
