package com.xwal.domain.port.input

interface SyncInstanceStateUseCase {
    fun execute(): SyncResult

    data class SyncResult(
        val synced: Int,
        val failed: Int,
        val skipped: Int
    )
}
