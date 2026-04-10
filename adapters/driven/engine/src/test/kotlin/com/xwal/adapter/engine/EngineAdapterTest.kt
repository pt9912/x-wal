package com.xwal.adapter.engine

import com.xwal.adapter.engine.cache.InMemoryAdapterCache
import com.xwal.adapter.engine.camunda7.transformer.IwmToBpmnTransformer
import com.xwal.adapter.engine.factory.EngineAdapterFactoryImpl
import com.xwal.adapter.engine.resilience.Resilience4jConfig
import com.xwal.domain.exception.AdapterOperationException
import com.xwal.domain.model.EngineAdapterId
import com.xwal.domain.model.EngineType
import org.junit.jupiter.api.Test
import kotlin.test.*

class EngineAdapterTest {

    // ========== InMemoryAdapterCache ==========

    @Test
    fun `cache put and get`() {
        val cache = InMemoryAdapterCache()
        val id = EngineAdapterId.generate()
        assertNull(cache.get(id))

        val mockAdapter = object : com.xwal.domain.port.output.WorkflowEnginePort {
            override val engineName = "test"
            override val engineVersion = "1.0"
            override val engineType = EngineType.CAMUNDA7
            override fun deployWorkflow(workflowDefinition: String, name: String, version: String) = ""
            override fun exportNativeWorkflow(processDefinitionId: String) = ""
            override fun exportWorkflow(processDefinitionId: String) = ""
            override fun startInstance(workflowKey: String, businessKey: String?, variables: Map<String, Any>) = ""
            override fun getInstanceStatus(instanceId: String) = throw NotImplementedError()
            override fun suspendInstance(instanceId: String) {}
            override fun resumeInstance(instanceId: String) {}
            override fun cancelInstance(instanceId: String, reason: String?) {}
            override fun getInstanceVariables(instanceId: String) = emptyMap<String, Any>()
            override fun getHistoricInstance(instanceId: String) = throw NotImplementedError()
            override fun getActiveIncidents(instanceId: String) = emptyList<com.xwal.domain.model.Incident>()
            override fun getTask(taskId: String) = throw NotImplementedError()
            override fun queryTasks(filter: com.xwal.domain.model.TaskFilter) = emptyList<com.xwal.domain.model.Task>()
            override fun completeTask(taskId: String, variables: Map<String, Any>) {}
            override fun assignTask(taskId: String, assignee: String) {}
            override fun getCapabilities() = com.xwal.domain.model.AdapterCapabilities()
            override fun checkHealth() = true
            override fun getHealthDetails() = emptyMap<String, Any>()
        }

        cache.put(id, mockAdapter)
        assertNotNull(cache.get(id))
        assertEquals(1, cache.getAllCached().size)

        cache.evict(id)
        assertNull(cache.get(id))
    }

    @Test
    fun `cache evictAll`() {
        val cache = InMemoryAdapterCache()
        cache.put(EngineAdapterId.generate(), object : com.xwal.domain.port.output.WorkflowEnginePort {
            override val engineName = "t"; override val engineVersion = "1"; override val engineType = EngineType.CAMUNDA7
            override fun deployWorkflow(d: String, n: String, v: String) = ""; override fun exportNativeWorkflow(p: String) = ""; override fun exportWorkflow(p: String) = ""
            override fun startInstance(w: String, b: String?, v: Map<String, Any>) = ""; override fun getInstanceStatus(i: String) = throw NotImplementedError()
            override fun suspendInstance(i: String) {}; override fun resumeInstance(i: String) {}; override fun cancelInstance(i: String, r: String?) {}
            override fun getInstanceVariables(i: String) = emptyMap<String, Any>(); override fun getHistoricInstance(i: String) = throw NotImplementedError()
            override fun getActiveIncidents(i: String) = emptyList<com.xwal.domain.model.Incident>()
            override fun getTask(t: String) = throw NotImplementedError(); override fun queryTasks(f: com.xwal.domain.model.TaskFilter) = emptyList<com.xwal.domain.model.Task>()
            override fun completeTask(t: String, v: Map<String, Any>) {}; override fun assignTask(t: String, a: String) {}
            override fun getCapabilities() = com.xwal.domain.model.AdapterCapabilities(); override fun checkHealth() = true; override fun getHealthDetails() = emptyMap<String, Any>()
        })
        assertEquals(1, cache.getAllCached().size)
        cache.evictAll()
        assertEquals(0, cache.getAllCached().size)
    }

    // ========== Factory ==========

    @Test
    fun `factory supports camunda7 and flowable`() {
        val factory = EngineAdapterFactoryImpl()
        assertTrue(factory.supportsEngineType(EngineType.CAMUNDA7))
        assertTrue(factory.supportsEngineType(EngineType.FLOWABLE))
        assertFalse(factory.supportsEngineType(EngineType.TEMPORAL))
        assertEquals(setOf(EngineType.CAMUNDA7, EngineType.FLOWABLE), factory.getSupportedEngineTypes())
    }

    @Test
    fun `factory rejects missing url`() {
        val factory = EngineAdapterFactoryImpl()
        assertFailsWith<AdapterOperationException> {
            factory.createAdapter(EngineType.CAMUNDA7, emptyMap())
        }
    }

    @Test
    fun `factory rejects unsupported engine type`() {
        val factory = EngineAdapterFactoryImpl()
        assertFailsWith<AdapterOperationException> {
            factory.createAdapter(EngineType.TEMPORAL, mapOf("url" to "http://localhost:8080"))
        }
    }

    // ========== Transformer ==========

    @Test
    fun `Camunda7 transformer generates valid BPMN`() {
        val transformer = IwmToBpmnTransformer()
        val iwm = """
        {
            "workflow": {"name": "test", "version": "1.0"},
            "tasks": [
                {"id": "start", "type": "activity", "operation": "startProcess", "label": "Start"},
                {"id": "task1", "type": "userTask", "label": "Approve"},
                {"id": "end", "type": "activity", "operation": "endProcess", "label": "End"}
            ],
            "edges": [
                {"from": "start", "to": "task1"},
                {"from": "task1", "to": "end"}
            ]
        }
        """.trimIndent()

        val bpmn = transformer.transformToBpmn(iwm, "test-process", "1.0.0")

        assertTrue(bpmn.contains("<bpmn:startEvent"))
        assertTrue(bpmn.contains("<bpmn:userTask"))
        assertTrue(bpmn.contains("<bpmn:endEvent"))
        assertTrue(bpmn.contains("camunda:versionTag"))
        assertTrue(bpmn.contains("Flow_start_task1"))
    }

    // ========== Resilience ==========

    @Test
    fun `Resilience4jConfig creates circuit breaker and retry`() {
        val config = Resilience4jConfig()
        val cb = config.getCircuitBreaker("test-adapter")
        assertNotNull(cb)
        assertEquals("test-adapter", cb.name)

        val retry = config.getRetry("test-adapter")
        assertNotNull(retry)
        assertEquals("test-adapter", retry.name)
    }
}
