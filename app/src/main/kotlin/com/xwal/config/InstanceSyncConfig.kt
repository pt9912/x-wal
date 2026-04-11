package com.xwal.config

import io.micronaut.context.annotation.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("xwal.instance-sync")
class InstanceSyncConfig {
    var enabled: Boolean = true
    var batchSize: Int = 100
    var timeout: Duration = Duration.ofSeconds(5)
    var includeSuspended: Boolean = true

    var retry = RetryConfig()

    @ConfigurationProperties("retry")
    class RetryConfig {
        var enabled: Boolean = true
        var maxAttempts: Int = 3
        var backoff: Duration = Duration.ofSeconds(1)
    }
}
