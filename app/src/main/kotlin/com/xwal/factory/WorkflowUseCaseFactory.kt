package com.xwal.factory

import com.xwal.application.service.AdapterResolutionService
import com.xwal.application.usecase.workflow.*
import com.xwal.decorator.MeteredUseCaseDecorator
import com.xwal.decorator.TracedUseCaseDecorator
import com.xwal.domain.model.Workflow
import com.xwal.domain.model.WorkflowInstance
import com.xwal.domain.port.input.*
import com.xwal.domain.port.output.*
import com.xwal.domain.service.IwmValidationService
import io.micronaut.context.annotation.Factory
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Tracer
import jakarta.inject.Singleton

@Factory
class WorkflowUseCaseFactory {

    @Singleton
    fun createWorkflowUseCase(
        workflowRepository: WorkflowRepository,
        transactionPort: TransactionPort,
        iwmValidationService: IwmValidationService,
        tracer: Tracer, meter: Meter
    ): CreateWorkflowUseCase {
        val impl = CreateWorkflowUseCaseImpl(workflowRepository, transactionPort, iwmValidationService)
        val traced = TracedUseCaseDecorator<CreateWorkflowUseCase.Command, Workflow>(tracer, "workflow.create") { impl.execute(it) }
        val metered = MeteredUseCaseDecorator<CreateWorkflowUseCase.Command, Workflow>(meter, "workflow.create") { traced.execute(it) }
        return object : CreateWorkflowUseCase { override fun execute(command: CreateWorkflowUseCase.Command) = metered.execute(command) }
    }

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
        transactionPort: TransactionPort,
        tracer: Tracer, meter: Meter
    ): StartWorkflowUseCase {
        val impl = StartWorkflowUseCaseImpl(workflowRepository, instanceRepository, adapterConfigRepository, adapterResolution, transactionPort)
        val traced = TracedUseCaseDecorator<StartWorkflowUseCase.Command, WorkflowInstance>(tracer, "workflow.start") { impl.execute(it) }
        val metered = MeteredUseCaseDecorator<StartWorkflowUseCase.Command, WorkflowInstance>(meter, "workflow.start") { traced.execute(it) }
        return object : StartWorkflowUseCase { override fun execute(command: StartWorkflowUseCase.Command) = metered.execute(command) }
    }

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
        transactionPort: TransactionPort,
        tracer: Tracer, meter: Meter
    ): SuspendInstanceUseCase {
        val impl = SuspendInstanceUseCaseImpl(instanceRepository, adapterResolution, transactionPort)
        val traced = TracedUseCaseDecorator<com.xwal.domain.model.InstanceId, WorkflowInstance>(tracer, "instance.suspend") { impl.execute(it) }
        val metered = MeteredUseCaseDecorator<com.xwal.domain.model.InstanceId, WorkflowInstance>(meter, "instance.suspend") { traced.execute(it) }
        return object : SuspendInstanceUseCase { override fun execute(instanceId: com.xwal.domain.model.InstanceId) = metered.execute(instanceId) }
    }

    @Singleton
    fun resumeInstanceUseCase(
        instanceRepository: InstanceRepository,
        adapterResolution: AdapterResolutionService,
        transactionPort: TransactionPort,
        tracer: Tracer, meter: Meter
    ): ResumeInstanceUseCase {
        val impl = ResumeInstanceUseCaseImpl(instanceRepository, adapterResolution, transactionPort)
        val traced = TracedUseCaseDecorator<com.xwal.domain.model.InstanceId, WorkflowInstance>(tracer, "instance.resume") { impl.execute(it) }
        val metered = MeteredUseCaseDecorator<com.xwal.domain.model.InstanceId, WorkflowInstance>(meter, "instance.resume") { traced.execute(it) }
        return object : ResumeInstanceUseCase { override fun execute(instanceId: com.xwal.domain.model.InstanceId) = metered.execute(instanceId) }
    }

    @Singleton
    fun cancelInstanceUseCase(
        instanceRepository: InstanceRepository,
        adapterResolution: AdapterResolutionService,
        transactionPort: TransactionPort,
        tracer: Tracer, meter: Meter
    ): CancelInstanceUseCase {
        val impl = CancelInstanceUseCaseImpl(instanceRepository, adapterResolution, transactionPort)
        return object : CancelInstanceUseCase {
            override fun execute(instanceId: com.xwal.domain.model.InstanceId, reason: String?): WorkflowInstance {
                val span = tracer.spanBuilder("instance.cancel").startSpan()
                val scope = span.makeCurrent()
                return try {
                    meter.counterBuilder("instance.cancel.total").build().add(1)
                    impl.execute(instanceId, reason)
                } catch (e: Exception) {
                    span.recordException(e); throw e
                } finally { scope.close(); span.end() }
            }
        }
    }
}
