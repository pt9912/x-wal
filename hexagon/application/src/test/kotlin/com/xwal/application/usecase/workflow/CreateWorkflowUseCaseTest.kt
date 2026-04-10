package com.xwal.application.usecase.workflow

import com.xwal.domain.exception.DuplicateWorkflowException
import com.xwal.domain.exception.WorkflowValidationException
import com.xwal.domain.model.Workflow
import com.xwal.domain.model.WorkflowStatus
import com.xwal.domain.port.input.CreateWorkflowUseCase.Command
import com.xwal.domain.port.output.TransactionPort
import com.xwal.domain.port.output.WorkflowRepository
import com.xwal.domain.service.IwmValidationService
import com.xwal.domain.validation.IwmValidator
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CreateWorkflowUseCaseTest {

    private val workflowRepository = mockk<WorkflowRepository>()
    private val transactionPort = mockk<TransactionPort>()
    private val iwmValidationService = IwmValidationService(IwmValidator())

    private val useCase = CreateWorkflowUseCaseImpl(workflowRepository, transactionPort, iwmValidationService)

    private val validIwm = """{"workflow":{"name":"test","version":"1.0.0"},"tasks":[{"id":"s","type":"activity","label":"S"}],"edges":[{"from":"s","to":"s"}]}"""

    @BeforeEach
    fun setup() {
        every { transactionPort.executeInTransaction(any<() -> Workflow>()) } answers {
            firstArg<() -> Workflow>().invoke()
        }
    }

    @Test
    fun `creates workflow successfully`() {
        every { workflowRepository.existsByNameAndVersion("test", "1.0.0") } returns false
        every { workflowRepository.save(any()) } answers { firstArg() }

        val result = useCase.execute(Command("test", "1.0.0", "desc", validIwm, "admin"))

        assertEquals("test", result.name)
        assertEquals(WorkflowStatus.ACTIVE, result.status)
        verify { workflowRepository.save(any()) }
    }

    @Test
    fun `throws on duplicate workflow`() {
        every { workflowRepository.existsByNameAndVersion("test", "1.0.0") } returns true

        assertFailsWith<DuplicateWorkflowException> {
            useCase.execute(Command("test", "1.0.0", null, validIwm, null))
        }
    }

    @Test
    fun `throws on invalid IWM definition`() {
        assertFailsWith<WorkflowValidationException> {
            useCase.execute(Command("test", "1.0.0", null, """{"invalid":true}""", null))
        }
    }
}
