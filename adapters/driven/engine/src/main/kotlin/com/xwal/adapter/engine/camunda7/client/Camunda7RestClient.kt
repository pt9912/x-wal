package com.xwal.adapter.engine.camunda7.client

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.xwal.domain.exception.AdapterOperationException
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.client.multipart.MultipartBody
import org.slf4j.LoggerFactory
import java.net.URLEncoder

class Camunda7RestClient(
    private val httpClient: HttpClient,
    baseUrl: String
) {
    private val log = LoggerFactory.getLogger(Camunda7RestClient::class.java)
    private val baseUrl = baseUrl.trimEnd('/')
    private val objectMapper = ObjectMapper()

    // ========== Deployment ==========

    fun deployProcess(bpmnXml: String, deploymentName: String, processName: String): String {
        val body = MultipartBody.builder()
            .addPart("deployment-name", deploymentName)
            .addPart("deployment-source", "x-wal")
            .addPart("$processName.bpmn", processName, MediaType.APPLICATION_XML_TYPE, bpmnXml.toByteArray())
            .build()

        val request = HttpRequest.POST("$baseUrl/deployment/create", body)
            .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)

        return executeRequest(request, "deploy") { response ->
            val json = objectMapper.readTree(response.body() as String)
            json.get("id")?.asText() ?: throw AdapterOperationException("camunda7", "deploy", "No deployment ID in response")
        }
    }

    // ========== Process Instance ==========

    fun startProcessInstance(processDefinitionKey: String, businessKey: String?, variables: Map<String, Any>): String {
        val body = objectMapper.createObjectNode().apply {
            businessKey?.let { put("businessKey", it) }
            set<ObjectNode>("variables", convertToVariableMap(variables))
        }

        val request = HttpRequest.POST("$baseUrl/process-definition/key/${encodePathSegment(processDefinitionKey)}/start", body.toString())
            .contentType(MediaType.APPLICATION_JSON_TYPE)

        return executeRequest(request, "startInstance") { response ->
            val json = objectMapper.readTree(response.body() as String)
            json.get("id")?.asText() ?: throw AdapterOperationException("camunda7", "startInstance", "No instance ID in response")
        }
    }

    fun getProcessInstance(instanceId: String): JsonNode {
        val request = HttpRequest.GET<String>("$baseUrl/process-instance/${encodePathSegment(instanceId)}")
        return executeRequest(request, "getInstance") { response ->
            objectMapper.readTree(response.body() as String)
        }
    }

    fun getProcessInstanceVariables(instanceId: String): Map<String, Any> {
        val request = HttpRequest.GET<String>("$baseUrl/process-instance/${encodePathSegment(instanceId)}/variables")
        return executeRequest(request, "getVariables") { response ->
            val json = objectMapper.readTree(response.body() as String)
            convertFromVariableMap(json)
        }
    }

    fun suspendProcessInstance(instanceId: String) {
        val body = """{"suspended": true}"""
        val request = HttpRequest.PUT("$baseUrl/process-instance/${encodePathSegment(instanceId)}/suspended", body)
            .contentType(MediaType.APPLICATION_JSON_TYPE)
        executeRequest(request, "suspend") { }
    }

    fun activateProcessInstance(instanceId: String) {
        val body = """{"suspended": false}"""
        val request = HttpRequest.PUT("$baseUrl/process-instance/${encodePathSegment(instanceId)}/suspended", body)
            .contentType(MediaType.APPLICATION_JSON_TYPE)
        executeRequest(request, "activate") { }
    }

    fun deleteProcessInstance(instanceId: String, reason: String?) {
        val params = buildString {
            append("?skipCustomListeners=false&skipIoMappings=false&skipSubprocesses=false")
            reason?.let { append("&deleteReason=${URLEncoder.encode(it, Charsets.UTF_8)}") }
        }
        val request = HttpRequest.DELETE<String>("$baseUrl/process-instance/${encodePathSegment(instanceId)}$params")
        executeRequest(request, "delete") { }
    }

    // ========== Tasks ==========

    fun getTask(taskId: String): JsonNode {
        val request = HttpRequest.GET<String>("$baseUrl/task/${encodePathSegment(taskId)}")
        return executeRequest(request, "getTask") { response ->
            objectMapper.readTree(response.body() as String)
        }
    }

    fun queryTasks(assignee: String?, processInstanceId: String?, maxResults: Int?): List<JsonNode> {
        val params = buildString {
            val parts = mutableListOf<String>()
            assignee?.let { parts.add("assignee=${URLEncoder.encode(it, Charsets.UTF_8)}") }
            processInstanceId?.let { parts.add("processInstanceId=${URLEncoder.encode(it, Charsets.UTF_8)}") }
            maxResults?.let { parts.add("maxResults=$it") }
            if (parts.isNotEmpty()) append("?${parts.joinToString("&")}")
        }
        val request = HttpRequest.GET<String>("$baseUrl/task$params")
        return executeRequest(request, "queryTasks") { response ->
            val json = objectMapper.readTree(response.body() as String)
            if (json.isArray) json.toList() else emptyList()
        }
    }

    fun completeTask(taskId: String, variables: Map<String, Any>) {
        val body = objectMapper.createObjectNode().apply {
            set<ObjectNode>("variables", convertToVariableMap(variables))
        }
        val request = HttpRequest.POST("$baseUrl/task/${encodePathSegment(taskId)}/complete", body.toString())
            .contentType(MediaType.APPLICATION_JSON_TYPE)
        executeRequest(request, "completeTask") { }
    }

    fun assignTask(taskId: String, userId: String) {
        val body = objectMapper.createObjectNode().apply { put("userId", userId) }.toString()
        val request = HttpRequest.POST("$baseUrl/task/${encodePathSegment(taskId)}/assignee", body)
            .contentType(MediaType.APPLICATION_JSON_TYPE)
        executeRequest(request, "assignTask") { }
    }

    // ========== Process Definition ==========

    fun getProcessDefinitionXml(processDefinitionId: String): String {
        val request = HttpRequest.GET<String>("$baseUrl/process-definition/${encodePathSegment(processDefinitionId)}/xml")
        return executeRequest(request, "getDefinitionXml") { response ->
            val json = objectMapper.readTree(response.body() as String)
            json.get("bpmn20Xml")?.asText()
                ?: throw AdapterOperationException("camunda7", "getDefinitionXml", "No bpmn20Xml field in response")
        }
    }

    // ========== History ==========

    fun getHistoricProcessInstance(processInstanceId: String): JsonNode {
        val request = HttpRequest.GET<String>("$baseUrl/history/process-instance/${encodePathSegment(processInstanceId)}")
        return executeRequest(request, "getHistoricInstance") { response ->
            objectMapper.readTree(response.body() as String)
        }
    }

    fun getIncidents(processInstanceId: String): JsonNode {
        val request = HttpRequest.GET<String>("$baseUrl/incident?processInstanceId=${URLEncoder.encode(processInstanceId, Charsets.UTF_8)}")
        return executeRequest(request, "getIncidents") { response ->
            objectMapper.readTree(response.body() as String)
        }
    }

    // ========== Health ==========

    fun checkHealth(): Boolean {
        return try {
            val request = HttpRequest.GET<String>("$baseUrl/version")
            val response = httpClient.toBlocking().exchange(request, String::class.java)
            response.status.code == 200
        } catch (e: Exception) {
            false
        }
    }

    fun getVersion(): JsonNode {
        val request = HttpRequest.GET<String>("$baseUrl/version")
        return executeRequest(request, "getVersion") { response ->
            objectMapper.readTree(response.body() as String)
        }
    }

    // ========== Internal Helpers ==========

    private fun <T> executeRequest(
        request: HttpRequest<*>,
        operation: String,
        handler: (HttpResponse<String>) -> T
    ): T {
        return try {
            val response = httpClient.toBlocking().exchange(request, String::class.java)
            handler(response)
        } catch (e: HttpClientResponseException) {
            val status = e.status.code
            log.error("Camunda7 {} failed with HTTP {}: {}", operation, status, e.message)
            throw AdapterOperationException("camunda7", operation, "HTTP $status: ${e.message}", e)
        } catch (e: Exception) {
            if (e is AdapterOperationException) throw e
            log.error("Camunda7 {} failed: {}", operation, e.message)
            throw AdapterOperationException("camunda7", operation, "Connection error: ${e.message}", e)
        }
    }

    private fun convertToVariableMap(variables: Map<String, Any>): ObjectNode {
        val result = objectMapper.createObjectNode()
        for ((key, value) in variables) {
            val varNode = objectMapper.createObjectNode()
            varNode.put("type", inferType(value))
            when (value) {
                is String -> varNode.put("value", value)
                is Int -> varNode.put("value", value)
                is Long -> varNode.put("value", value)
                is Double -> varNode.put("value", value)
                is Boolean -> varNode.put("value", value)
                else -> varNode.put("value", value.toString())
            }
            result.set<ObjectNode>(key, varNode)
        }
        return result
    }

    private fun convertFromVariableMap(camundaVars: JsonNode): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        camundaVars.fields().forEach { (key, varNode) ->
            val value = varNode.get("value")
            if (value != null && !value.isNull) {
                result[key] = when {
                    value.isTextual -> value.asText()
                    value.isInt -> value.asInt()
                    value.isLong -> value.asLong()
                    value.isDouble || value.isFloat -> value.asDouble()
                    value.isBoolean -> value.asBoolean()
                    else -> value.toString()
                }
            }
        }
        return result
    }

    private fun encodePathSegment(value: String): String = URLEncoder.encode(value, Charsets.UTF_8).replace("+", "%20")

    private fun inferType(value: Any): String = when (value) {
        is String -> "String"
        is Int -> "Integer"
        is Long -> "Long"
        is Double, is Float -> "Double"
        is Boolean -> "Boolean"
        else -> "Object"
    }
}
