package com.xwal.domain.model

import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DomainModelTest {

    @Test
    fun `WorkflowId generates unique IDs`() {
        val id1 = WorkflowId.generate()
        val id2 = WorkflowId.generate()
        assertTrue(id1 != id2)
    }

    @Test
    fun `IwmDefinition rejects blank content`() {
        assertFailsWith<IllegalArgumentException> {
            IwmDefinition("")
        }
        assertFailsWith<IllegalArgumentException> {
            IwmDefinition("   ")
        }
    }

    @Test
    fun `TaskId composite format`() {
        val taskId = TaskId.of(EngineType.CAMUNDA7, "task-123")
        assertEquals("CAMUNDA7:task-123", taskId.value)
        assertEquals(EngineType.CAMUNDA7, taskId.engineType)
        assertEquals("task-123", taskId.engineTaskId)
    }

    @Test
    fun `TaskId supports adapter-scoped format`() {
        val adapterId = EngineAdapterId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
        val taskId = TaskId.of(EngineType.CAMUNDA7, adapterId, "task-123")

        assertEquals("CAMUNDA7:${adapterId.value}:task-123", taskId.value)
        assertEquals(EngineType.CAMUNDA7, taskId.engineType)
        assertEquals(adapterId, taskId.adapterId)
        assertEquals("task-123", taskId.engineTaskId)
    }

    @Test
    fun `TaskId accepts legacy task id with colon`() {
        val taskId = TaskId("CAMUNDA7:tenant:abc:123")
        assertEquals(EngineType.CAMUNDA7, taskId.engineType)
        assertEquals(null, taskId.adapterId)
        assertEquals("tenant:abc:123", taskId.engineTaskId)
    }

    @Test
    fun `TaskId rejects malformed values`() {
        assertFailsWith<IllegalArgumentException> {
            TaskId("no-colon")
        }
        assertFailsWith<IllegalArgumentException> {
            TaskId("INVALID_ENGINE:task-1")
        }
    }

    @Test
    fun `Workflow data class creation`() {
        val now = Instant.now()
        val workflow = Workflow(
            id = WorkflowId.generate(),
            name = "urlaubsantrag",
            version = "1.0.0",
            description = "Urlaubsantrag Workflow",
            iwmDefinition = IwmDefinition("""{"workflow":{"name":"test"}}"""),
            status = WorkflowStatus.ACTIVE,
            createdAt = now,
            createdBy = "admin",
            updatedAt = now,
            updatedBy = null
        )
        assertEquals("urlaubsantrag", workflow.name)
        assertEquals(WorkflowStatus.ACTIVE, workflow.status)
    }

    @Test
    fun `WorkflowInstance data class creation`() {
        val instance = WorkflowInstance(
            id = InstanceId.generate(),
            workflowId = WorkflowId.generate(),
            engineInstanceId = "camunda-instance-1",
            engineAdapterId = EngineAdapterId.generate(),
            status = InstanceStatus.RUNNING,
            businessKey = "REQ-2026-001",
            variables = mapOf("employee" to "alice"),
            startedAt = Instant.now(),
            startedBy = "alice",
            endedAt = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        assertEquals(InstanceStatus.RUNNING, instance.status)
        assertEquals("REQ-2026-001", instance.businessKey)
    }

    @Test
    fun `EngineType has display names`() {
        assertEquals("Camunda 7", EngineType.CAMUNDA7.displayName)
        assertEquals("Camunda 8 / Zeebe", EngineType.CAMUNDA8.displayName)
        assertEquals("Flowable", EngineType.FLOWABLE.displayName)
    }

    @Test
    fun `AdapterCapabilities BPMN factory`() {
        val caps = AdapterCapabilities.createBpmnCapabilities()
        assertTrue(caps.supportsUserTasks)
        assertTrue(caps.supportsServiceTasks)
        assertTrue(caps.supportsParallelGateways)
        assertTrue(!caps.supportsDMN)
        assertTrue(!caps.supportsCMMN)
    }

    @Test
    fun `AdapterCapabilities hasAllCapabilities check`() {
        val full = AdapterCapabilities.createBpmnCapabilities()
        val required = AdapterCapabilities(supportsUserTasks = true, supportsServiceTasks = true)
        assertTrue(full.hasAllCapabilities(required))

        val requireDmn = AdapterCapabilities(supportsDMN = true)
        assertTrue(!full.hasAllCapabilities(requireDmn))
    }

    @Test
    fun `EngineInstanceStatus status enum`() {
        assertEquals("Running", EngineInstanceStatus.Status.RUNNING.displayName)
        assertEquals("Failed", EngineInstanceStatus.Status.FAILED.displayName)
    }

    @Test
    fun `EngineAdapterConfig data class creation`() {
        val adapter = EngineAdapterConfig(
            id = EngineAdapterId.generate(),
            name = "local-camunda",
            engineType = EngineType.CAMUNDA7,
            config = mapOf("url" to "http://localhost:8080/engine-rest"),
            capabilities = AdapterCapabilities.createBpmnCapabilities(),
            enabled = true,
            healthStatus = HealthStatus.HEALTHY,
            lastHealthCheck = Instant.now(),
            priority = 0,
            createdAt = Instant.now(),
            createdBy = "admin",
            updatedAt = Instant.now(),
            updatedBy = null
        )
        assertEquals(EngineType.CAMUNDA7, adapter.engineType)
        assertTrue(adapter.enabled)
        assertEquals("http://localhost:8080/engine-rest", adapter.endpointUrl)
    }

    @Test
    fun `CANCELLED spelling is consistent`() {
        // Verify all cancelled-related enums use double-L
        assertEquals("CANCELLED", InstanceStatus.CANCELLED.name)
        assertEquals("CANCELLED", EngineInstanceStatus.Status.CANCELLED.name)
        assertEquals("CANCELLED", TaskStatus.CANCELLED.name)
    }
}
