package com.xwal.domain.validation

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IwmValidatorTest {

    private val validator = IwmValidator()

    private val validIwm = """
    {
        "workflow": {
            "name": "urlaubsantrag",
            "version": "1.0.0",
            "description": "Urlaubsantrag Test"
        },
        "tasks": [
            {"id": "start", "type": "activity", "label": "Start"},
            {"id": "end", "type": "activity", "label": "End"}
        ],
        "edges": [
            {"from": "start", "to": "end"}
        ]
    }
    """.trimIndent()

    @Test
    fun `validates correct IWM successfully`() {
        val result = validator.validate(validIwm)
        assertTrue(result.valid, "Expected valid but got: ${result.message}")
    }

    @Test
    fun `rejects missing workflow field`() {
        val json = """{"tasks":[],"edges":[]}"""
        val result = validator.validate(json)
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("workflow") })
    }

    @Test
    fun `rejects missing tasks field`() {
        val json = """{"workflow":{"name":"test","version":"1.0"},"edges":[]}"""
        val result = validator.validate(json)
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("tasks") })
    }

    @Test
    fun `rejects missing edges field`() {
        val json = """{"workflow":{"name":"test","version":"1.0"},"tasks":[]}"""
        val result = validator.validate(json)
        assertFalse(result.valid)
        assertTrue(result.errors.any { it.contains("edges") })
    }

    @Test
    fun `rejects missing workflow name`() {
        val json = """{"workflow":{"version":"1.0"},"tasks":[],"edges":[]}"""
        val result = validator.validate(json)
        assertFalse(result.valid)
    }

    @Test
    fun `rejects invalid JSON`() {
        val result = validator.validate("{invalid json")
        assertFalse(result.valid)
        assertTrue(result.message.contains("JSON"))
    }
}
