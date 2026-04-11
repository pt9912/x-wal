package com.xwal.adapter.engine

import com.xwal.adapter.engine.flowable.FlowableAdapter
import com.xwal.domain.exception.AdapterOperationException
import com.xwal.domain.model.*
import io.micronaut.http.client.HttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FlowableAdapterMockTest {

    private lateinit var server: MockWebServer
    private lateinit var adapter: FlowableAdapter

    @BeforeAll fun setup() {
        server = MockWebServer()
        server.start()
        val url = server.url("/").toString().trimEnd('/')
        adapter = FlowableAdapter(HttpClient.create(URI.create(url).toURL()), url)
    }

    @AfterAll fun teardown() { server.shutdown() }

    @Test fun `getInstanceStatus returns RUNNING`() {
        server.enqueue(MockResponse().setBody("""[{"name":"x","value":"1","type":"string"}]""").addHeader("Content-Type", "application/json"))
        val status = adapter.getInstanceStatus("i-1")
        assertEquals(EngineInstanceStatus.Status.RUNNING, status.status)
    }

    @Test fun `getInstanceStatus returns COMPLETED on 404`() {
        server.enqueue(MockResponse().setResponseCode(404))
        val status = adapter.getInstanceStatus("i-1")
        assertEquals(EngineInstanceStatus.Status.COMPLETED, status.status)
    }

    @Test fun `getInstanceStatus rethrows non-404 errors`() {
        server.enqueue(MockResponse().setResponseCode(500))
        assertFailsWith<AdapterOperationException> { adapter.getInstanceStatus("i-1") }
    }

    @Test fun `getHistoricInstance maps fields`() {
        server.enqueue(MockResponse().setBody("""{"id":"i-1","processDefinitionId":"pd-1","businessKey":"BK","durationInMillis":5000}""").addHeader("Content-Type", "application/json"))
        val hist = adapter.getHistoricInstance("i-1")
        assertEquals("i-1", hist.instanceId)
        assertEquals(5000L, hist.durationInMillis)
    }

    @Test fun `getActiveIncidents from suspended executions`() {
        server.enqueue(MockResponse().setBody("""{"data":[{"id":"e-1","suspensionState":2,"activityId":"task1"},{"id":"e-2","suspensionState":1,"activityId":"task2"}]}""").addHeader("Content-Type", "application/json"))
        val incidents = adapter.getActiveIncidents("i-1")
        assertEquals(1, incidents.size) // only suspensionState == 2
        assertEquals(IncidentType.FAILED_ACTIVITY, incidents[0].type)
        assertEquals("task1", incidents[0].activityId)
    }

    @Test fun `getActiveIncidents no suspended executions`() {
        server.enqueue(MockResponse().setBody("""{"data":[{"id":"e-1","suspensionState":1}]}""").addHeader("Content-Type", "application/json"))
        assertTrue(adapter.getActiveIncidents("i-1").isEmpty())
    }

    @Test fun `queryTasks maps to Task model`() {
        server.enqueue(MockResponse().setBody("""{"data":[{"id":"t-1","name":"Check","assignee":"bob","processInstanceId":"i-1","taskDefinitionKey":"checkTask","priority":50}]}""").addHeader("Content-Type", "application/json"))
        val tasks = adapter.queryTasks(TaskFilter())
        assertEquals(1, tasks.size)
        assertEquals("Check", tasks[0].name)
        assertEquals("bob", tasks[0].assignee)
        assertEquals(TaskStatus.ASSIGNED, tasks[0].status)
    }

    @Test fun `deployWorkflow transforms and deploys`() {
        server.enqueue(MockResponse().setBody("""{"id":"fd-1"}""").addHeader("Content-Type", "application/json").setResponseCode(201))
        val iwm = """{"workflow":{"name":"t","version":"1.0"},"tasks":[{"id":"s","type":"start","label":"S"}],"edges":[]}"""
        assertEquals("fd-1", adapter.deployWorkflow(iwm, "proc", "1.0"))
    }

    @Test fun `startInstance calls REST client`() {
        server.enqueue(MockResponse().setBody("""{"id":"fi-1"}""").addHeader("Content-Type", "application/json").setResponseCode(201))
        assertEquals("fi-1", adapter.startInstance("proc", "BK", mapOf("k" to "v")))
    }

    @Test fun `getInstanceVariables returns map`() {
        server.enqueue(MockResponse().setBody("""[{"name":"approved","value":true,"type":"boolean"}]""").addHeader("Content-Type", "application/json"))
        assertEquals(true, adapter.getInstanceVariables("i-1")["approved"])
    }
}
