package com.xwal.adapter.identity

import io.grpc.*
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * gRPC server interceptor that extracts and validates JWT Bearer tokens
 * from the Authorization metadata header.
 */
@Singleton
class GrpcJwtAuthInterceptor : ServerInterceptor {

    private val log = LoggerFactory.getLogger(GrpcJwtAuthInterceptor::class.java)

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val authHeader = headers.get(AUTHORIZATION_KEY)

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("gRPC call without valid Authorization header to {}", call.methodDescriptor.fullMethodName)
            call.close(Status.UNAUTHENTICATED.withDescription("Missing or invalid Authorization header"), Metadata())
            return object : ServerCall.Listener<ReqT>() {}
        }

        log.debug("gRPC JWT token present for {}", call.methodDescriptor.fullMethodName)
        return next.startCall(call, headers)
    }

    companion object {
        private val AUTHORIZATION_KEY: Metadata.Key<String> =
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER)
    }
}
