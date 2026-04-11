package com.xwal.config

import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigTest {

    @Test fun `InstanceSyncConfig defaults`() {
        val config = InstanceSyncConfig()
        assertTrue(config.enabled)
        assertEquals(100, config.batchSize)
        assertEquals(Duration.ofSeconds(5), config.timeout)
        assertTrue(config.includeSuspended)
    }

    @Test fun `InstanceSyncConfig retry defaults`() {
        val retry = InstanceSyncConfig.RetryConfig()
        assertTrue(retry.enabled)
        assertEquals(3, retry.maxAttempts)
        assertEquals(Duration.ofSeconds(1), retry.backoff)
    }

    @Test fun `XwalGrpcConfiguration defaults`() {
        val config = XwalGrpcConfiguration()
        assertEquals(50051, config.port)
        assertEquals("5m", config.keepAliveTime)
    }
}
