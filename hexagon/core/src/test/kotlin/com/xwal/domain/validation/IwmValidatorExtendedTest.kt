package com.xwal.domain.validation

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class IwmValidatorExtendedTest {

    private val validator = IwmValidator()

    @Test fun `validates minimal valid IWM`() {
        val iwm = """{"workflow":{"name":"t","version":"1.0"},"tasks":[{"id":"s","type":"activity","label":"S"}],"edges":[{"from":"s","to":"s"}]}"""
        assertTrue(validator.validate(iwm).valid)
    }


    @Test fun `rejects empty JSON object`() { assertFalse(validator.validate("{}").valid) }
    @Test fun `rejects empty string`() { assertFalse(validator.validate("").valid) }
    @Test fun `rejects null-like`() { assertFalse(validator.validate("null").valid) }
    @Test fun `rejects array`() { assertFalse(validator.validate("[]").valid) }

    @Test fun `rejects missing workflow name`() {
        assertFalse(validator.validate("""{"workflow":{"version":"1.0"},"tasks":[],"edges":[]}""").valid)
    }

    @Test fun `rejects missing workflow version`() {
        assertFalse(validator.validate("""{"workflow":{"name":"t"},"tasks":[],"edges":[]}""").valid)
    }

    @Test fun `validation result has source for success`() {
        val iwm = """{"workflow":{"name":"t","version":"1.0"},"tasks":[{"id":"s","type":"activity","label":"S"}],"edges":[{"from":"s","to":"s"}]}"""
        val result = validator.validate(iwm)
        assertTrue(result.valid)
        assertEquals("<string>", result.source)
    }

    @Test fun `validation result has errors for failure`() {
        val result = validator.validate("""{"invalid":true}""")
        assertFalse(result.valid)
        assertTrue(result.errors.isNotEmpty())
    }

    @Test fun `invalid JSON returns error`() {
        val result = validator.validate("{not valid json")
        assertFalse(result.valid)
        assertTrue(result.message.contains("JSON"))
    }
}
