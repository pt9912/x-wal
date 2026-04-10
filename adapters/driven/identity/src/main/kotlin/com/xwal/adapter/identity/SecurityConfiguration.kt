package com.xwal.adapter.identity

import io.micronaut.context.annotation.Factory
import io.micronaut.http.HttpRequest
import io.micronaut.security.token.jwt.validator.GenericJwtClaimsValidator
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Factory
class SecurityConfiguration {

    private val log = LoggerFactory.getLogger(SecurityConfiguration::class.java)

    @Singleton
    fun customJwtClaimsValidator(): GenericJwtClaimsValidator<HttpRequest<*>> {
        return GenericJwtClaimsValidator<HttpRequest<*>> { claims, _ ->
            val issuer = claims["iss"]?.toString() ?: ""
            val subject = claims["sub"]?.toString() ?: ""
            val valid = issuer.contains("realms/xwal") && subject.isNotBlank()
            if (!valid) {
                log.warn("JWT validation failed: issuer={}, subject={}", issuer, subject)
            }
            valid
        }
    }
}
