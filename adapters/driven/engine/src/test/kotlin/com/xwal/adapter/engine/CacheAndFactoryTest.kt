package com.xwal.adapter.engine

import com.xwal.adapter.engine.cache.InMemoryAdapterCache
import com.xwal.adapter.engine.config.HttpClientFactory
import com.xwal.adapter.engine.config.HttpClientPoolConfig
import com.xwal.adapter.engine.factory.EngineAdapterFactoryImpl
import com.xwal.domain.exception.AdapterOperationException
import com.xwal.domain.model.EngineAdapterId
import com.xwal.domain.model.EngineType
import org.junit.jupiter.api.Test
import kotlin.test.*

class CacheAndFactoryTest {

    // ========== InMemoryAdapterCache ==========
    @Test fun `cache starts empty`() { assertEquals(0, InMemoryAdapterCache().getAllCached().size) }
    @Test fun `cache put and get`() { val c = InMemoryAdapterCache(); val id = EngineAdapterId.generate(); val p = mockPort(); c.put(id, p); assertSame(p, c.get(id)) }
    @Test fun `cache evict`() { val c = InMemoryAdapterCache(); val id = EngineAdapterId.generate(); c.put(id, mockPort()); c.evict(id); assertNull(c.get(id)) }
    @Test fun `cache evictAll`() { val c = InMemoryAdapterCache(); c.put(EngineAdapterId.generate(), mockPort()); c.put(EngineAdapterId.generate(), mockPort()); c.evictAll(); assertEquals(0, c.getAllCached().size) }
    @Test fun `cache get nonexistent returns null`() { assertNull(InMemoryAdapterCache().get(EngineAdapterId.generate())) }
    @Test fun `cache getAllCached`() { val c = InMemoryAdapterCache(); c.put(EngineAdapterId.generate(), mockPort()); c.put(EngineAdapterId.generate(), mockPort()); assertEquals(2, c.getAllCached().size) }
    @Test fun `cache overwrite`() { val c = InMemoryAdapterCache(); val id = EngineAdapterId.generate(); val p1 = mockPort(); val p2 = mockPort(); c.put(id, p1); c.put(id, p2); assertSame(p2, c.get(id)) }

    // ========== EngineAdapterFactoryImpl ==========
    @Test fun `factory supports camunda7`() { assertTrue(EngineAdapterFactoryImpl().supportsEngineType(EngineType.CAMUNDA7)) }
    @Test fun `factory supports flowable`() { assertTrue(EngineAdapterFactoryImpl().supportsEngineType(EngineType.FLOWABLE)) }
    @Test fun `factory does not support temporal`() { assertFalse(EngineAdapterFactoryImpl().supportsEngineType(EngineType.TEMPORAL)) }
    @Test fun `factory does not support conductor`() { assertFalse(EngineAdapterFactoryImpl().supportsEngineType(EngineType.CONDUCTOR)) }
    @Test fun `factory supported types`() { assertEquals(setOf(EngineType.CAMUNDA7, EngineType.FLOWABLE), EngineAdapterFactoryImpl().getSupportedEngineTypes()) }
    @Test fun `factory rejects missing url`() { assertFailsWith<AdapterOperationException> { EngineAdapterFactoryImpl().createAdapter(EngineType.CAMUNDA7, emptyMap()) } }
    @Test fun `factory rejects unsupported type`() { assertFailsWith<AdapterOperationException> { EngineAdapterFactoryImpl().createAdapter(EngineType.TEMPORAL, mapOf("url" to "http://x")) } }
    @Test fun `factory rejects camunda8`() { assertFailsWith<AdapterOperationException> { EngineAdapterFactoryImpl().createAdapter(EngineType.CAMUNDA8, mapOf("url" to "http://x")) } }

    // ========== HttpClientPoolConfig ==========
    @Test fun `pool config defaults`() {
        val config = HttpClientPoolConfig()
        assertEquals(10, config.maxConnectionsPerHost)
        assertEquals(50, config.maxConnections)
        assertEquals("10s", config.connectionTimeout)
        assertEquals("30s", config.readTimeout)
    }

    private fun mockPort() = io.mockk.mockk<com.xwal.domain.port.output.WorkflowEnginePort>(relaxed = true)
}
