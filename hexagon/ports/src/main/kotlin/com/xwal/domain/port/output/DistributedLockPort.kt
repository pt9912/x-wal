package com.xwal.domain.port.output

import java.time.Duration

interface DistributedLockPort {
    fun <T> withLock(lockKey: String, timeout: Duration = Duration.ofSeconds(30), block: () -> T): T
    fun tryAcquire(lockKey: String, timeout: Duration = Duration.ofSeconds(30)): Boolean
    fun release(lockKey: String)
}
