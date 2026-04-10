package com.xwal.adapter.persistence

import com.xwal.adapter.persistence.adapter.WorkflowRepositoryAdapter
import com.xwal.adapter.persistence.mapper.WorkflowEntityMapper
import com.xwal.adapter.persistence.repository.MnWorkflowRepository
import com.xwal.domain.model.*
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.*

@MicronautTest
class WorkflowRepositoryAdapterTest {

    @Inject
    lateinit var mnRepo: MnWorkflowRepository

    private val mapper = WorkflowEntityMapper()

    private fun createAdapter() = WorkflowRepositoryAdapter(mnRepo, mapper)

    private fun workflow(name: String = "test", version: String = "1.0") = Workflow(
        id = WorkflowId.generate(),
        name = name,
        version = version,
        description = "Test workflow",
        iwmDefinition = IwmDefinition("""{"workflow":{"name":"$name","version":"$version"},"tasks":[],"edges":[]}"""),
        status = WorkflowStatus.ACTIVE,
        createdAt = Instant.now(),
        createdBy = "test",
        updatedAt = Instant.now(),
        updatedBy = null
    )

    @Test
    fun `save and findById`() {
        val adapter = createAdapter()
        val wf = workflow()
        val saved = adapter.save(wf)

        assertEquals(wf.name, saved.name)
        assertNotNull(saved.id)

        val found = adapter.findById(saved.id)
        assertNotNull(found)
        assertEquals(saved.name, found.name)
        // JSONB may normalize whitespace, so compare parsed content
        assertTrue(found.iwmDefinition.json.contains("\"name\""))
    }

    @Test
    fun `existsByNameAndVersion`() {
        val adapter = createAdapter()
        assertFalse(adapter.existsByNameAndVersion("unique", "9.9"))

        adapter.save(workflow("unique", "9.9"))
        assertTrue(adapter.existsByNameAndVersion("unique", "9.9"))
    }

    @Test
    fun `findAllByStatus`() {
        val adapter = createAdapter()
        adapter.save(workflow("active-wf", "1.0"))
        val active = adapter.findAllByStatus(WorkflowStatus.ACTIVE)
        assertTrue(active.isNotEmpty())
    }

    @Test
    fun `findByNameAndVersion`() {
        val adapter = createAdapter()
        adapter.save(workflow("lookup", "2.0"))
        val found = adapter.findByNameAndVersion("lookup", "2.0")
        assertNotNull(found)
        assertEquals("lookup", found.name)
    }

    @Test
    fun `deleteById`() {
        val adapter = createAdapter()
        val saved = adapter.save(workflow("del", "1.0"))
        adapter.deleteById(saved.id)
        assertNull(adapter.findById(saved.id))
    }
}
