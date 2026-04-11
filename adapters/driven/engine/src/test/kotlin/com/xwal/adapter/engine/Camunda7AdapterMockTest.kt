package com.xwal.adapter.engine

import com.fasterxml.jackson.databind.ObjectMapper
import com.xwal.adapter.engine.camunda7.Camunda7Adapter
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Camunda7AdapterMockTest {

    private lateinit var server: MockWebServer
    private lateinit var adapter: Camunda7Adapter

    @BeforeAll fun setup() {
        server = MockWebServer()
        server.start()
        val url = server.url("/").toString().trimEnd('/')
        // Enqueue version response for constructor health check
        server.enqueue(MockResponse().setBody("""{"version":"7.24.0"}""").addHeader("Content-Type", "application/json"))
        adapter = Camunda7Adapter(HttpClient.create(URI.create(url).toURL()), url)
    }

    @AfterAll fun teardown() { server.shutdown() }

    @Test fun `getInstanceStatus returns RUNNING`() {
        server.enqueue(MockResponse().setBody("""{"id":"i-1","suspended":false,"ended":false,"definitionId":"pd-1"}""").addHeader("Content-Type", "application/json"))
        server.enqueue(MockResponse().setBody("""{"approved":{"value":true,"type":"Boolean"}}""").addHeader("Content-Type", "application/json"))
        val status = adapter.getInstanceStatus("i-1")
        assertEquals(EngineInstanceStatus.Status.RUNNING, status.status)
        assertEquals("i-1", status.instanceId)
    }

    @Test fun `getInstanceStatus returns SUSPENDED`() {
        server.enqueue(MockResponse().setBody("""{"id":"i-1","suspended":true,"ended":false}""").addHeader("Content-Type", "application/json"))
        server.enqueue(MockResponse().setBody("""{}""").addHeader("Content-Type", "application/json"))
        assertEquals(EngineInstanceStatus.Status.SUSPENDED, adapter.getInstanceStatus("i-1").status)
    }

    @Test fun `getInstanceStatus returns COMPLETED`() {
        server.enqueue(MockResponse().setBody("""{"id":"i-1","suspended":false,"ended":true}""").addHeader("Content-Type", "application/json"))
        server.enqueue(MockResponse().setBody("""{}""").addHeader("Content-Type", "application/json"))
        assertEquals(EngineInstanceStatus.Status.COMPLETED, adapter.getInstanceStatus("i-1").status)
    }

    @Test fun `getHistoricInstance maps fields`() {
        server.enqueue(MockResponse().setBody("""{"id":"i-1","processDefinitionId":"pd-1","businessKey":"BK-1","startTime":"2026-01-01T00:00:00+00:00","endTime":"2026-01-02T00:00:00+00:00","durationInMillis":86400000,"deleteReason":null}""").addHeader("Content-Type", "application/json"))
        val hist = adapter.getHistoricInstance("i-1")
        assertEquals("i-1", hist.instanceId)
        assertEquals("pd-1", hist.processDefinitionId)
        assertEquals("BK-1", hist.businessKey)
        assertNotNull(hist.startTime)
        assertEquals(86400000L, hist.durationInMillis)
    }

    @Test fun `getActiveIncidents maps incidents`() {
        server.enqueue(MockResponse().setBody("""[{"id":"inc-1","processInstanceId":"i-1","executionId":"exec-1","activityId":"task1","incidentType":"failedJob","incidentMessage":"NullPointer","incidentTimestamp":"2026-01-01T12:00:00+00:00"}]""").addHeader("Content-Type", "application/json"))
        val incidents = adapter.getActiveIncidents("i-1")
        assertEquals(1, incidents.size)
        assertEquals(IncidentType.FAILED_JOB, incidents[0].type)
        assertEquals("NullPointer", incidents[0].message)
    }

    @Test fun `getActiveIncidents with failedExternalTask`() {
        server.enqueue(MockResponse().setBody("""[{"id":"inc-2","incidentType":"failedExternalTask","incidentMessage":"timeout"}]""").addHeader("Content-Type", "application/json"))
        assertEquals(IncidentType.FAILED_EXTERNAL_TASK, adapter.getActiveIncidents("i-1")[0].type)
    }

    @Test fun `getActiveIncidents with unknown type`() {
        server.enqueue(MockResponse().setBody("""[{"id":"inc-3","incidentType":"somethingNew","incidentMessage":"?"}]""").addHeader("Content-Type", "application/json"))
        assertEquals(IncidentType.UNKNOWN, adapter.getActiveIncidents("i-1")[0].type)
    }

    @Test fun `getActiveIncidents empty array`() {
        server.enqueue(MockResponse().setBody("""[]""").addHeader("Content-Type", "application/json"))
        assertTrue(adapter.getActiveIncidents("i-1").isEmpty())
    }

    @Test fun `queryTasks maps to Task model`() {
        server.enqueue(MockResponse().setBody("""[{"id":"t-1","name":"Approve","assignee":"alice","processInstanceId":"i-1","taskDefinitionKey":"approveTask","priority":75,"created":"2026-01-01T10:00:00+00:00"}]""").addHeader("Content-Type", "application/json"))
        val tasks = adapter.queryTasks(TaskFilter(assignee = "alice"))
        assertEquals(1, tasks.size)
        assertEquals("Approve", tasks[0].name)
        assertEquals("alice", tasks[0].assignee)
        assertEquals(TaskStatus.ASSIGNED, tasks[0].status)
        assertEquals(75, tasks[0].priority)
    }

    @Test fun `queryTasks unassigned task has CREATED status`() {
        server.enqueue(MockResponse().setBody("""[{"id":"t-2","name":"Review","processInstanceId":"i-1"}]""").addHeader("Content-Type", "application/json"))
        assertEquals(TaskStatus.CREATED, adapter.queryTasks(TaskFilter())[0].status)
    }

    @Test fun `getInstanceVariables delegates to REST client`() {
        server.enqueue(MockResponse().setBody("""{"days":{"value":5,"type":"Integer"},"name":{"value":"Alice","type":"String"}}""").addHeader("Content-Type", "application/json"))
        val vars = adapter.getInstanceVariables("i-1")
        assertEquals(5, vars["days"])
        assertEquals("Alice", vars["name"])
    }

    @Test fun `deployWorkflow transforms IWM and deploys`() {
        // Deploy response
        server.enqueue(MockResponse().setBody("""{"id":"dep-1"}""").addHeader("Content-Type", "application/json"))
        val iwm = """{"workflow":{"name":"t","version":"1.0"},"tasks":[{"id":"s","type":"start","label":"S"}],"edges":[]}"""
        val depId = adapter.deployWorkflow(iwm, "test-proc", "1.0")
        assertEquals("dep-1", depId)
    }

    @Test fun `startInstance calls REST client`() {
        server.enqueue(MockResponse().setBody("""{"id":"new-inst-1"}""").addHeader("Content-Type", "application/json"))
        assertEquals("new-inst-1", adapter.startInstance("myProc", "BK-1", mapOf("x" to 1)))
    }
}
