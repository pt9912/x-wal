package com.xwal.adapter.engine.cache

import com.xwal.domain.model.EngineAdapterId
import com.xwal.domain.port.output.AdapterInstanceCachePort
import com.xwal.domain.port.output.WorkflowEnginePort
import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

@Singleton
class InMemoryAdapterCache : AdapterInstanceCachePort {

    private val cache = ConcurrentHashMap<EngineAdapterId, WorkflowEnginePort>()

    override fun get(id: EngineAdapterId): WorkflowEnginePort? = cache[id]

    override fun put(id: EngineAdapterId, adapter: WorkflowEnginePort) {
        cache[id] = adapter
    }

    override fun evict(id: EngineAdapterId) {
        cache.remove(id)
    }

    override fun evictAll() {
        cache.clear()
    }

    override fun getAllCached(): Map<EngineAdapterId, WorkflowEnginePort> =
        cache.toMap()
}
