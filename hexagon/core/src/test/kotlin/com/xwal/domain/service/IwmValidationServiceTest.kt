package com.xwal.domain.service

import com.xwal.domain.exception.WorkflowValidationException
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class IwmValidationServiceTest {

    private val service = IwmValidationService()

    private val validIwm = """
    {
        "workflow": {"name": "test", "version": "1.0.0"},
        "tasks": [{"id": "start", "type": "activity", "label": "Start"}],
        "edges": [{"from": "start", "to": "start"}]
    }
    """.trimIndent()

    @Test
    fun `validate returns success for valid IWM`() {
        val result = service.validate(validIwm)
        assertTrue(result.valid)
    }

    @Test
    fun `validate returns failure for invalid IWM`() {
        val result = service.validate("""{"invalid": true}""")
        assertTrue(!result.valid)
    }

    @Test
    fun `validateOrThrow succeeds for valid IWM`() {
        service.validateOrThrow(validIwm) // should not throw
    }

    @Test
    fun `validateOrThrow throws for invalid IWM`() {
        assertFailsWith<WorkflowValidationException> {
            service.validateOrThrow("""{"no": "workflow"}""")
        }
    }
}
