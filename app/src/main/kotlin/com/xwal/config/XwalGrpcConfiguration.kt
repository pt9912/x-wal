package com.xwal.config

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("grpc.server")
class XwalGrpcConfiguration {
    var port: Int = 50051
    var keepAliveTime: String = "5m"
}
