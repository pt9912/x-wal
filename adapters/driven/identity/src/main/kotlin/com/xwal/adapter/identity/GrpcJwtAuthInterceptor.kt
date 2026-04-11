package com.xwal.adapter.identity

import io.grpc.*
import io.micronaut.http.HttpRequest
import io.micronaut.security.token.jwt.validator.JwtTokenValidator
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux

/**
 * gRPC server interceptor that validates JWT Bearer tokens.
 * Uses Micronaut's JwtTokenValidator for signature and claims validation.
 * Micronaut gRPC auto-discovers @Singleton ServerInterceptor beans.
 */
@Singleton
class GrpcJwtAuthInterceptor(
    private val jwtTokenValidator: JwtTokenValidator<HttpRequest<*>>
) : ServerInterceptor {

    private val log = LoggerFactory.getLogger(GrpcJwtAuthInterceptor::class.java)

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val authHeader = headers.get(AUTHORIZATION_KEY)

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("gRPC call without Authorization to {}", call.methodDescriptor.fullMethodName)
            call.close(Status.UNAUTHENTICATED.withDescription("Missing or invalid Authorization header"), Metadata())
            return object : ServerCall.Listener<ReqT>() {}
        }

        val token = authHeader.removePrefix("Bearer ").trim()

        val authentication = try {
            Flux.from(jwtTokenValidator.validateToken(token, null)).blockFirst()
        } catch (e: Exception) {
            log.warn("JWT validation error for gRPC call: {}", e.message)
            null
        }

        if (authentication == null) {
            log.warn("Invalid JWT for gRPC call to {}", call.methodDescriptor.fullMethodName)
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid or expired JWT token"), Metadata())
            return object : ServerCall.Listener<ReqT>() {}
        }

        log.debug("gRPC JWT validated for {} (subject={})", call.methodDescriptor.fullMethodName, authentication.name)
        return next.startCall(call, headers)
    }

    companion object {
        private val AUTHORIZATION_KEY: Metadata.Key<String> =
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER)
    }
}
