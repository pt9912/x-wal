package com.xwal.application.usecase.workflow

import com.xwal.domain.exception.WorkflowNotFoundException
import com.xwal.domain.model.*
import com.xwal.domain.port.output.WorkflowRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetWorkflowUseCaseTest {

    private val workflowRepository = mockk<WorkflowRepository>()
    private val useCase = GetWorkflowUseCaseImpl(workflowRepository)

    private val workflowId = WorkflowId.generate()
    private val now = Instant.now()

    private val workflow = Workflow(
        id = workflowId, name = "test", version = "1.0",
        description = null, iwmDefinition = IwmDefinition("""{"workflow":{}}"""),
        status = WorkflowStatus.ACTIVE, createdAt = now, createdBy = null,
        updatedAt = now, updatedBy = null
    )

    @Test
    fun `returns workflow when found`() {
        every { workflowRepository.findById(workflowId) } returns workflow
        assertEquals(workflow, useCase.execute(workflowId))
    }

    @Test
    fun `throws when not found`() {
        every { workflowRepository.findById(workflowId) } returns null
        assertFailsWith<WorkflowNotFoundException> { useCase.execute(workflowId) }
    }
}
