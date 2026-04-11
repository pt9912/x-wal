package com.xwal.adapter.engine

import com.xwal.adapter.engine.resilience.AdapterResilientDecorator
import com.xwal.adapter.engine.resilience.DeadLetterQueue
import com.xwal.adapter.engine.resilience.FailedOperation
import com.xwal.adapter.engine.resilience.Resilience4jConfig
import com.xwal.domain.exception.AdapterOperationException
import org.junit.jupiter.api.Test
import java.util.function.Supplier
import kotlin.test.*

class ResilienceTest {

    @Test
    fun `successful execution returns result`() {
        val config = Resilience4jConfig()
        val result = AdapterResilientDecorator.execute("test", config, Supplier { "ok" })
        assertEquals("ok", result)
    }

    @Test
    fun `failing execution throws AdapterOperationException`() {
        val config = Resilience4jConfig()
        assertFailsWith<AdapterOperationException> {
            AdapterResilientDecorator.execute("test-fail", config, Supplier { throw RuntimeException("boom") })
        }
    }

    @Test
    fun `dead letter queue stores failed operations`() {
        val dlq = DeadLetterQueue()
        assertEquals(0, dlq.size())

        dlq.add(FailedOperation("adapter1", "deploy", "Connection refused", "CONNECTION_ERROR"))
        assertEquals(1, dlq.size())

        val op = dlq.poll()
        assertNotNull(op)
        assertEquals("adapter1", op.adapterName)
        assertEquals("deploy", op.operationType)
        assertEquals(0, dlq.size())
    }

    @Test
    fun `dead letter queue getAll returns all`() {
        val dlq = DeadLetterQueue()
        dlq.add(FailedOperation("a1", "op1", "err1", "ERR"))
        dlq.add(FailedOperation("a2", "op2", "err2", "ERR"))
        assertEquals(2, dlq.getAll().size)
    }

    @Test
    fun `dead letter queue clear removes all`() {
        val dlq = DeadLetterQueue()
        dlq.add(FailedOperation("a1", "op1", "err1", "ERR"))
        dlq.clear()
        assertEquals(0, dlq.size())
    }

    @Test
    fun `dead letter queue peek does not remove`() {
        val dlq = DeadLetterQueue()
        dlq.add(FailedOperation("a1", "op1", "err1", "ERR"))
        assertNotNull(dlq.peek())
        assertEquals(1, dlq.size())
    }

    @Test
    fun `failed operation records DLQ entry`() {
        val config = Resilience4jConfig()
        val dlq = DeadLetterQueue()
        assertFailsWith<AdapterOperationException> {
            AdapterResilientDecorator.execute("dlq-test", config, Supplier { throw RuntimeException("fail") }, "deploy", dlq)
        }
        assertEquals(1, dlq.size())
        assertEquals("dlq-test", dlq.peek()?.adapterName)
    }

    @Test
    fun `circuit breaker name matches adapter`() {
        val config = Resilience4jConfig()
        val cb = config.getCircuitBreaker("my-adapter")
        assertEquals("my-adapter", cb.name)
    }

    @Test
    fun `retry name matches adapter`() {
        val config = Resilience4jConfig()
        val retry = config.getRetry("my-adapter")
        assertEquals("my-adapter", retry.name)
    }
}
