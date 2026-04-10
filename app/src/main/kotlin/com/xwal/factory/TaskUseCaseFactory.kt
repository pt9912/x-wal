package com.xwal.factory

import com.xwal.application.service.AdapterResolutionService
import com.xwal.application.usecase.task.*
import com.xwal.domain.port.input.*
import com.xwal.domain.port.output.EngineAdapterConfigRepository
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton

@Factory
class TaskUseCaseFactory {

    @Singleton
    fun queryTasksUseCase(
        adapterConfigRepository: EngineAdapterConfigRepository,
        adapterResolution: AdapterResolutionService
    ): QueryTasksUseCase =
        QueryTasksUseCaseImpl(adapterConfigRepository, adapterResolution)

    @Singleton
    fun completeTaskUseCase(adapterResolution: AdapterResolutionService): CompleteTaskUseCase =
        CompleteTaskUseCaseImpl(adapterResolution)

    @Singleton
    fun assignTaskUseCase(adapterResolution: AdapterResolutionService): AssignTaskUseCase =
        AssignTaskUseCaseImpl(adapterResolution)

    @Singleton
    fun getTaskUseCase(adapterResolution: AdapterResolutionService): GetTaskUseCase =
        GetTaskUseCaseImpl(adapterResolution)
}
