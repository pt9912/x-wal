package com.xwal.domain.port.output

import com.xwal.domain.model.AdapterCapabilities
import com.xwal.domain.model.EngineInstanceStatus
import com.xwal.domain.model.EngineType
import com.xwal.domain.model.HistoricInstanceInfo
import com.xwal.domain.model.Incident
import com.xwal.domain.model.Task
import com.xwal.domain.model.TaskFilter

interface WorkflowEnginePort {

    // Metadata
    val engineName: String
    val engineVersion: String
    val engineType: EngineType

    // Workflow Operations
    fun deployWorkflow(workflowDefinition: String, name: String, version: String): String
    fun exportNativeWorkflow(processDefinitionId: String): String
    fun exportWorkflow(processDefinitionId: String): String

    // Instance Operations
    fun startInstance(workflowKey: String, businessKey: String?, variables: Map<String, Any>): String
    fun getInstanceStatus(instanceId: String): EngineInstanceStatus
    fun suspendInstance(instanceId: String)
    fun resumeInstance(instanceId: String)
    fun cancelInstance(instanceId: String, reason: String?)
    fun getInstanceVariables(instanceId: String): Map<String, Any>

    // History & Incidents
    fun getHistoricInstance(instanceId: String): HistoricInstanceInfo
    fun getActiveIncidents(instanceId: String): List<Incident>

    // Task Operations
    fun getTask(taskId: String): Task
    fun queryTasks(filter: TaskFilter): List<Task>
    fun completeTask(taskId: String, variables: Map<String, Any>)
    fun assignTask(taskId: String, assignee: String)

    // Health & Capabilities
    fun getCapabilities(): AdapterCapabilities
    fun checkHealth(): Boolean
    fun getHealthDetails(): Map<String, Any>
}
