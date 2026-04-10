package com.xwal.adapter.engine.flowable

import com.xwal.adapter.engine.flowable.client.FlowableRestClient
import com.xwal.adapter.engine.flowable.transformer.IwmToBpmnTransformer
import com.xwal.adapter.engine.resilience.AdapterResilientDecorator
import com.xwal.adapter.engine.resilience.Resilience4jConfig
import com.xwal.domain.exception.AdapterOperationException
import com.xwal.domain.model.*
import com.xwal.domain.port.output.WorkflowEnginePort
import io.micronaut.http.client.HttpClient
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.function.Supplier

class FlowableAdapter(
    httpClient: HttpClient,
    baseUrl: String,
    private val resilienceConfig: Resilience4jConfig = Resilience4jConfig()
) : WorkflowEnginePort {

    private val log = LoggerFactory.getLogger(FlowableAdapter::class.java)
    private val restClient = FlowableRestClient(httpClient, baseUrl)
    private val transformer = IwmToBpmnTransformer()

    override val engineName: String = "Flowable"
    override val engineVersion: String = "7.0.0"
    override val engineType: EngineType = EngineType.FLOWABLE

    override fun deployWorkflow(workflowDefinition: String, name: String, version: String): String {
        return AdapterResilientDecorator.execute("flowable", resilienceConfig, Supplier {
            val bpmnXml = transformer.transformToBpmn(workflowDefinition, name, version)
            restClient.deployProcess(bpmnXml, "$name-v$version", name)
        })
    }

    override fun exportNativeWorkflow(processDefinitionId: String): String =
        restClient.getProcessDefinitionXml(processDefinitionId)

    private val bpmnToIwmTransformer = com.xwal.adapter.engine.transformer.BpmnToIwmTransformer()

    override fun exportWorkflow(processDefinitionId: String): String {
        val bpmnXml = exportNativeWorkflow(processDefinitionId)
        return bpmnToIwmTransformer.transformToIwm(bpmnXml, "flowable")
    }

    override fun startInstance(workflowKey: String, businessKey: String?, variables: Map<String, Any>): String =
        restClient.startProcessInstance(workflowKey, businessKey, variables)

    override fun getInstanceStatus(instanceId: String): EngineInstanceStatus {
        return try {
            val variables = restClient.getProcessInstanceVariables(instanceId)
            EngineInstanceStatus(
                instanceId = instanceId, workflowKey = null, businessKey = null,
                status = EngineInstanceStatus.Status.RUNNING,
                startedAt = null, endedAt = null, variables = variables,
                currentActivityId = null, engineInstanceId = instanceId
            )
        } catch (e: AdapterOperationException) {
            // Only treat 404 (instance not found) as COMPLETED, re-throw other errors
            if (e.message?.contains("HTTP 404") == true || e.message?.contains("not found") == true) {
                EngineInstanceStatus(
                    instanceId = instanceId, workflowKey = null, businessKey = null,
                    status = EngineInstanceStatus.Status.COMPLETED,
                    startedAt = null, endedAt = null, currentActivityId = null, engineInstanceId = instanceId
                )
            } else throw e
        }
    }

    override fun suspendInstance(instanceId: String) = restClient.suspendProcessInstance(instanceId)
    override fun resumeInstance(instanceId: String) = restClient.activateProcessInstance(instanceId)
    override fun cancelInstance(instanceId: String, reason: String?) = restClient.deleteProcessInstance(instanceId, reason)
    override fun getInstanceVariables(instanceId: String): Map<String, Any> = restClient.getProcessInstanceVariables(instanceId)

    override fun getHistoricInstance(instanceId: String): HistoricInstanceInfo {
        val json = restClient.getHistoricProcessInstance(instanceId)
        return HistoricInstanceInfo(
            instanceId = json.path("id").asText(instanceId),
            processDefinitionId = json.path("processDefinitionId").asText(null),
            businessKey = json.path("businessKey").asText(null),
            startTime = json.path("startTime").asText(null)?.let { parseInstant(it) },
            endTime = json.path("endTime").asText(null)?.let { parseInstant(it) },
            durationInMillis = json.path("durationInMillis").asLong(0).takeIf { it > 0 },
            deleteReason = json.path("deleteReason").asText(null)
        )
    }

    override fun getActiveIncidents(instanceId: String): List<Incident> {
        val executions = restClient.getExecutions(instanceId)
        if (!executions.isArray) return emptyList()
        return executions.filter { it.path("suspensionState").asInt() == 2 }.map { node ->
            Incident(
                id = node.path("id").asText(""),
                processInstanceId = instanceId,
                executionId = node.path("id").asText(null),
                activityId = node.path("activityId").asText(null),
                type = IncidentType.FAILED_ACTIVITY,
                message = "Suspended execution",
                cause = null,
                createdTime = null
            )
        }
    }

    override fun getTask(taskId: String): Task {
        val taskNode = restClient.getTask(taskId)
        return convertToTask(taskNode)
    }

    override fun queryTasks(filter: TaskFilter): List<Task> {
        val tasksJson = restClient.queryTasks(filter.assignee, filter.candidateGroup, filter.processInstanceId)
        if (!tasksJson.isArray) return emptyList()
        return tasksJson.map { convertToTask(it) }
    }

    override fun completeTask(taskId: String, variables: Map<String, Any>) = restClient.completeTask(taskId, variables)
    override fun assignTask(taskId: String, assignee: String) = restClient.assignTask(taskId, assignee)

    override fun getCapabilities(): AdapterCapabilities = CAPABILITIES

    override fun checkHealth(): Boolean = restClient.checkHealth()

    override fun getHealthDetails(): Map<String, Any> = mapOf(
        "engineName" to engineName,
        "engineVersion" to engineVersion,
        "engineType" to engineType.name,
        "healthy" to checkHealth(),
        "timestamp" to Instant.now().toString()
    )

    private fun convertToTask(node: com.fasterxml.jackson.databind.JsonNode): Task = Task(
        taskId = TaskId.of(EngineType.FLOWABLE, node.path("id").asText("")),
        name = node.path("name").asText(null),
        type = node.path("taskDefinitionKey").asText(null),
        description = node.path("description").asText(null),
        assignee = node.path("assignee").asText(null),
        owner = node.path("owner").asText(null),
        processInstanceId = node.path("processInstanceId").asText(null),
        processDefinitionKey = node.path("processDefinitionId").asText(null),
        status = if (node.path("assignee").asText(null) != null) TaskStatus.ASSIGNED else TaskStatus.CREATED,
        createdAt = node.path("createTime").asText(null)?.let { parseInstant(it) },
        dueDate = node.path("dueDate").asText(null)?.let { parseInstant(it) },
        followUpDate = null,
        priority = node.path("priority").asInt(50)
    )

    private fun parseInstant(text: String): Instant? = try {
        Instant.parse(text)
    } catch (_: Exception) { null }

    companion object {
        private val CAPABILITIES = AdapterCapabilities(
            supportsUserTasks = true, supportsServiceTasks = true,
            supportsTimerEvents = true, supportsMessageEvents = true,
            supportsSignalEvents = true, supportsErrorEvents = true,
            supportsCompensation = true, supportsSubProcesses = true,
            supportsParallelGateways = true, supportsExclusiveGateways = true,
            supportsInclusiveGateways = true, supportsEventBasedGateways = true,
            supportsMultiInstance = true, supportsCallActivity = true,
            supportsScriptTasks = true, supportsExternalTasks = false,
            supportsDMN = true, supportsCMMN = true,
            supportedScriptLanguages = setOf("javascript", "groovy", "juel")
        )
    }
}
