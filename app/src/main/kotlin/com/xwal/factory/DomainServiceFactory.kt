package com.xwal.factory

import com.xwal.application.service.AdapterResolutionService
import com.xwal.domain.port.output.AdapterInstanceCachePort
import com.xwal.domain.port.output.EngineAdapterConfigRepository
import com.xwal.domain.port.output.EngineAdapterFactoryPort
import com.xwal.domain.service.IwmValidationService
import com.xwal.domain.validation.IwmValidator
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton

@Factory
class DomainServiceFactory {

    @Singleton
    fun iwmValidationService(): IwmValidationService =
        IwmValidationService(IwmValidator())

    @Singleton
    fun adapterResolutionService(
        adapterConfigRepository: EngineAdapterConfigRepository,
        adapterCache: AdapterInstanceCachePort,
        adapterFactory: EngineAdapterFactoryPort
    ): AdapterResolutionService =
        AdapterResolutionService(adapterConfigRepository, adapterCache, adapterFactory)
}
