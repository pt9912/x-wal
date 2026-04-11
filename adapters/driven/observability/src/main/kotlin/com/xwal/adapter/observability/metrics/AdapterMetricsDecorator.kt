package com.xwal.adapter.observability.metrics

import com.xwal.domain.model.*
import com.xwal.domain.port.output.WorkflowEnginePort

/**
 * Decorator that wraps a WorkflowEnginePort with metrics collection.
 * Records call count, duration, and errors per operation.
 */
class AdapterMetricsDecorator(
    private val delegate: WorkflowEnginePort,
    private val metrics: AdapterMetrics
) : WorkflowEnginePort by delegate {

    override fun deployWorkflow(workflowDefinition: String, name: String, version: String): String =
        metered("deployWorkflow") { delegate.deployWorkflow(workflowDefinition, name, version) }

    override fun startInstance(workflowKey: String, businessKey: String?, variables: Map<String, Any>): String =
        metered("startInstance") { delegate.startInstance(workflowKey, businessKey, variables) }

    override fun getInstanceStatus(instanceId: String): EngineInstanceStatus =
        metered("getInstanceStatus") { delegate.getInstanceStatus(instanceId) }

    override fun suspendInstance(instanceId: String) =
        metered("suspendInstance") { delegate.suspendInstance(instanceId) }

    override fun resumeInstance(instanceId: String) =
        metered("resumeInstance") { delegate.resumeInstance(instanceId) }

    override fun cancelInstance(instanceId: String, reason: String?) =
        metered("cancelInstance") { delegate.cancelInstance(instanceId, reason) }

    override fun queryTasks(filter: TaskFilter): List<Task> =
        metered("queryTasks") { delegate.queryTasks(filter) }

    override fun completeTask(taskId: String, variables: Map<String, Any>) =
        metered("completeTask") { delegate.completeTask(taskId, variables) }

    override fun checkHealth(): Boolean =
        metered("checkHealth") { delegate.checkHealth() }

    private fun <T> metered(operation: String, block: () -> T): T {
        val start = System.currentTimeMillis()
        var success = true
        return try {
            block()
        } catch (e: Exception) {
            success = false
            throw e
        } finally {
            metrics.recordCall(delegate.engineType.name, operation, System.currentTimeMillis() - start, success)
        }
    }
}
