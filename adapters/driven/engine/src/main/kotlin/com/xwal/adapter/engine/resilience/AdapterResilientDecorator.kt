package com.xwal.adapter.engine.resilience

import com.xwal.domain.exception.AdapterOperationException
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.retry.Retry
import org.slf4j.LoggerFactory
import java.util.function.Supplier

object AdapterResilientDecorator {

    private val log = LoggerFactory.getLogger(AdapterResilientDecorator::class.java)

    fun <T> execute(
        adapterName: String,
        config: Resilience4jConfig,
        supplier: Supplier<T>
    ): T {
        val circuitBreaker = config.getCircuitBreaker(adapterName)
        val retry = config.getRetry(adapterName)

        val decorated = Retry.decorateSupplier(retry,
            CircuitBreaker.decorateSupplier(circuitBreaker, supplier)
        )

        return try {
            decorated.get()
        } catch (e: CallNotPermittedException) {
            log.error("Circuit breaker OPEN for adapter {}", adapterName)
            throw AdapterOperationException(
                engineType = adapterName, operation = "execute",
                message = "Circuit breaker is open for $adapterName", cause = e
            )
        } catch (e: Exception) {
            log.error("Resilient execution failed for {}: {}", adapterName, e.message)
            throw if (e is AdapterOperationException) e
            else AdapterOperationException(
                engineType = adapterName, operation = "execute",
                message = "Operation failed after retries: ${e.message}", cause = e
            )
        }
    }

    fun executeVoid(
        adapterName: String,
        config: Resilience4jConfig,
        runnable: Runnable
    ) {
        execute(adapterName, config, Supplier { runnable.run(); null })
    }
}
