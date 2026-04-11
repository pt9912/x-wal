package com.xwal.adapter.engine.config

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("xwal.http-client.pool")
class HttpClientPoolConfig {
    var maxConnectionsPerHost: Int = 10
    var maxConnections: Int = 50
    var connectionTimeout: String = "10s"
    var readTimeout: String = "30s"
    var idleTimeout: String = "60s"
    var connectionTtl: String = "5m"
}
