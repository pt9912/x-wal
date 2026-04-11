package com.xwal.adapter.engine.camunda7

import com.xwal.adapter.engine.camunda7.client.Camunda7RestClient
import com.xwal.adapter.engine.camunda7.transformer.IwmToBpmnTransformer
import com.xwal.adapter.engine.resilience.AdapterResilientDecorator
import com.xwal.adapter.engine.resilience.Resilience4jConfig
import com.xwal.domain.exception.AdapterOperationException
import com.xwal.domain.model.*
import com.xwal.domain.port.output.WorkflowEnginePort
import io.micronaut.http.client.HttpClient
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.function.Supplier

class Camunda7Adapter(
    private val httpClient: HttpClient,
    baseUrl: String,
    private val resilienceConfig: Resilience4jConfig = Resilience4jConfig()
) : WorkflowEnginePort, AutoCloseable {

    private val log = LoggerFactory.getLogger(Camunda7Adapter::class.java)
    private val restClient = Camunda7RestClient(httpClient, baseUrl)
    private val transformer = IwmToBpmnTransformer()
    private var _engineVersion = "7.24.0"

    override val engineName: String = "Camunda 7"
    override val engineVersion: String get() = _engineVersion
    override val engineType: EngineType = EngineType.CAMUNDA7

    init {
        try {
            val version = restClient.getVersion()
            _engineVersion = version.path("version").asText(_engineVersion)
        } catch (e: Exception) {
            log.warn("Could not fetch Camunda 7 version: {}", e.message)
        }
    }

    override fun deployWorkflow(workflowDefinition: String, name: String, version: String): String {
        return AdapterResilientDecorator.execute("camunda7", resilienceConfig, Supplier {
            val bpmnXml = transformer.transformToBpmn(workflowDefinition, name, version)
            restClient.deployProcess(bpmnXml, "$name-v$version", name)
        })
    }

    override fun exportNativeWorkflow(processDefinitionId: String): String =
        restClient.getProcessDefinitionXml(processDefinitionId)

    override fun exportWorkflow(processDefinitionId: String): String {
        val bpmnXml = exportNativeWorkflow(processDefinitionId)
        return bpmnToIwmTransformer.transformToIwm(bpmnXml, "camunda7")
    }

    private val bpmnToIwmTransformer = com.xwal.adapter.engine.transformer.BpmnToIwmTransformer()

    override fun startInstance(workflowKey: String, businessKey: String?, variables: Map<String, Any>): String =
        restClient.startProcessInstance(workflowKey, businessKey, variables)

    override fun getInstanceStatus(instanceId: String): EngineInstanceStatus {
        val instance = restClient.getProcessInstance(instanceId)
        val variables = try { restClient.getProcessInstanceVariables(instanceId) } catch (_: Exception) { emptyMap() }
        val status = when {
            instance.path("suspended").asBoolean(false) -> EngineInstanceStatus.Status.SUSPENDED
            instance.path("ended").asBoolean(false) -> EngineInstanceStatus.Status.COMPLETED
            else -> EngineInstanceStatus.Status.RUNNING
        }
        return EngineInstanceStatus(
            instanceId = instanceId,
            workflowKey = instance.path("definitionId").asText(null),
            businessKey = instance.path("businessKey").asText(null),
            status = status,
            startedAt = null,
            endedAt = null,
            variables = variables,
            currentActivityId = null,
            engineInstanceId = instanceId
        )
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
        val json = restClient.getIncidents(instanceId)
        if (!json.isArray) return emptyList()
        return json.map { node ->
            Incident(
                id = node.path("id").asText(""),
                processInstanceId = node.path("processInstanceId").asText(null),
                executionId = node.path("executionId").asText(null),
                activityId = node.path("activityId").asText(null),
                type = mapIncidentType(node.path("incidentType").asText("")),
                message = node.path("incidentMessage").asText(null),
                cause = node.path("causeIncidentId").asText(null),
                createdTime = node.path("incidentTimestamp").asText(null)?.let { parseInstant(it) }
            )
        }
    }

    override fun getTask(taskId: String): Task {
        val taskNode = restClient.getTask(taskId)
        return convertToTask(taskNode)
    }

    override fun queryTasks(filter: TaskFilter): List<Task> {
        val tasks = restClient.queryTasks(filter.assignee, filter.processInstanceId, filter.limit)
        return tasks.map { convertToTask(it) }
    }

    override fun completeTask(taskId: String, variables: Map<String, Any>) = restClient.completeTask(taskId, variables)
    override fun assignTask(taskId: String, assignee: String) = restClient.assignTask(taskId, assignee)

    override fun getCapabilities(): AdapterCapabilities = CAPABILITIES

    override fun checkHealth(): Boolean = restClient.checkHealth()

    override fun getHealthDetails(): Map<String, Any> {
        val healthy = checkHealth()
        val version = try { restClient.getVersion().path("version").asText("N/A") } catch (_: Exception) { "N/A" }
        return mapOf(
            "engineName" to engineName,
            "engineVersion" to version,
            "engineType" to engineType.name,
            "healthy" to healthy,
                "timestamp" to Instant.now().toString()
        )
    }

    override fun close() {
        try {
            httpClient.close()
        } catch (e: Exception) {
            log.warn("Failed to close Camunda7 HttpClient: {}", e.message)
        }
    }

    // ========== Private Helpers ==========

    private fun convertToTask(node: com.fasterxml.jackson.databind.JsonNode): Task = Task(
        taskId = TaskId.of(EngineType.CAMUNDA7, node.path("id").asText("")),
        name = node.path("name").asText(null),
        type = node.path("taskDefinitionKey").asText(null),
        description = node.path("description").asText(null),
        assignee = node.path("assignee").asText(null),
        owner = node.path("owner").asText(null),
        processInstanceId = node.path("processInstanceId").asText(null),
        processDefinitionKey = node.path("processDefinitionId").asText(null),
        status = if (node.path("assignee").asText(null) != null) TaskStatus.ASSIGNED else TaskStatus.CREATED,
        createdAt = node.path("created").asText(null)?.let { parseInstant(it) },
        dueDate = node.path("due").asText(null)?.let { parseInstant(it) },
        followUpDate = node.path("followUp").asText(null)?.let { parseInstant(it) },
        priority = node.path("priority").asInt(50)
    )

    private fun mapIncidentType(camundaType: String): IncidentType = when (camundaType) {
        "failedJob" -> IncidentType.FAILED_JOB
        "failedExternalTask" -> IncidentType.FAILED_EXTERNAL_TASK
        else -> IncidentType.UNKNOWN
    }

    private fun parseInstant(text: String): Instant? = try {
        // Camunda returns timezone offsets like +0000, +0100 — Java expects +00:00, +01:00
        val fixed = text.replace(Regex("([+-])(\\d{2})(\\d{2})$"), "$1$2:$3")
        Instant.parse(fixed)
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
            supportsScriptTasks = true, supportsExternalTasks = true,
            supportsDMN = true, supportsCMMN = true,
            supportedScriptLanguages = setOf("groovy", "javascript", "python")
        )
    }
}
