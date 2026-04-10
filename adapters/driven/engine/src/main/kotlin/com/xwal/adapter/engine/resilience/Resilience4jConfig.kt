package com.xwal.adapter.engine.resilience

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import io.micronaut.http.client.exceptions.HttpClientException
import org.slf4j.LoggerFactory
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.time.Duration
import java.util.concurrent.TimeoutException

class Resilience4jConfig {

    private val log = LoggerFactory.getLogger(Resilience4jConfig::class.java)

    val circuitBreakerRegistry: CircuitBreakerRegistry = CircuitBreakerRegistry.of(
        CircuitBreakerConfig.custom()
            .failureRateThreshold(50.0f)
            .slidingWindowSize(10)
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .minimumNumberOfCalls(5)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .permittedNumberOfCallsInHalfOpenState(3)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .recordExceptions(
                ConnectException::class.java,
                SocketTimeoutException::class.java,
                TimeoutException::class.java,
                HttpClientException::class.java
            )
            .ignoreExceptions(
                IllegalArgumentException::class.java,
                IllegalStateException::class.java
            )
            .build()
    )

    val retryRegistry: RetryRegistry = RetryRegistry.of(
        RetryConfig.custom<Any>()
            .maxAttempts(3)
            .waitDuration(Duration.ofSeconds(1))
            .retryOnException { throwable ->
                val root = generateSequence(throwable) { it.cause }.last()
                root is ConnectException || root is SocketTimeoutException ||
                    root is TimeoutException || root is HttpClientException
            }
            .ignoreExceptions(
                IllegalArgumentException::class.java,
                IllegalStateException::class.java
            )
            .build()
    )

    fun getCircuitBreaker(adapterName: String): CircuitBreaker =
        circuitBreakerRegistry.circuitBreaker(adapterName)

    fun getRetry(adapterName: String): Retry =
        retryRegistry.retry(adapterName)
}
