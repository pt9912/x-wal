package com.xwal.factory

import com.xwal.application.service.AdapterResolutionService
import com.xwal.application.usecase.adapter.*
import com.xwal.application.usecase.sync.SyncInstanceStateOptions
import com.xwal.application.usecase.sync.SyncInstanceStateUseCaseImpl
import com.xwal.config.InstanceSyncConfig
import com.xwal.decorator.MeteredUseCaseDecorator
import com.xwal.decorator.TracedUseCaseDecorator
import com.xwal.domain.model.EngineAdapterConfig
import com.xwal.domain.model.EngineAdapterId
import com.xwal.domain.port.input.*
import com.xwal.domain.port.output.*
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Tracer
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton

@Factory
class AdapterUseCaseFactory {

    @Singleton
    fun registerAdapterUseCase(
        adapterConfigRepository: EngineAdapterConfigRepository,
        adapterResolution: AdapterResolutionService,
        adapterCache: AdapterInstanceCachePort,
        transactionPort: TransactionPort,
        tracer: Tracer,
        meter: Meter
    ): RegisterAdapterUseCase {
        val impl = RegisterAdapterUseCaseImpl(adapterConfigRepository, adapterResolution, adapterCache, transactionPort)
        val traced = TracedUseCaseDecorator<RegisterAdapterUseCase.Command, EngineAdapterConfig>(
            tracer,
            "adapter.register"
        ) { impl.execute(it) }
        val metered = MeteredUseCaseDecorator<RegisterAdapterUseCase.Command, EngineAdapterConfig>(
            meter,
            "adapter.register"
        ) { traced.execute(it) }
        return object : RegisterAdapterUseCase { override fun execute(command: RegisterAdapterUseCase.Command) = metered.execute(command) }
    }

    @Singleton
    fun deregisterAdapterUseCase(
        adapterConfigRepository: EngineAdapterConfigRepository,
        adapterCache: AdapterInstanceCachePort,
        instanceRepository: InstanceRepository,
        transactionPort: TransactionPort,
        tracer: Tracer,
        meter: Meter
    ): DeregisterAdapterUseCase {
        val impl = DeregisterAdapterUseCaseImpl(adapterConfigRepository, adapterCache, instanceRepository, transactionPort)
        val traced = TracedUseCaseDecorator<EngineAdapterId, Unit>(tracer, "adapter.deregister") { impl.execute(it) }
        val metered = MeteredUseCaseDecorator<EngineAdapterId, Unit>(meter, "adapter.deregister") { traced.execute(it) }
        return object : DeregisterAdapterUseCase { override fun execute(adapterId: EngineAdapterId) = metered.execute(adapterId) }
    }

    @Singleton
    fun healthCheckAdapterUseCase(
        adapterConfigRepository: EngineAdapterConfigRepository,
        adapterResolution: AdapterResolutionService,
        tracer: Tracer,
        meter: Meter
    ): HealthCheckAdapterUseCase {
        val impl = HealthCheckAdapterUseCaseImpl(adapterConfigRepository, adapterResolution)
        val tracedSingle = TracedUseCaseDecorator<EngineAdapterId, EngineAdapterConfig>(
            tracer,
            "adapter.healthcheck"
        ) { impl.executeSingle(it) }
        val meteredSingle = MeteredUseCaseDecorator<EngineAdapterId, EngineAdapterConfig>(
            meter,
            "adapter.healthcheck"
        ) { tracedSingle.execute(it) }

        val tracedAll = TracedUseCaseDecorator<Unit, List<EngineAdapterConfig>>(
            tracer,
            "adapter.healthcheck"
        ) { impl.executeAll() }
        val meteredAll = MeteredUseCaseDecorator<Unit, List<EngineAdapterConfig>>(
            meter,
            "adapter.healthcheck"
        ) { tracedAll.execute(it) }

        return object : HealthCheckAdapterUseCase {
            override fun executeSingle(adapterId: EngineAdapterId): EngineAdapterConfig = meteredSingle.execute(adapterId)
            override fun executeAll(): List<EngineAdapterConfig> = meteredAll.execute(Unit)
        }
    }

    @Singleton
    fun syncInstanceStateUseCase(
        instanceRepository: InstanceRepository,
        adapterResolution: AdapterResolutionService,
        distributedLock: DistributedLockPort,
        syncConfig: InstanceSyncConfig,
        tracer: Tracer,
        meter: Meter
    ): SyncInstanceStateUseCase {
        val syncOptions = SyncInstanceStateOptions(
            batchSize = syncConfig.batchSize,
            timeout = syncConfig.timeout,
            includeSuspended = syncConfig.includeSuspended,
            retryEnabled = syncConfig.retry.enabled,
            retryMaxAttempts = syncConfig.retry.maxAttempts,
            retryBackoff = syncConfig.retry.backoff
        )

        val impl = SyncInstanceStateUseCaseImpl(instanceRepository, adapterResolution, distributedLock, syncOptions)
        val traced = TracedUseCaseDecorator<Unit, SyncInstanceStateUseCase.SyncResult>(
            tracer,
            "adapter.sync_instances"
        ) { impl.execute() }
        val metered = MeteredUseCaseDecorator<Unit, SyncInstanceStateUseCase.SyncResult>(
            meter,
            "adapter.sync_instances"
        ) { traced.execute(it) }
        return object : SyncInstanceStateUseCase { override fun execute() = metered.execute(Unit) }
    }
}
