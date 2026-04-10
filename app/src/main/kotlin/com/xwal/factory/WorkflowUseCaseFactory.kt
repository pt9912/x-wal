package com.xwal.factory

import com.xwal.application.service.AdapterResolutionService
import com.xwal.application.usecase.workflow.*
import com.xwal.domain.port.input.*
import com.xwal.domain.port.output.*
import com.xwal.domain.service.IwmValidationService
import com.xwal.domain.validation.IwmValidator
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton

@Factory
class WorkflowUseCaseFactory {

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

    @Singleton
    fun createWorkflowUseCase(
        workflowRepository: WorkflowRepository,
        transactionPort: TransactionPort,
        iwmValidationService: IwmValidationService
    ): CreateWorkflowUseCase =
        CreateWorkflowUseCaseImpl(workflowRepository, transactionPort, iwmValidationService)

    @Singleton
    fun getWorkflowUseCase(workflowRepository: WorkflowRepository): GetWorkflowUseCase =
        GetWorkflowUseCaseImpl(workflowRepository)

    @Singleton
    fun listWorkflowsUseCase(workflowRepository: WorkflowRepository): ListWorkflowsUseCase =
        ListWorkflowsUseCaseImpl(workflowRepository)

    @Singleton
    fun updateWorkflowUseCase(
        workflowRepository: WorkflowRepository,
        transactionPort: TransactionPort
    ): UpdateWorkflowUseCase =
        UpdateWorkflowUseCaseImpl(workflowRepository, transactionPort)

    @Singleton
    fun deleteWorkflowUseCase(
        workflowRepository: WorkflowRepository,
        transactionPort: TransactionPort
    ): DeleteWorkflowUseCase =
        DeleteWorkflowUseCaseImpl(workflowRepository, transactionPort)

    @Singleton
    fun startWorkflowUseCase(
        workflowRepository: WorkflowRepository,
        instanceRepository: InstanceRepository,
        adapterConfigRepository: EngineAdapterConfigRepository,
        adapterResolution: AdapterResolutionService,
        transactionPort: TransactionPort
    ): StartWorkflowUseCase =
        StartWorkflowUseCaseImpl(workflowRepository, instanceRepository, adapterConfigRepository, adapterResolution, transactionPort)

    @Singleton
    fun getInstanceUseCase(instanceRepository: InstanceRepository): GetInstanceUseCase =
        GetInstanceUseCaseImpl(instanceRepository)

    @Singleton
    fun getInstanceVariablesUseCase(
        instanceRepository: InstanceRepository,
        adapterResolution: AdapterResolutionService
    ): GetInstanceVariablesUseCase =
        GetInstanceVariablesUseCaseImpl(instanceRepository, adapterResolution)

    @Singleton
    fun suspendInstanceUseCase(
        instanceRepository: InstanceRepository,
        adapterResolution: AdapterResolutionService,
        transactionPort: TransactionPort
    ): SuspendInstanceUseCase =
        SuspendInstanceUseCaseImpl(instanceRepository, adapterResolution, transactionPort)

    @Singleton
    fun resumeInstanceUseCase(
        instanceRepository: InstanceRepository,
        adapterResolution: AdapterResolutionService,
        transactionPort: TransactionPort
    ): ResumeInstanceUseCase =
        ResumeInstanceUseCaseImpl(instanceRepository, adapterResolution, transactionPort)

    @Singleton
    fun cancelInstanceUseCase(
        instanceRepository: InstanceRepository,
        adapterResolution: AdapterResolutionService,
        transactionPort: TransactionPort
    ): CancelInstanceUseCase =
        CancelInstanceUseCaseImpl(instanceRepository, adapterResolution, transactionPort)
}
