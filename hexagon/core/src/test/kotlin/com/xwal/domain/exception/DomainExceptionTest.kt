package com.xwal.domain.exception

import org.junit.jupiter.api.Test
import kotlin.test.*

class DomainExceptionTest {

    @Test
    fun `WorkflowNotFoundException contains workflowId`() {
        val ex = WorkflowNotFoundException("wf-123")
        assertEquals("wf-123", ex.workflowId)
        assertContains(ex.message, "wf-123")
    }

    @Test
    fun `InstanceNotFoundException contains instanceId`() {
        val ex = InstanceNotFoundException("inst-456")
        assertEquals("inst-456", ex.instanceId)
    }

    @Test
    fun `WorkflowValidationException contains errors`() {
        val ex = WorkflowValidationException("invalid", listOf("field1 missing", "field2 bad"))
        assertEquals(2, ex.errors.size)
        assertEquals("invalid", ex.message)
    }

    @Test
    fun `AdapterNotFoundException contains adapterId`() {
        val ex = AdapterNotFoundException("adapter-789")
        assertContains(ex.message, "adapter-789")
    }

    @Test
    fun `AdapterUnavailableException with cause`() {
        val cause = RuntimeException("connection refused")
        val ex = AdapterUnavailableException("a1", cause = cause)
        assertEquals(cause, ex.cause)
    }

    @Test
    fun `InvalidStateTransitionException details`() {
        val ex = InvalidStateTransitionException("RUNNING", "COMPLETED", "inst-1")
        assertContains(ex.message, "RUNNING")
        assertContains(ex.message, "COMPLETED")
        assertContains(ex.message, "inst-1")
    }

    @Test
    fun `AdapterOperationException details`() {
        val ex = AdapterOperationException("camunda7", "deploy", "timeout")
        assertEquals("camunda7", ex.engineType)
        assertEquals("deploy", ex.operation)
    }

    @Test
    fun `DuplicateWorkflowException details`() {
        val ex = DuplicateWorkflowException("myflow", "1.0")
        assertEquals("myflow", ex.name)
        assertEquals("1.0", ex.version)
        assertContains(ex.message, "myflow")
    }

    @Test
    fun `sealed class hierarchy`() {
        val exceptions: List<DomainException> = listOf(
            WorkflowNotFoundException("1"),
            InstanceNotFoundException("2"),
            WorkflowValidationException("3"),
            AdapterNotFoundException("4"),
            AdapterUnavailableException("5"),
            InvalidStateTransitionException("A", "B", "C"),
            AdapterOperationException("e", "o", "m"),
            DuplicateWorkflowException("n", "v")
        )
        assertEquals(8, exceptions.size)
        assertTrue(exceptions.all { it is RuntimeException })
    }
}
