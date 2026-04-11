package com.xwal.adapter.engine.integration

import com.xwal.adapter.engine.camunda7.Camunda7Adapter
import com.xwal.adapter.engine.camunda7.transformer.IwmToBpmnTransformer
import com.xwal.domain.model.*
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.multipart.MultipartBody
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.net.URI
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Camunda7IntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val camunda = GenericContainer("camunda/camunda-bpm-platform:7.24.0")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/engine-rest/engine").forStatusCode(200))
            .withStartupTimeout(Duration.ofMinutes(2))
    }

    private val urlaubsantragIwm = """{"workflow":{"name":"urlaubsantrag","version":"1.0.0"},"tasks":[{"id":"start","type":"activity","operation":"startProcess","label":"Start"},{"id":"genehmigen","type":"userTask","label":"Antrag genehmigen","user":{"assignee":"manager"}},{"id":"end","type":"activity","operation":"endProcess","label":"Ende"}],"edges":[{"from":"start","to":"genehmigen"},{"from":"genehmigen","to":"end"}]}"""

    @Test
    fun `health check`() {
        val adapter = adapter()
        assertTrue(adapter.checkHealth())
        assertEquals("Camunda 7", adapter.engineName)
    }

    @Test
    fun `E2E urlaubsantrag`() {
        val baseUrl = baseUrl()
        val httpClient = HttpClient.create(URI.create(baseUrl).toURL())

        // Generate BPMN
        val bpmn = IwmToBpmnTransformer().transformToBpmn(urlaubsantragIwm, "urlaubsantrag", "1.0.0")
        println("BPMN process line: ${bpmn.lines().find { it.contains("<bpmn:process") }}")

        // Deploy directly via REST (bypassing our client to debug)
        val deployBody = MultipartBody.builder()
            .addPart("deployment-name", "e2e-test")
            .addPart("deployment-source", "x-wal-test")
            .addPart("urlaubsantrag.bpmn", "urlaubsantrag.bpmn", MediaType.APPLICATION_XML_TYPE, bpmn.toByteArray())
            .build()
        val deployReq = HttpRequest.POST("$baseUrl/deployment/create", deployBody)
            .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)
        val deployResp = httpClient.toBlocking().exchange(deployReq, String::class.java)
        println("Deploy response: ${deployResp.body()}")

        // List process definitions
        val defsResp = httpClient.toBlocking().retrieve("$baseUrl/process-definition")
        println("Process definitions: $defsResp")

        // Start
        val adapter = adapter()
        val instanceId = adapter.startInstance("urlaubsantrag", "BK-E2E", mapOf("employee" to "alice"))
        assertNotNull(instanceId)

        // Query task
        val tasks = adapter.queryTasks(TaskFilter(assignee = "manager"))
        assertTrue(tasks.isNotEmpty())

        // Complete
        adapter.completeTask(tasks.first().taskId.engineTaskId, mapOf("approved" to true))

        // History
        val hist = adapter.getHistoricInstance(instanceId)
        assertNotNull(hist)
        println("E2E complete!")
    }

    @Test fun `capabilities`() { assertTrue(adapter().getCapabilities().supportsUserTasks) }

    private fun baseUrl() = "http://localhost:${camunda.getMappedPort(8080)}/engine-rest"
    private fun adapter() = Camunda7Adapter(HttpClient.create(URI.create(baseUrl()).toURL()), baseUrl())
}
