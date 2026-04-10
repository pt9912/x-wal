package com.xwal.adapter.engine.flowable.client

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.xwal.domain.exception.AdapterOperationException
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.client.multipart.MultipartBody
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

class FlowableRestClient(
    private val httpClient: HttpClient,
    baseUrl: String
) {
    private val log = LoggerFactory.getLogger(FlowableRestClient::class.java)
    private val objectMapper = ObjectMapper()
    private val baseUrl: String
    private val username: String?
    private val password: String?

    init {
        val creds = extractCredentials(baseUrl)
        this.baseUrl = creds.cleanedBaseUrl.trimEnd('/')
        this.username = creds.username
        this.password = creds.password
    }

    // ========== Deployment ==========

    fun deployProcess(bpmnXml: String, deploymentName: String, processName: String): String {
        val body = MultipartBody.builder()
            .addPart("deploymentName", deploymentName)
            .addPart("deploymentKey", processName)
            .addPart("tenantId", "")
            .addPart("$processName.bpmn20.xml", processName, MediaType.APPLICATION_XML_TYPE, bpmnXml.toByteArray())
            .build()

        val request = applyAuth(
            HttpRequest.POST("$baseUrl/repository/deployments", body)
                .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)
        )

        return executeRequest(request, "deploy") { response ->
            val json = objectMapper.readTree(response.body() as String)
            json.get("id")?.asText() ?: throw AdapterOperationException("flowable", "deploy", "No deployment ID")
        }
    }

    // ========== Process Instance ==========

    fun startProcessInstance(processDefinitionKey: String, businessKey: String?, variables: Map<String, Any>): String {
        val body = objectMapper.createObjectNode().apply {
            put("processDefinitionKey", processDefinitionKey)
            businessKey?.let { put("businessKey", it) }
            set<ArrayNode>("variables", convertToFlowableFormat(variables))
        }

        val request = applyAuth(
            HttpRequest.POST("$baseUrl/runtime/process-instances", body.toString())
                .contentType(MediaType.APPLICATION_JSON_TYPE)
        )

        return executeRequest(request, "startInstance") { response ->
            val json = objectMapper.readTree(response.body() as String)
            json.get("id")?.asText() ?: throw AdapterOperationException("flowable", "startInstance", "No instance ID")
        }
    }

    fun suspendProcessInstance(instanceId: String) {
        val body = """{"action":"suspend"}"""
        val request = applyAuth(
            HttpRequest.PUT("$baseUrl/runtime/process-instances/$instanceId", body)
                .contentType(MediaType.APPLICATION_JSON_TYPE)
        )
        executeRequest(request, "suspend") { }
    }

    fun activateProcessInstance(instanceId: String) {
        val body = """{"action":"activate"}"""
        val request = applyAuth(
            HttpRequest.PUT("$baseUrl/runtime/process-instances/$instanceId", body)
                .contentType(MediaType.APPLICATION_JSON_TYPE)
        )
        executeRequest(request, "activate") { }
    }

    fun deleteProcessInstance(instanceId: String, reason: String?) {
        val encoded = reason?.let { "?deleteReason=${URLEncoder.encode(it, Charsets.UTF_8)}" } ?: ""
        val request = applyAuth(HttpRequest.DELETE<String>("$baseUrl/runtime/process-instances/$instanceId$encoded"))
        executeRequest(request, "delete") { }
    }

    fun getProcessInstanceVariables(instanceId: String): Map<String, Any> {
        val request = applyAuth(HttpRequest.GET<String>("$baseUrl/runtime/process-instances/$instanceId/variables"))
        return executeRequest(request, "getVariables") { response ->
            val json = objectMapper.readTree(response.body() as String)
            convertFlowableVariablesToMap(json)
        }
    }

    // ========== Tasks ==========

    fun getTask(taskId: String): JsonNode {
        val request = applyAuth(HttpRequest.GET<String>("$baseUrl/runtime/tasks/$taskId"))
        return executeRequest(request, "getTask") { response ->
            objectMapper.readTree(response.body() as String)
        }
    }

    fun queryTasks(assignee: String?, candidateGroup: String?, processInstanceId: String?): JsonNode {
        val params = buildString {
            val parts = mutableListOf<String>()
            assignee?.let { parts.add("assignee=$it") }
            candidateGroup?.let { parts.add("candidateGroup=$it") }
            processInstanceId?.let { parts.add("processInstanceId=$it") }
            if (parts.isNotEmpty()) append("?${parts.joinToString("&")}")
        }
        val request = applyAuth(HttpRequest.GET<String>("$baseUrl/runtime/tasks$params"))
        return executeRequest(request, "queryTasks") { response ->
            val json = objectMapper.readTree(response.body() as String)
            json.get("data") ?: objectMapper.createArrayNode()
        }
    }

    fun completeTask(taskId: String, variables: Map<String, Any>) {
        val body = objectMapper.createObjectNode().apply {
            put("action", "complete")
            set<ArrayNode>("variables", convertToFlowableFormat(variables))
        }
        val request = applyAuth(
            HttpRequest.POST("$baseUrl/runtime/tasks/$taskId", body.toString())
                .contentType(MediaType.APPLICATION_JSON_TYPE)
        )
        executeRequest(request, "completeTask") { }
    }

    fun assignTask(taskId: String, assignee: String) {
        val body = objectMapper.createObjectNode().apply { put("assignee", assignee) }.toString()
        val request = applyAuth(
            HttpRequest.PUT("$baseUrl/runtime/tasks/$taskId", body)
                .contentType(MediaType.APPLICATION_JSON_TYPE)
        )
        executeRequest(request, "assignTask") { }
    }

    // ========== Process Definition ==========

    fun getProcessDefinitionXml(processDefinitionId: String): String {
        val request = applyAuth(HttpRequest.GET<String>("$baseUrl/repository/process-definitions/$processDefinitionId/resourcedata"))
        return executeRequest(request, "getDefinitionXml") { response ->
            response.body() as String
        }
    }

    // ========== History ==========

    fun getHistoricProcessInstance(processInstanceId: String): JsonNode {
        val request = applyAuth(HttpRequest.GET<String>("$baseUrl/history/historic-process-instances/$processInstanceId"))
        return executeRequest(request, "getHistoric") { response ->
            objectMapper.readTree(response.body() as String)
        }
    }

    fun getExecutions(processInstanceId: String): JsonNode {
        val request = applyAuth(HttpRequest.GET<String>("$baseUrl/runtime/executions?processInstanceId=$processInstanceId"))
        return executeRequest(request, "getExecutions") { response ->
            val json = objectMapper.readTree(response.body() as String)
            json.get("data") ?: objectMapper.createArrayNode()
        }
    }

    // ========== Health ==========

    fun checkHealth(): Boolean {
        return try {
            val request = applyAuth(HttpRequest.GET<String>("$baseUrl/management/engine"))
            val response = httpClient.toBlocking().exchange(request, String::class.java)
            response.status.code == 200
        } catch (_: Exception) { false }
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
            log.error("Flowable {} failed with HTTP {}: {}", operation, e.status.code, e.message)
            throw AdapterOperationException("flowable", operation, "HTTP ${e.status.code}: ${e.message}", e)
        } catch (e: Exception) {
            if (e is AdapterOperationException) throw e
            log.error("Flowable {} failed: {}", operation, e.message)
            throw AdapterOperationException("flowable", operation, "Connection error: ${e.message}", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> applyAuth(request: HttpRequest<T>): HttpRequest<T> {
        return if (username != null && password != null) {
            (request as io.micronaut.http.MutableHttpRequest<T>).basicAuth(username, password)
        } else request
    }

    private fun convertToFlowableFormat(variables: Map<String, Any>): ArrayNode {
        val arr = objectMapper.createArrayNode()
        for ((key, value) in variables) {
            val node = objectMapper.createObjectNode()
            node.put("name", key)
            when (value) {
                is String -> { node.put("value", value); node.put("type", "string") }
                is Int -> { node.put("value", value); node.put("type", "integer") }
                is Long -> { node.put("value", value); node.put("type", "long") }
                is Double -> { node.put("value", value); node.put("type", "double") }
                is Boolean -> { node.put("value", value); node.put("type", "boolean") }
                else -> { node.put("value", value.toString()); node.put("type", "string") }
            }
            arr.add(node)
        }
        return arr
    }

    private fun convertFlowableVariablesToMap(variablesNode: JsonNode): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val arr = if (variablesNode.isArray) variablesNode else return result
        for (node in arr) {
            val name = node.path("name").asText(null) ?: continue
            val type = node.path("type").asText("string")
            val value: Any = when (type) {
                "integer" -> node.path("value").asInt()
                "long" -> node.path("value").asLong()
                "double" -> node.path("value").asDouble()
                "boolean" -> node.path("value").asBoolean()
                else -> node.path("value").asText("")
            }
            result[name] = value
        }
        return result
    }

    private data class CredentialsAndUrl(val cleanedBaseUrl: String, val username: String?, val password: String?)

    private fun extractCredentials(rawBaseUrl: String): CredentialsAndUrl {
        return try {
            val uri = URI(rawBaseUrl)
            val userInfo = uri.userInfo
            if (userInfo != null && userInfo.contains(":")) {
                val parts = userInfo.split(":", limit = 2)
                val cleanUrl = rawBaseUrl.replace("${userInfo}@", "")
                CredentialsAndUrl(cleanUrl, URLDecoder.decode(parts[0], Charsets.UTF_8), URLDecoder.decode(parts[1], Charsets.UTF_8))
            } else {
                CredentialsAndUrl(rawBaseUrl, null, null)
            }
        } catch (_: Exception) {
            CredentialsAndUrl(rawBaseUrl, null, null)
        }
    }
}
