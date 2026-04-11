package com.xwal.adapter.engine

import com.xwal.adapter.engine.camunda7.client.Camunda7RestClient
import com.xwal.adapter.engine.flowable.client.FlowableRestClient
import io.micronaut.http.client.HttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RestClientVariableTest {

    private fun <T> withCamunda(block: (MockWebServer, Camunda7RestClient) -> T): T {
        val s = MockWebServer(); s.start()
        try { val u = s.url("/").toString().trimEnd('/'); return block(s, Camunda7RestClient(HttpClient.create(URI.create(u).toURL()), u)) }
        finally { s.shutdown() }
    }

    private fun <T> withFlowable(block: (MockWebServer, FlowableRestClient) -> T): T {
        val s = MockWebServer(); s.start()
        try { val u = s.url("/").toString().trimEnd('/'); return block(s, FlowableRestClient(HttpClient.create(URI.create(u).toURL()), u)) }
        finally { s.shutdown() }
    }

    @Test fun `Camunda sends typed variables`() = withCamunda { s, c ->
        s.enqueue(MockResponse().setBody("""{"id":"i-1"}""").addHeader("Content-Type", "application/json"))
        c.startProcessInstance("p", null, mapOf("str" to "hello", "num" to 42, "bool" to true, "dbl" to 3.14, "lng" to 9999L))
        val body = s.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"type\":\"String\""))
        assertTrue(body.contains("\"type\":\"Integer\""))
        assertTrue(body.contains("\"type\":\"Boolean\""))
        assertTrue(body.contains("\"type\":\"Double\""))
        assertTrue(body.contains("\"type\":\"Long\""))
    }

    @Test fun `Camunda reads typed variables`() = withCamunda { s, c ->
        s.enqueue(MockResponse().setBody("""{"str":{"value":"hi","type":"String"},"num":{"value":5,"type":"Integer"},"bool":{"value":true,"type":"Boolean"},"dbl":{"value":1.5,"type":"Double"},"lng":{"value":999,"type":"Long"}}""").addHeader("Content-Type", "application/json"))
        val v = c.getProcessInstanceVariables("i-1")
        assertEquals("hi", v["str"]); assertEquals(5, v["num"]); assertEquals(true, v["bool"])
    }

    @Test fun `Camunda handles empty vars`() = withCamunda { s, c ->
        s.enqueue(MockResponse().setBody("""{"id":"i-1"}""").addHeader("Content-Type", "application/json"))
        c.startProcessInstance("p", null, emptyMap())
        assertNotNull(s.takeRequest())
    }

    @Test fun `Camunda queryTasks with params`() = withCamunda { s, c ->
        s.enqueue(MockResponse().setBody("""[{"id":"t1"}]""").addHeader("Content-Type", "application/json"))
        c.queryTasks("alice", "inst-1", 25)
        assertTrue(s.takeRequest().path!!.contains("assignee="))
    }

    @Test fun `Camunda completeTask with vars`() = withCamunda { s, c ->
        s.enqueue(MockResponse().setBody("").addHeader("Content-Type", "application/json").setResponseCode(200))
        c.completeTask("t-1", mapOf("ok" to true, "msg" to "done"))
        assertTrue(s.takeRequest().body.readUtf8().contains("ok"))
    }

    @Test fun `Flowable sends typed variables`() = withFlowable { s, c ->
        s.enqueue(MockResponse().setBody("""{"id":"i-1"}""").addHeader("Content-Type", "application/json").setResponseCode(201))
        c.startProcessInstance("p", "BK", mapOf("str" to "x", "num" to 7, "bool" to false, "dbl" to 2.5, "lng" to 888L))
        val body = s.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"type\":\"string\"")); assertTrue(body.contains("\"type\":\"integer\""))
        assertTrue(body.contains("\"type\":\"boolean\"")); assertTrue(body.contains("\"type\":\"double\""))
        assertTrue(body.contains("BK"))
    }

    @Test fun `Flowable reads typed variables`() = withFlowable { s, c ->
        s.enqueue(MockResponse().setBody("""[{"name":"x","value":"hi","type":"string"},{"name":"n","value":42,"type":"integer"},{"name":"b","value":true,"type":"boolean"},{"name":"l","value":99,"type":"long"},{"name":"d","value":1.5,"type":"double"}]""").addHeader("Content-Type", "application/json"))
        val v = c.getProcessInstanceVariables("i-1")
        assertEquals("hi", v["x"]); assertEquals(42, v["n"]); assertEquals(true, v["b"])
    }

    @Test fun `Flowable queryTasks with candidateGroup`() = withFlowable { s, c ->
        s.enqueue(MockResponse().setBody("""{"data":[],"total":0}""").addHeader("Content-Type", "application/json"))
        c.queryTasks(null, "managers", null)
        assertTrue(s.takeRequest().path!!.contains("candidateGroup="))
    }

    @Test fun `Flowable completeTask`() = withFlowable { s, c ->
        s.enqueue(MockResponse().setBody("").addHeader("Content-Type", "application/json").setResponseCode(200))
        c.completeTask("t-1", mapOf("done" to true))
        assertTrue(s.takeRequest().body.readUtf8().contains("complete"))
    }
}
