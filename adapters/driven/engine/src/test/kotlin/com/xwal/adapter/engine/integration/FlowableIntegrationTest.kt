package com.xwal.adapter.engine.integration

import com.xwal.adapter.engine.flowable.FlowableAdapter
import com.xwal.adapter.engine.flowable.transformer.IwmToBpmnTransformer
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
class FlowableIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val flowable = GenericContainer("flowable/flowable-rest:8.0.0")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/flowable-rest/service/management/engine")
                .withBasicCredentials("rest-admin", "test")
                .forStatusCode(200))
            .withStartupTimeout(Duration.ofMinutes(3))
    }

    private val urlaubsantragIwm = """{"workflow":{"name":"urlaubsantrag","version":"1.0.0"},"tasks":[{"id":"start","type":"activity","operation":"startProcess","label":"Start"},{"id":"genehmigen","type":"userTask","label":"Antrag genehmigen","user":{"assignee":"manager"}},{"id":"end","type":"activity","operation":"endProcess","label":"Ende"}],"edges":[{"from":"start","to":"genehmigen"},{"from":"genehmigen","to":"end"}]}"""

    private fun baseUrl() = "http://rest-admin:test@localhost:${flowable.getMappedPort(8080)}/flowable-rest/service"
    private fun adapter() = FlowableAdapter(
        HttpClient.create(URI.create("http://localhost:${flowable.getMappedPort(8080)}/flowable-rest/service").toURL()),
        baseUrl()
    )

    @Test
    fun `health check`() {
        assertTrue(adapter().checkHealth())
        assertEquals("Flowable", adapter().engineName)
    }

    @Test
    fun `E2E urlaubsantrag`() {
        val adapter = adapter()

        // Deploy
        val deployId = adapter.deployWorkflow(urlaubsantragIwm, "urlaubsantrag", "1.0.0")
        assertNotNull(deployId)
        println("Flowable deploy: $deployId")

        // Start
        val instanceId = adapter.startInstance("urlaubsantrag", "URL-FLOW-001", mapOf("employee" to "bob"))
        assertNotNull(instanceId)

        // Verify running
        val status = adapter.getInstanceStatus(instanceId)
        assertEquals(EngineInstanceStatus.Status.RUNNING, status.status)

        // Query task
        val tasks = adapter.queryTasks(TaskFilter(assignee = "manager"))
        assertTrue(tasks.isNotEmpty())

        // Complete
        adapter.completeTask(tasks.first().taskId.engineTaskId, mapOf("approved" to true))

        // Verify completed
        val finalStatus = adapter.getInstanceStatus(instanceId)
        assertEquals(EngineInstanceStatus.Status.COMPLETED, finalStatus.status)
        println("Flowable E2E complete!")
    }

    @Test
    fun `capabilities differ from camunda`() {
        val caps = adapter().getCapabilities()
        assertTrue(caps.supportsUserTasks)
        assertTrue(caps.supportsDMN)
    }
}
