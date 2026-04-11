package com.xwal.adapter.engine.cache

import com.xwal.domain.model.EngineAdapterId
import com.xwal.domain.port.output.AdapterInstanceCachePort
import com.xwal.domain.port.output.WorkflowEnginePort
import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

@Singleton
class InMemoryAdapterCache : AdapterInstanceCachePort {

    private val log = org.slf4j.LoggerFactory.getLogger(InMemoryAdapterCache::class.java)
    private val cache = ConcurrentHashMap<EngineAdapterId, WorkflowEnginePort>()

    override fun get(id: EngineAdapterId): WorkflowEnginePort? = cache[id]

    override fun put(id: EngineAdapterId, adapter: WorkflowEnginePort) {
        cache[id]?.let { previous ->
            try {
                (previous as? AutoCloseable)?.close()
            } catch (e: Exception) {
                log.warn("Error closing previous adapter instance {} during overwrite: {}", id, e.message)
            }
        }
        cache[id] = adapter
    }

    override fun evict(id: EngineAdapterId) {
        val removed = cache.remove(id)
        removed?.let { adapter ->
            try {
                (adapter as? AutoCloseable)?.close()
            } catch (e: Exception) {
                log.warn("Error closing adapter instance {} during evict: {}", id, e.message)
            }
        }
    }

    override fun evictAll() {
        cache.values.forEach { adapter ->
            try {
                (adapter as? AutoCloseable)?.close()
            } catch (e: Exception) {
                log.warn("Error closing adapter instance during evictAll: {}", e.message)
            }
        }
        cache.clear()
    }

    override fun getAllCached(): Map<EngineAdapterId, WorkflowEnginePort> =
        cache.toMap()
}
