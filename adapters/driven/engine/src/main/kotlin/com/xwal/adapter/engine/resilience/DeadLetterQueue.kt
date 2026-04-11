package com.xwal.adapter.engine.resilience

import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * In-memory dead letter queue for failed adapter operations.
 * Failed operations are stored for later retry or manual inspection.
 */
@Singleton
class DeadLetterQueue {

    private val log = LoggerFactory.getLogger(DeadLetterQueue::class.java)
    private val queue = ConcurrentLinkedQueue<FailedOperation>()

    fun add(operation: FailedOperation) {
        queue.add(operation)
        log.warn("Added to DLQ: adapter={}, operation={}, error={}",
            operation.adapterName, operation.operationType, operation.errorMessage)
    }

    fun poll(): FailedOperation? = queue.poll()

    fun peek(): FailedOperation? = queue.peek()

    fun size(): Int = queue.size

    fun getAll(): List<FailedOperation> = queue.toList()

    fun clear() {
        val count = queue.size
        queue.clear()
        log.info("Cleared DLQ ({} items)", count)
    }
}

data class FailedOperation(
    val adapterName: String,
    val operationType: String,
    val errorMessage: String,
    val errorType: String,
    val timestamp: Instant = Instant.now(),
    val stackTrace: String? = null
)
