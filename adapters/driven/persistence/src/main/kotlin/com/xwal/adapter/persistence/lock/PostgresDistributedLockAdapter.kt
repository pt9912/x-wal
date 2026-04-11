package com.xwal.adapter.persistence.lock

import com.xwal.domain.port.output.DistributedLockPort
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.time.Duration
import kotlin.math.min
import javax.sql.DataSource

/**
 * Implements DistributedLockPort using PostgreSQL Advisory Locks.
 * Uses two-argument advisory locks (classId, objId) to avoid hash collisions.
 * Only withLock() is reliable — tryAcquire/release pair cannot work across
 * different connections (advisory locks are session-scoped).
 */
@Singleton
class PostgresDistributedLockAdapter(
    private val dataSource: DataSource
) : DistributedLockPort {

    private val log = LoggerFactory.getLogger(PostgresDistributedLockAdapter::class.java)

    override fun <T> withLock(lockKey: String, timeout: Duration, block: () -> T): T {
        val (classId, objId) = lockIds(lockKey)
        val connection = dataSource.connection
        var acquired = false

        return try {
            acquired = acquireWithTimeout(connection, lockKey, classId, objId, timeout)
            if (!acquired) {
                throw IllegalStateException("Could not acquire lock: $lockKey")
            }
            log.debug("Acquired advisory lock: {} (class={}, obj={})", lockKey, classId, objId)
            block()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IllegalStateException("Interrupted while waiting for lock: $lockKey", e)
        } finally {
            if (acquired) {
                releaseAdvisoryLock(connection, classId, objId)
                log.debug("Released advisory lock: {}", lockKey)
            }
            connection.close()
        }
    }

    override fun tryAcquire(lockKey: String, timeout: Duration): Boolean {
        // Note: advisory locks are session-scoped. tryAcquire/release across
        // different calls will use different connections and NOT work correctly.
        // Use withLock() instead for reliable locking.
        log.warn("tryAcquire() is unreliable with pooled connections — use withLock() instead")
        val (classId, objId) = lockIds(lockKey)
        val connection = dataSource.connection
        return try {
            acquireWithTimeout(connection, lockKey, classId, objId, timeout)
        } catch (e: Exception) {
            log.warn("Failed to acquire advisory lock {}: {}", lockKey, e.message)
            false
        } finally {
            connection.close()
        }
    }

    override fun release(lockKey: String) {
        log.warn("release() is unreliable with pooled connections — use withLock() instead")
        val (classId, objId) = lockIds(lockKey)
        val connection = dataSource.connection
        try {
            releaseAdvisoryLock(connection, classId, objId)
        } finally {
            connection.close()
        }
    }

    private fun tryAdvisoryLock(connection: Connection, classId: Int, objId: Int): Boolean {
        connection.prepareStatement("SELECT pg_try_advisory_lock(?, ?)").use { stmt ->
            stmt.setInt(1, classId)
            stmt.setInt(2, objId)
            stmt.executeQuery().use { rs -> return rs.next() && rs.getBoolean(1) }
        }
    }

    private fun acquireWithTimeout(
        connection: Connection,
        lockKey: String,
        classId: Int,
        objId: Int,
        timeout: Duration
    ): Boolean {
        if (timeout.isZero || timeout.isNegative) {
            return tryAdvisoryLock(connection, classId, objId)
        }

        val deadlineNanos = System.nanoTime() + timeout.toNanos()
        while (true) {
            if (tryAdvisoryLock(connection, classId, objId)) {
                return true
            }

            val remainingNanos = deadlineNanos - System.nanoTime()
            if (remainingNanos <= 0) {
                log.debug("Timed out waiting for advisory lock: {}", lockKey)
                return false
            }

            val sleepNanos = min(remainingNanos, LOCK_POLL_INTERVAL.toNanos())
            if (sleepNanos > 0) {
                Thread.sleep(
                    sleepNanos / NANO_PER_MILLI,
                    (sleepNanos % NANO_PER_MILLI).toInt()
                )
            }
        }
    }

    private fun releaseAdvisoryLock(connection: Connection, classId: Int, objId: Int) {
        connection.prepareStatement("SELECT pg_advisory_unlock(?, ?)").use { stmt ->
            stmt.setInt(1, classId)
            stmt.setInt(2, objId)
            stmt.executeQuery().use { it.next() }
        }
    }

    /** Split lock key into two 32-bit IDs to avoid collisions */
    private fun lockIds(lockKey: String): Pair<Int, Int> {
        val hash = lockKey.hashCode().toLong()
        val classId = XWAL_LOCK_CLASS
        val objId = hash.toInt()
        return classId to objId
    }

    companion object {
        /** Fixed class ID for all x-wal advisory locks */
        private const val XWAL_LOCK_CLASS = 0x5857414C // "XWAL" as int
        private val LOCK_POLL_INTERVAL: Duration = Duration.ofMillis(100)
        private const val NANO_PER_MILLI: Long = 1_000_000
    }
}
