package com.xwal.factory

import com.xwal.application.service.AdapterResolutionService
import com.xwal.application.usecase.adapter.*
import com.xwal.application.usecase.sync.SyncInstanceStateUseCaseImpl
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
        transactionPort: TransactionPort
    ): RegisterAdapterUseCase =
        RegisterAdapterUseCaseImpl(adapterConfigRepository, adapterResolution, transactionPort)

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
        @io.micronaut.context.annotation.Value("\${xwal.instance-sync.batch-size:100}") batchSize: Int
    ): SyncInstanceStateUseCase =
        SyncInstanceStateUseCaseImpl(instanceRepository, adapterResolution, batchSize)
}
