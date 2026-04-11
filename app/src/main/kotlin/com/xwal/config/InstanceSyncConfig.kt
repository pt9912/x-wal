package com.xwal.config

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("xwal.instance-sync")
class InstanceSyncConfig {
    var enabled: Boolean = true
    var batchSize: Int = 100
    var timeout: String = "5s"
    var includeSuspended: Boolean = true

    var retry = RetryConfig()

    @ConfigurationProperties("retry")
    class RetryConfig {
        var enabled: Boolean = true
        var maxAttempts: Int = 3
        var backoff: String = "1s"
    }
}
