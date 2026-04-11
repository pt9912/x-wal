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
        supplier: Supplier<T>,
        operationType: String = "unknown",
        dlq: DeadLetterQueue? = null
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
            val ex = AdapterOperationException(adapterName, operationType, "Circuit breaker is open for $adapterName", e)
            dlq?.add(FailedOperation(adapterName, operationType, ex.message, "CIRCUIT_OPEN"))
            throw ex
        } catch (e: Exception) {
            log.error("Resilient execution failed for {}: {}", adapterName, e.message)
            val ex = if (e is AdapterOperationException) e
                else AdapterOperationException(adapterName, operationType, "Operation failed after retries: ${e.message}", e)
            dlq?.add(FailedOperation(adapterName, operationType, ex.message, e.javaClass.simpleName))
            throw ex
        }
    }

    fun executeVoid(
        adapterName: String,
        config: Resilience4jConfig,
        runnable: Runnable,
        operationType: String = "unknown",
        dlq: DeadLetterQueue? = null
    ) {
        execute(adapterName, config, Supplier { runnable.run(); null }, operationType, dlq)
    }
}
