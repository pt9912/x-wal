package com.xwal.application.usecase.workflow

import com.xwal.domain.exception.WorkflowNotFoundException
import com.xwal.domain.model.*
import com.xwal.domain.port.input.UpdateWorkflowUseCase
import com.xwal.domain.port.output.TransactionPort
import com.xwal.domain.port.output.WorkflowRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UpdateDeleteWorkflowUseCaseTest {

    private val workflowRepository = mockk<WorkflowRepository>()
    private val transactionPort = mockk<TransactionPort>()

    private val updateUseCase = UpdateWorkflowUseCaseImpl(workflowRepository, transactionPort)
    private val deleteUseCase = DeleteWorkflowUseCaseImpl(workflowRepository, transactionPort)

    private val workflowId = WorkflowId.generate()
    private val now = Instant.now()
    private val workflow = Workflow(workflowId, "test", "1.0", "old desc", IwmDefinition("""{"workflow":{}}"""), WorkflowStatus.ACTIVE, now, null, now, null)

    @BeforeEach
    fun setup() {
        every { transactionPort.executeInTransaction(any<() -> Any>()) } answers { firstArg<() -> Any>().invoke() }
    }

    @Test
    fun `updates description`() {
        every { workflowRepository.findById(workflowId) } returns workflow
        every { workflowRepository.update(any()) } answers { firstArg() }
        val result = updateUseCase.execute(UpdateWorkflowUseCase.Command(workflowId, description = "new desc", updatedBy = "admin"))
        assertEquals("new desc", result.description)
    }

    @Test
    fun `updates status`() {
        every { workflowRepository.findById(workflowId) } returns workflow
        every { workflowRepository.update(any()) } answers { firstArg() }
        val result = updateUseCase.execute(UpdateWorkflowUseCase.Command(workflowId, status = WorkflowStatus.DEPRECATED, updatedBy = "admin"))
        assertEquals(WorkflowStatus.DEPRECATED, result.status)
    }

    @Test
    fun `update throws when not found`() {
        every { workflowRepository.findById(workflowId) } returns null
        assertFailsWith<WorkflowNotFoundException> {
            updateUseCase.execute(UpdateWorkflowUseCase.Command(workflowId, updatedBy = null))
        }
    }

    @Test
    fun `deletes workflow`() {
        every { workflowRepository.findById(workflowId) } returns workflow
        every { workflowRepository.deleteById(workflowId) } just runs
        deleteUseCase.execute(workflowId)
        verify { workflowRepository.deleteById(workflowId) }
    }

    @Test
    fun `delete throws when not found`() {
        every { workflowRepository.findById(workflowId) } returns null
        assertFailsWith<WorkflowNotFoundException> { deleteUseCase.execute(workflowId) }
    }
}
