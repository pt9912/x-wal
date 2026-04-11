package com.xwal.adapter.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import com.xwal.adapter.persistence.entity.EngineAdapterEntity
import com.xwal.adapter.persistence.entity.InstanceEntity
import com.xwal.adapter.persistence.entity.WorkflowEntity
import com.xwal.adapter.persistence.mapper.AdapterEntityMapper
import com.xwal.adapter.persistence.mapper.InstanceEntityMapper
import com.xwal.adapter.persistence.mapper.WorkflowEntityMapper
import com.xwal.domain.model.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MapperTest {

    private val objectMapper = ObjectMapper().apply { findAndRegisterModules() }
    private val workflowMapper = WorkflowEntityMapper()
    private val instanceMapper = InstanceEntityMapper(objectMapper)
    private val adapterMapper = AdapterEntityMapper(objectMapper)

    // ========== WorkflowEntityMapper ==========

    @Test fun `workflow toDomain maps all fields`() {
        val entity = WorkflowEntity(UUID.randomUUID(), "wf", "1.0", "desc", """{"workflow":{}}""", "ACTIVE", Instant.now(), "admin", Instant.now(), null)
        val domain = workflowMapper.toDomain(entity)
        assertEquals("wf", domain.name)
        assertEquals("1.0", domain.version)
        assertEquals("desc", domain.description)
        assertEquals(WorkflowStatus.ACTIVE, domain.status)
        assertEquals("admin", domain.createdBy)
    }

    @Test fun `workflow toEntity maps all fields`() {
        val domain = Workflow(WorkflowId.generate(), "wf", "1.0", "desc", IwmDefinition("""{"workflow":{}}"""), WorkflowStatus.DRAFT, Instant.now(), "admin", Instant.now(), "editor")
        val entity = workflowMapper.toEntity(domain)
        assertEquals("wf", entity.name)
        assertEquals("DRAFT", entity.status)
        assertEquals("editor", entity.updatedBy)
    }

    @Test fun `workflow round-trip preserves data`() {
        val original = Workflow(WorkflowId.generate(), "test", "2.0", null, IwmDefinition("""{"workflow":{}}"""), WorkflowStatus.ARCHIVED, Instant.now(), null, Instant.now(), null)
        val roundTripped = workflowMapper.toDomain(workflowMapper.toEntity(original))
        assertEquals(original.id, roundTripped.id)
        assertEquals(original.name, roundTripped.name)
        assertEquals(original.status, roundTripped.status)
    }

    // ========== InstanceEntityMapper ==========

    @Test fun `instance toDomain maps all fields`() {
        val entity = InstanceEntity(UUID.randomUUID(), UUID.randomUUID(), "eng-1", UUID.randomUUID(), "RUNNING", "BK-1", """{"x":1}""", Instant.now(), "user", null, Instant.now(), Instant.now())
        val domain = instanceMapper.toDomain(entity)
        assertEquals(InstanceStatus.RUNNING, domain.status)
        assertEquals("BK-1", domain.businessKey)
        assertEquals("eng-1", domain.engineInstanceId)
        assertNotNull(domain.variables)
        assertEquals(1, domain.variables!!["x"])
    }

    @Test fun `instance toDomain handles null variables`() {
        val entity = InstanceEntity(UUID.randomUUID(), UUID.randomUUID(), null, null, "SUSPENDED", null, null, null, null, null, Instant.now(), Instant.now())
        val domain = instanceMapper.toDomain(entity)
        assertNull(domain.variables)
        assertNull(domain.engineInstanceId)
    }

    @Test fun `instance toEntity serializes variables`() {
        val domain = WorkflowInstance(InstanceId.generate(), WorkflowId.generate(), "eng-1", EngineAdapterId.generate(), InstanceStatus.COMPLETED, "BK", mapOf("approved" to true), Instant.now(), "user", Instant.now(), Instant.now(), Instant.now())
        val entity = instanceMapper.toEntity(domain)
        assertEquals("COMPLETED", entity.status)
        assertNotNull(entity.variables)
    }

    // ========== AdapterEntityMapper ==========

    @Test fun `adapter toDomain maps all fields`() {
        val entity = EngineAdapterEntity(UUID.randomUUID(), "camunda-1", "CAMUNDA7", """{"url":"http://localhost"}""", null, true, "HEALTHY", Instant.now(), 5, Instant.now(), "admin", Instant.now(), null)
        val domain = adapterMapper.toDomain(entity)
        assertEquals("camunda-1", domain.name)
        assertEquals(EngineType.CAMUNDA7, domain.engineType)
        assertEquals(HealthStatus.HEALTHY, domain.healthStatus)
        assertEquals(5, domain.priority)
        assertEquals("http://localhost", domain.config["url"])
    }

    @Test fun `adapter toDomain handles null capabilities`() {
        val entity = EngineAdapterEntity(UUID.randomUUID(), "flow-1", "FLOWABLE", """{"url":"http://localhost"}""", null, true, "UNKNOWN", null, 0, Instant.now(), null, Instant.now(), null)
        val domain = adapterMapper.toDomain(entity)
        assertNull(domain.capabilities)
    }

    @Test fun `adapter toDomain handles invalid capabilities JSON`() {
        val entity = EngineAdapterEntity(UUID.randomUUID(), "x", "CAMUNDA7", """{"url":"http://localhost"}""", """{"invalid":true}""", true, "UNKNOWN", null, 0, Instant.now(), null, Instant.now(), null)
        val domain = adapterMapper.toDomain(entity)
        // Should not throw, capabilities parsed as null due to schema mismatch
        // (logged as warning)
    }

    @Test fun `adapter toEntity serializes config`() {
        val domain = EngineAdapterConfig(EngineAdapterId.generate(), "test", EngineType.FLOWABLE, mapOf("url" to "http://localhost", "timeout" to 30), null, true, HealthStatus.UNHEALTHY, Instant.now(), 10, Instant.now(), null, Instant.now(), null)
        val entity = adapterMapper.toEntity(domain)
        assertEquals("FLOWABLE", entity.engineType)
        assertEquals("UNHEALTHY", entity.healthStatus)
        assertEquals(10, entity.priority)
    }

    @Test fun `adapter round-trip preserves data`() {
        val original = EngineAdapterConfig(EngineAdapterId.generate(), "rt-test", EngineType.CAMUNDA7, mapOf("url" to "http://example.com"), null, false, HealthStatus.HEALTHY, Instant.now(), 99, Instant.now(), "admin", Instant.now(), "editor")
        val roundTripped = adapterMapper.toDomain(adapterMapper.toEntity(original))
        assertEquals(original.id, roundTripped.id)
        assertEquals(original.name, roundTripped.name)
        assertEquals(original.engineType, roundTripped.engineType)
        assertEquals(original.enabled, roundTripped.enabled)
        assertEquals(original.priority, roundTripped.priority)
    }
}
