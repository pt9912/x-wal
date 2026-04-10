package com.xwal.domain.port.output

interface TransactionPort {
    fun <T> executeInTransaction(block: () -> T): T
}
