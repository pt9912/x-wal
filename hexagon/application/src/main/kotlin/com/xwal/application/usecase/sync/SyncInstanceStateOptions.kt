package com.xwal.application.usecase.sync

import java.time.Duration

/**
 * Configuration options for instance state sync.
 * Pure data class — no framework dependencies.
 */
data class SyncInstanceStateOptions(
    val batchSize: Int = 100,
    val timeout: Duration = Duration.ofSeconds(30),
    val includeSuspended: Boolean = true,
    val retryEnabled: Boolean = true,
    val retryMaxAttempts: Int = 3,
    val retryBackoff: Duration = Duration.ofSeconds(1)
)
