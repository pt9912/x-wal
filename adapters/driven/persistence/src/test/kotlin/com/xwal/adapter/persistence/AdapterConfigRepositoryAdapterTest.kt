package com.xwal.adapter.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import com.xwal.adapter.persistence.adapter.AdapterConfigRepositoryAdapter
import com.xwal.adapter.persistence.mapper.AdapterEntityMapper
import com.xwal.adapter.persistence.repository.MnAdapterRepository
import com.xwal.domain.model.*
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.*

@MicronautTest
class AdapterConfigRepositoryAdapterTest {

    @Inject lateinit var mnRepo: MnAdapterRepository

    private val objectMapper = ObjectMapper().apply { findAndRegisterModules() }
    private val mapper = AdapterEntityMapper(objectMapper)
    private fun adapter() = AdapterConfigRepositoryAdapter(mnRepo, mapper)

    private fun config(name: String = "test-${System.nanoTime()}", type: EngineType = EngineType.CAMUNDA7) = EngineAdapterConfig(
        EngineAdapterId.generate(), name, type, mapOf("url" to "http://localhost:8080"),
        null, true, HealthStatus.UNKNOWN, null, 0, Instant.now(), null, Instant.now(), null
    )

    @Test
    fun `save and findById`() {
        val repo = adapter()
        val saved = repo.save(config())
        val found = repo.findById(saved.id)
        assertNotNull(found)
        assertEquals(saved.name, found.name)
    }

    @Test
    fun `findByName`() {
        val repo = adapter()
        val name = "unique-${System.nanoTime()}"
        repo.save(config(name))
        val found = repo.findByName(name)
        assertNotNull(found)
        assertEquals(name, found.name)
    }

    @Test
    fun `findByEngineType`() {
        val repo = adapter()
        repo.save(config(type = EngineType.FLOWABLE))
        val found = repo.findByEngineType(EngineType.FLOWABLE)
        assertTrue(found.isNotEmpty())
    }

    @Test
    fun `findByEnabled`() {
        val repo = adapter()
        repo.save(config())
        val enabled = repo.findByEnabled(true)
        assertTrue(enabled.isNotEmpty())
    }

    @Test
    fun `findByHealthStatus`() {
        val repo = adapter()
        repo.save(config())
        val unknown = repo.findByHealthStatus(HealthStatus.UNKNOWN)
        assertTrue(unknown.isNotEmpty())
    }

    @Test
    fun `update changes health status`() {
        val repo = adapter()
        val saved = repo.save(config())
        val updated = repo.update(saved.copy(healthStatus = HealthStatus.HEALTHY, updatedAt = Instant.now()))
        assertEquals(HealthStatus.HEALTHY, repo.findById(updated.id)?.healthStatus)
    }

    @Test
    fun `deleteById`() {
        val repo = adapter()
        val saved = repo.save(config())
        repo.deleteById(saved.id)
        assertNull(repo.findById(saved.id))
    }
}
