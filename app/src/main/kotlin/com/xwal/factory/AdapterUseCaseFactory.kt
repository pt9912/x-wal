package com.xwal.factory

import com.xwal.application.service.AdapterResolutionService
import com.xwal.application.usecase.adapter.*
import com.xwal.application.usecase.sync.SyncInstanceStateOptions
import com.xwal.application.usecase.sync.SyncInstanceStateUseCaseImpl
import com.xwal.config.InstanceSyncConfig
import com.xwal.domain.port.input.*
import com.xwal.domain.port.output.*
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton

@Factory
class AdapterUseCaseFactory {

    @Singleton
    fun registerAdapterUseCase(
        adapterConfigRepository: EngineAdapterConfigRepository,
        adapterResolution: AdapterResolutionService,
        adapterCache: AdapterInstanceCachePort,
        transactionPort: TransactionPort
    ): RegisterAdapterUseCase =
        RegisterAdapterUseCaseImpl(adapterConfigRepository, adapterResolution, adapterCache, transactionPort)

    @Singleton
    fun deregisterAdapterUseCase(
        adapterConfigRepository: EngineAdapterConfigRepository,
        adapterCache: AdapterInstanceCachePort,
        instanceRepository: InstanceRepository,
        transactionPort: TransactionPort
    ): DeregisterAdapterUseCase =
        DeregisterAdapterUseCaseImpl(adapterConfigRepository, adapterCache, instanceRepository, transactionPort)

    @Singleton
    fun healthCheckAdapterUseCase(
        adapterConfigRepository: EngineAdapterConfigRepository,
        adapterResolution: AdapterResolutionService
    ): HealthCheckAdapterUseCase =
        HealthCheckAdapterUseCaseImpl(adapterConfigRepository, adapterResolution)

    @Singleton
    fun syncInstanceStateUseCase(
        instanceRepository: InstanceRepository,
        adapterResolution: AdapterResolutionService,
        distributedLock: DistributedLockPort,
        syncConfig: InstanceSyncConfig
    ): SyncInstanceStateUseCase {
        val syncOptions = SyncInstanceStateOptions(
            batchSize = syncConfig.batchSize,
            timeout = syncConfig.timeout,
            includeSuspended = syncConfig.includeSuspended,
            retryEnabled = syncConfig.retry.enabled,
            retryMaxAttempts = syncConfig.retry.maxAttempts,
            retryBackoff = syncConfig.retry.backoff
        )

        return SyncInstanceStateUseCaseImpl(instanceRepository, adapterResolution, distributedLock, syncOptions)
    }
}
