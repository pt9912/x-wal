package com.xwal.adapter.persistence.lock

import com.xwal.domain.port.output.DistributedLockPort
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.time.Duration
import javax.sql.DataSource

/**
 * Implements DistributedLockPort using PostgreSQL Advisory Locks.
 * Advisory locks are session-level and automatically released on disconnect.
 */
@Singleton
class PostgresDistributedLockAdapter(
    private val dataSource: DataSource
) : DistributedLockPort {

    private val log = LoggerFactory.getLogger(PostgresDistributedLockAdapter::class.java)

    override fun <T> withLock(lockKey: String, timeout: Duration, block: () -> T): T {
        val lockId = lockKey.hashCode().toLong()
        val connection = dataSource.connection
        return try {
            if (!tryAdvisoryLock(connection, lockId)) {
                throw IllegalStateException("Could not acquire lock: $lockKey")
            }
            log.debug("Acquired advisory lock: {} (id={})", lockKey, lockId)
            block()
        } finally {
            releaseAdvisoryLock(connection, lockId)
            connection.close()
            log.debug("Released advisory lock: {} (id={})", lockKey, lockId)
        }
    }

    override fun tryAcquire(lockKey: String, timeout: Duration): Boolean {
        val lockId = lockKey.hashCode().toLong()
        val connection = dataSource.connection
        return try {
            tryAdvisoryLock(connection, lockId)
        } catch (e: Exception) {
            log.warn("Failed to acquire advisory lock {}: {}", lockKey, e.message)
            false
        }
    }

    override fun release(lockKey: String) {
        val lockId = lockKey.hashCode().toLong()
        val connection = dataSource.connection
        try {
            releaseAdvisoryLock(connection, lockId)
        } finally {
            connection.close()
        }
    }

    private fun tryAdvisoryLock(connection: Connection, lockId: Long): Boolean {
        val stmt = connection.prepareStatement("SELECT pg_try_advisory_lock(?)")
        stmt.setLong(1, lockId)
        val rs = stmt.executeQuery()
        return rs.next() && rs.getBoolean(1)
    }

    private fun releaseAdvisoryLock(connection: Connection, lockId: Long) {
        val stmt = connection.prepareStatement("SELECT pg_advisory_unlock(?)")
        stmt.setLong(1, lockId)
        stmt.executeQuery()
    }
}
