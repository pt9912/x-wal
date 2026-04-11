package com.xwal.domain.service

import com.xwal.domain.exception.WorkflowValidationException
import com.xwal.domain.validation.IwmValidator
import com.xwal.domain.validation.ValidationResult
import org.junit.jupiter.api.Test
import kotlin.test.*

class IwmValidationServiceExtendedTest {

    private val service = IwmValidationService(IwmValidator())

    private val validIwm = """{"workflow":{"name":"t","version":"1.0"},"tasks":[{"id":"s","type":"activity","label":"S"}],"edges":[{"from":"s","to":"s"}]}"""

    @Test fun `validate returns success`() { assertTrue(service.validate(validIwm).valid) }
    @Test fun `validate returns failure`() { assertFalse(service.validate("{}").valid) }
    @Test fun `validateOrThrow passes for valid`() { service.validateOrThrow(validIwm) }
    @Test fun `validateOrThrow throws for invalid`() {
        val ex = assertFailsWith<WorkflowValidationException> { service.validateOrThrow("{}") }
        assertTrue(ex.errors.isNotEmpty())
    }
    @Test fun `validateOrThrow for missing workflow`() {
        assertFailsWith<WorkflowValidationException> { service.validateOrThrow("""{"tasks":[],"edges":[]}""") }
    }
    @Test fun `validateOrThrow for missing tasks`() {
        assertFailsWith<WorkflowValidationException> { service.validateOrThrow("""{"workflow":{"name":"t","version":"1"},"edges":[]}""") }
    }
    @Test fun `validateOrThrow for missing edges`() {
        assertFailsWith<WorkflowValidationException> { service.validateOrThrow("""{"workflow":{"name":"t","version":"1"},"tasks":[]}""") }
    }
    @Test fun `validateOrThrow for invalid JSON`() {
        assertFailsWith<WorkflowValidationException> { service.validateOrThrow("{broken") }
    }

    @Test fun `validation result success factory`() {
        val r = ValidationResult.success("test.json")
        assertTrue(r.valid)
        assertEquals("test.json", r.source)
    }
    @Test fun `validation result failure factory`() {
        val r = ValidationResult.failure("test.json", listOf("err1", "err2"))
        assertFalse(r.valid)
        assertEquals(2, r.errors.size)
    }
    @Test fun `validation result error factory`() {
        val r = ValidationResult.error("IO error")
        assertFalse(r.valid)
        assertContains(r.message, "IO error")
    }
    @Test fun `validation result failure message factory`() {
        val r = ValidationResult.failure("generic failure")
        assertFalse(r.valid)
        assertEquals("generic failure", r.message)
    }
}
