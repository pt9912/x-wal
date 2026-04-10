package com.xwal.domain.port.output

import com.xwal.domain.model.EngineAdapterId

/**
 * Cache fuer lebende WorkflowEnginePort-Instanzen (HTTP-Clients etc.).
 * MVP: InMemory (ConcurrentHashMap). Spaeter austauschbar durch Redis
 * (dann nur Config-Daten cachen, Instanzen bleiben lokal).
 */
interface AdapterInstanceCachePort {
    fun get(id: EngineAdapterId): WorkflowEnginePort?
    fun put(id: EngineAdapterId, adapter: WorkflowEnginePort)
    fun evict(id: EngineAdapterId)
    fun evictAll()
    fun getAllCached(): Map<EngineAdapterId, WorkflowEnginePort>
}
