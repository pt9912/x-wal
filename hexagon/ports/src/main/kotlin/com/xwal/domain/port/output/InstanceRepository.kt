package com.xwal.domain.port.output

import com.xwal.domain.model.EngineAdapterId
import com.xwal.domain.model.InstanceId
import com.xwal.domain.model.InstanceStatus
import com.xwal.domain.model.WorkflowId
import com.xwal.domain.model.WorkflowInstance
import java.time.Instant

interface InstanceRepository {
    fun save(instance: WorkflowInstance): WorkflowInstance
    fun update(instance: WorkflowInstance): WorkflowInstance
    fun findById(id: InstanceId): WorkflowInstance?
    fun findByEngineInstanceId(engineInstanceId: String): WorkflowInstance?
    fun findByWorkflowId(workflowId: WorkflowId): List<WorkflowInstance>
    fun findByStatus(status: InstanceStatus): List<WorkflowInstance>
    fun findByEngineAdapterId(adapterId: EngineAdapterId): List<WorkflowInstance>
    fun findByBusinessKey(businessKey: String): List<WorkflowInstance>
    fun countByStatus(status: InstanceStatus): Long

    /** Keyset-Pagination fuer Instance-State-Sync */
    fun findForSync(
        updatedBefore: Instant,
        lastId: InstanceId?,
        limit: Int
    ): List<WorkflowInstance>
}
