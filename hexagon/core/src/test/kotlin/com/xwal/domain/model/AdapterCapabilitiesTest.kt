package com.xwal.domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdapterCapabilitiesTest {

    @Test
    fun `BPMN capabilities have standard features`() {
        val caps = AdapterCapabilities.createBpmnCapabilities()
        assertTrue(caps.supportsUserTasks)
        assertTrue(caps.supportsServiceTasks)
        assertTrue(caps.supportsTimerEvents)
        assertTrue(caps.supportsMessageEvents)
        assertTrue(caps.supportsSignalEvents)
        assertTrue(caps.supportsParallelGateways)
        assertTrue(caps.supportsExclusiveGateways)
        assertTrue(caps.supportsMultiInstance)
        assertTrue(caps.supportsCallActivity)
        assertTrue(caps.supportsScriptTasks)
        assertTrue(caps.supportsExternalTasks)
        assertFalse(caps.supportsDMN)
        assertFalse(caps.supportsCMMN)
    }

    @Test
    fun `hasAllCapabilities passes when all required are present`() {
        val full = AdapterCapabilities.createBpmnCapabilities()
        val required = AdapterCapabilities(supportsUserTasks = true, supportsServiceTasks = true, supportsTimerEvents = true)
        assertTrue(full.hasAllCapabilities(required))
    }

    @Test
    fun `hasAllCapabilities fails when missing capability`() {
        val limited = AdapterCapabilities(supportsUserTasks = true)
        val required = AdapterCapabilities(supportsUserTasks = true, supportsDMN = true)
        assertFalse(limited.hasAllCapabilities(required))
    }

    @Test
    fun `hasAllCapabilities with empty required always passes`() {
        val any = AdapterCapabilities()
        assertTrue(any.hasAllCapabilities(AdapterCapabilities()))
    }

    @Test
    fun `default capabilities are all false`() {
        val empty = AdapterCapabilities()
        assertFalse(empty.supportsUserTasks)
        assertFalse(empty.supportsServiceTasks)
        assertFalse(empty.supportsDMN)
        assertTrue(empty.supportedDataTypes.isEmpty())
        assertTrue(empty.supportedScriptLanguages.isEmpty())
    }
}
