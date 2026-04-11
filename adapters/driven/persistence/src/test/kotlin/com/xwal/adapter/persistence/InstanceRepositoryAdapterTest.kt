package com.xwal.adapter.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import com.xwal.adapter.persistence.adapter.InstanceRepositoryAdapter
import com.xwal.adapter.persistence.adapter.WorkflowRepositoryAdapter
import com.xwal.adapter.persistence.mapper.InstanceEntityMapper
import com.xwal.adapter.persistence.mapper.WorkflowEntityMapper
import com.xwal.adapter.persistence.repository.MnInstanceRepository
import com.xwal.adapter.persistence.repository.MnWorkflowRepository
import com.xwal.domain.model.*
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.*

@MicronautTest
class InstanceRepositoryAdapterTest {

    @Inject lateinit var mnWorkflowRepo: MnWorkflowRepository
    @Inject lateinit var mnInstanceRepo: MnInstanceRepository

    private val objectMapper = ObjectMapper().apply { findAndRegisterModules() }
    private val workflowMapper = WorkflowEntityMapper()
    private lateinit var instanceMapper: InstanceEntityMapper
    private lateinit var workflowAdapter: WorkflowRepositoryAdapter
    private lateinit var instanceAdapter: InstanceRepositoryAdapter
    private var workflowId: WorkflowId = WorkflowId.generate()

    @BeforeEach
    fun setup() {
        instanceMapper = InstanceEntityMapper(objectMapper)
        workflowAdapter = WorkflowRepositoryAdapter(mnWorkflowRepo, workflowMapper)
        instanceAdapter = InstanceRepositoryAdapter(mnInstanceRepo, instanceMapper)

        val wf = workflowAdapter.save(Workflow(
            WorkflowId.generate(), "inst-test-${System.nanoTime()}", "1.0", null,
            IwmDefinition("""{"workflow":{}}"""), WorkflowStatus.ACTIVE,
            Instant.now(), null, Instant.now(), null
        ))
        workflowId = wf.id
    }

    private fun instance(status: InstanceStatus = InstanceStatus.RUNNING, businessKey: String? = null) = WorkflowInstance(
        InstanceId.generate(), workflowId, null, null, status, businessKey, null, Instant.now(), null, null, Instant.now(), Instant.now()
    )

    @Test
    fun `save and findById`() {
        val saved = instanceAdapter.save(instance())
        val found = instanceAdapter.findById(saved.id)
        assertNotNull(found)
        assertEquals(saved.id, found.id)
    }

    @Test
    fun `findByWorkflowId`() {
        instanceAdapter.save(instance())
        instanceAdapter.save(instance())
        val found = instanceAdapter.findByWorkflowId(workflowId)
        assertTrue(found.size >= 2)
    }

    @Test
    fun `findByStatus`() {
        instanceAdapter.save(instance(InstanceStatus.SUSPENDED))
        val found = instanceAdapter.findByStatus(InstanceStatus.SUSPENDED)
        assertTrue(found.isNotEmpty())
    }

    @Test
    fun `findByBusinessKey`() {
        instanceAdapter.save(instance(businessKey = "BK-TEST-123"))
        val found = instanceAdapter.findByBusinessKey("BK-TEST-123")
        assertTrue(found.isNotEmpty())
        assertEquals("BK-TEST-123", found.first().businessKey)
    }

    @Test
    fun `update changes status`() {
        val saved = instanceAdapter.save(instance())
        val updated = instanceAdapter.update(saved.copy(status = InstanceStatus.COMPLETED, updatedAt = Instant.now()))
        assertEquals(InstanceStatus.COMPLETED, instanceAdapter.findById(updated.id)?.status)
    }

    @Test
    fun `countByStatus`() {
        instanceAdapter.save(instance(InstanceStatus.RUNNING))
        assertTrue(instanceAdapter.countByStatus(InstanceStatus.RUNNING) > 0)
    }
}
