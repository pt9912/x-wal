package com.xwal.adapter.engine

import com.xwal.adapter.engine.flowable.client.FlowableRestClient
import com.xwal.domain.exception.AdapterOperationException
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FlowableRestClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: FlowableRestClient

    @BeforeAll fun setup() {
        server = MockWebServer()
        server.start()
        val url = server.url("/").toString().trimEnd('/')
        client = FlowableRestClient(HttpClient.create(URI.create(url).toURL()), url)
    }

    @AfterAll fun teardown() { server.shutdown() }

    @Test fun `checkHealth returns true`() {
        server.enqueue(MockResponse().setBody("""[{"name":"default"}]""").setResponseCode(200))
        assertTrue(client.checkHealth())
    }

    @Test fun `checkHealth returns false on error`() {
        server.enqueue(MockResponse().setResponseCode(500))
        assertFalse(client.checkHealth())
    }

    @Test fun `deployProcess returns ID`() {
        server.enqueue(MockResponse().setBody("""{"id":"fd-1"}""").addHeader("Content-Type", "application/json").setResponseCode(201))
        assertEquals("fd-1", client.deployProcess("<bpmn/>", "dep", "proc"))
    }

    @Test fun `startProcessInstance returns ID`() {
        server.enqueue(MockResponse().setBody("""{"id":"fi-1"}""").addHeader("Content-Type", "application/json").setResponseCode(201))
        assertEquals("fi-1", client.startProcessInstance("key", "bk", mapOf("k" to "v")))
    }

    @Test fun `getProcessInstanceVariables returns map`() {
        server.enqueue(MockResponse().setBody("""[{"name":"x","value":"y","type":"string"}]""").addHeader("Content-Type", "application/json"))
        val vars = client.getProcessInstanceVariables("i-1")
        assertEquals("y", vars["x"])
    }

    @Test fun `suspendProcessInstance sends request`() {
        server.enqueue(MockResponse().setResponseCode(200))
        client.suspendProcessInstance("i-1")
        assertNotNull(server.takeRequest())
    }

    @Test fun `deleteProcessInstance sends request`() {
        server.enqueue(MockResponse().setResponseCode(204))
        client.deleteProcessInstance("i-1", "cancelled")
        assertNotNull(server.takeRequest())
    }

    @Test fun `getTask returns JSON`() {
        server.enqueue(MockResponse().setBody("""{"id":"ft-1","name":"Review"}""").addHeader("Content-Type", "application/json"))
        assertEquals("ft-1", client.getTask("ft-1").get("id").asText())
    }

    @Test fun `queryTasks returns data array`() {
        server.enqueue(MockResponse().setBody("""{"data":[{"id":"t1"}],"total":1}""").addHeader("Content-Type", "application/json"))
        assertEquals(1, client.queryTasks(null, null, null).size())
    }

    @Test fun `completeTask sends request`() {
        server.enqueue(MockResponse().setResponseCode(200))
        client.completeTask("t-1", mapOf("ok" to true))
        assertNotNull(server.takeRequest())
    }

    @Test fun `assignTask sends request`() {
        server.enqueue(MockResponse().setResponseCode(200))
        client.assignTask("t-1", "bob")
        assertNotNull(server.takeRequest())
    }

    @Test fun `getProcessDefinitionXml returns raw XML`() {
        server.enqueue(MockResponse().setBody("<bpmn>flowable</bpmn>").addHeader("Content-Type", "application/xml"))
        assertEquals("<bpmn>flowable</bpmn>", client.getProcessDefinitionXml("pd-1"))
    }

    @Test fun `getHistoricProcessInstance returns JSON`() {
        server.enqueue(MockResponse().setBody("""{"id":"i-1"}""").addHeader("Content-Type", "application/json"))
        assertEquals("i-1", client.getHistoricProcessInstance("i-1").get("id").asText())
    }

    @Test fun `getExecutions returns data`() {
        server.enqueue(MockResponse().setBody("""{"data":[{"id":"e-1"}]}""").addHeader("Content-Type", "application/json"))
        assertTrue(client.getExecutions("i-1").isArray)
    }

    @Test fun `404 throws`() {
        server.enqueue(MockResponse().setResponseCode(404))
        assertFailsWith<AdapterOperationException> { client.getProcessInstanceVariables("x") }
    }

    @Test fun `credentials from URL`() {
        val authServer = MockWebServer()
        authServer.start()
        try {
            authServer.enqueue(MockResponse().setBody("[]").setResponseCode(200))
            val url = authServer.url("/").toString().trimEnd('/')
            val authClient = FlowableRestClient(HttpClient.create(URI.create(url).toURL()), "http://admin:pass@localhost:${authServer.port}")
            assertTrue(authClient.checkHealth())
        } finally { authServer.shutdown() }
    }
}
