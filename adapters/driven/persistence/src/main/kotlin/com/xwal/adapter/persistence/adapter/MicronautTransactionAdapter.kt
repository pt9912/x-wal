package com.xwal.adapter.persistence.adapter

import com.xwal.domain.port.output.TransactionPort
import io.micronaut.transaction.SynchronousTransactionManager
import jakarta.inject.Singleton
import java.sql.Connection

@Singleton
class MicronautTransactionAdapter(
    private val transactionManager: SynchronousTransactionManager<Connection>
) : TransactionPort {

    override fun <T> executeInTransaction(block: () -> T): T {
        return transactionManager.executeWrite { _ -> block() }
    }
}
