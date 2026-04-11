package com.xwal.factory

import com.xwal.application.service.AdapterResolutionService
import com.xwal.application.usecase.workflow.*
import com.xwal.decorator.MeteredUseCaseDecorator
import com.xwal.decorator.TracedUseCaseDecorator
import com.xwal.domain.model.InstanceId
import com.xwal.domain.model.Workflow
import com.xwal.domain.model.WorkflowId
import com.xwal.domain.model.WorkflowInstance
import com.xwal.domain.model.WorkflowStatus
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
        tracer: Tracer,
        meter: Meter
    ): CreateWorkflowUseCase {
        val impl = CreateWorkflowUseCaseImpl(workflowRepository, transactionPort, iwmValidationService)
        val traced = TracedUseCaseDecorator<CreateWorkflowUseCase.Command, Workflow>(tracer, "workflow.create") { impl.execute(it) }
        val metered = MeteredUseCaseDecorator<CreateWorkflowUseCase.Command, Workflow>(meter, "workflow.create") { traced.execute(it) }
        return object : CreateWorkflowUseCase { override fun execute(command: CreateWorkflowUseCase.Command) = metered.execute(command) }
    }

    @Singleton
    fun getWorkflowUseCase(
        workflowRepository: WorkflowRepository,
        tracer: Tracer,
        meter: Meter
    ): GetWorkflowUseCase {
        val impl = GetWorkflowUseCaseImpl(workflowRepository)
        val traced = TracedUseCaseDecorator<WorkflowId, Workflow>(tracer, "workflow.get") { impl.execute(it) }
        val metered = MeteredUseCaseDecorator<WorkflowId, Workflow>(meter, "workflow.get") { traced.execute(it) }
        return object : GetWorkflowUseCase { override fun execute(workflowId: WorkflowId) = metered.execute(workflowId) }
    }

    @Singleton
    fun listWorkflowsUseCase(
        workflowRepository: WorkflowRepository,
        tracer: Tracer,
        meter: Meter
    ): ListWorkflowsUseCase {
        val impl = ListWorkflowsUseCaseImpl(workflowRepository)
        val traced = TracedUseCaseDecorator<WorkflowStatus?, List<Workflow>>(tracer, "workflow.list") { impl.execute(it) }
        val metered = MeteredUseCaseDecorator<WorkflowStatus?, List<Workflow>>(meter, "workflow.list") { traced.execute(it) }
        return object : ListWorkflowsUseCase { override fun execute(status: WorkflowStatus?) = metered.execute(status) }
    }

    @Singleton
    fun updateWorkflowUseCase(
        workflowRepository: WorkflowRepository,
        transactionPort: TransactionPort,
        tracer: Tracer,
        meter: Meter
    ): UpdateWorkflowUseCase {
        val impl = UpdateWorkflowUseCaseImpl(workflowRepository, transactionPort)
        val traced = TracedUseCaseDecorator<UpdateWorkflowUseCase.Command, Workflow>(tracer, "workflow.update") { impl.execute(it) }
        val metered = MeteredUseCaseDecorator<UpdateWorkflowUseCase.Command, Workflow>(meter, "workflow.update") { traced.execute(it) }
        return object : UpdateWorkflowUseCase { override fun execute(command: UpdateWorkflowUseCase.Command) = metered.execute(command) }
    }

    @Singleton
    fun deleteWorkflowUseCase(
        workflowRepository: WorkflowRepository,
        transactionPort: TransactionPort,
        tracer: Tracer,
        meter: Meter
    ): DeleteWorkflowUseCase {
        val impl = DeleteWorkflowUseCaseImpl(workflowRepository, transactionPort)
        val traced = TracedUseCaseDecorator<WorkflowId, Unit>(tracer, "workflow.delete") { impl.execute(it) }
        val metered = MeteredUseCaseDecorator<WorkflowId, Unit>(meter, "workflow.delete") { traced.execute(it) }
        return object : DeleteWorkflowUseCase { override fun execute(workflowId: WorkflowId) = metered.execute(workflowId) }
    }

    @Singleton
    fun startWorkflowUseCase(
        workflowRepository: WorkflowRepository,
        instanceRepository: InstanceRepository,
        adapterConfigRepository: EngineAdapterConfigRepository,
        adapterResolution: AdapterResolutionService,
        transactionPort: TransactionPort,
        tracer: Tracer,
        meter: Meter
    ): StartWorkflowUseCase {
        val impl = StartWorkflowUseCaseImpl(workflowRepository, instanceRepository, adapterConfigRepository, adapterResolution, transactionPort)
        val traced = TracedUseCaseDecorator<StartWorkflowUseCase.Command, WorkflowInstance>(tracer, "workflow.start") { impl.execute(it) }
        val metered = MeteredUseCaseDecorator<StartWorkflowUseCase.Command, WorkflowInstance>(meter, "workflow.start") { traced.execute(it) }
        return object : StartWorkflowUseCase { override fun execute(command: StartWorkflowUseCase.Command) = metered.execute(command) }
    }

    @Singleton
    fun getInstanceUseCase(
        instanceRepository: InstanceRepository,
        tracer: Tracer,
        meter: Meter
    ): GetInstanceUseCase {
        val impl = GetInstanceUseCaseImpl(instanceRepository)
        val traced = TracedUseCaseDecorator<InstanceId, WorkflowInstance>(tracer, "instance.get") { impl.execute(it) }
        val metered = MeteredUseCaseDecorator<InstanceId, WorkflowInstance>(meter, "instance.get") { traced.execute(it) }
        return object : GetInstanceUseCase { override fun execute(instanceId: InstanceId) = metered.execute(instanceId) }
    }

    @Singleton
    fun getInstanceVariablesUseCase(
        instanceRepository: InstanceRepository,
        adapterResolution: AdapterResolutionService,
        tracer: Tracer,
        meter: Meter
    ): GetInstanceVariablesUseCase {
        val impl = GetInstanceVariablesUseCaseImpl(instanceRepository, adapterResolution)
        val traced = TracedUseCaseDecorator<InstanceId, Map<String, Any>>(tracer, "instance.variables") { impl.execute(it) }
        val metered = MeteredUseCaseDecorator<InstanceId, Map<String, Any>>(meter, "instance.variables") { traced.execute(it) }
        return object : GetInstanceVariablesUseCase { override fun execute(instanceId: InstanceId) = metered.execute(instanceId) }
    }

    @Singleton
    fun suspendInstanceUseCase(
        instanceRepository: InstanceRepository,
        adapterResolution: AdapterResolutionService,
        transactionPort: TransactionPort,
        tracer: Tracer,
        meter: Meter
    ): SuspendInstanceUseCase {
        val impl = SuspendInstanceUseCaseImpl(instanceRepository, adapterResolution, transactionPort)
        val traced = TracedUseCaseDecorator<InstanceId, WorkflowInstance>(tracer, "instance.suspend") { impl.execute(it) }
        val metered = MeteredUseCaseDecorator<InstanceId, WorkflowInstance>(meter, "instance.suspend") { traced.execute(it) }
        return object : SuspendInstanceUseCase { override fun execute(instanceId: InstanceId) = metered.execute(instanceId) }
    }

    @Singleton
    fun resumeInstanceUseCase(
        instanceRepository: InstanceRepository,
        adapterResolution: AdapterResolutionService,
        transactionPort: TransactionPort,
        tracer: Tracer,
        meter: Meter
    ): ResumeInstanceUseCase {
        val impl = ResumeInstanceUseCaseImpl(instanceRepository, adapterResolution, transactionPort)
        val traced = TracedUseCaseDecorator<InstanceId, WorkflowInstance>(tracer, "instance.resume") { impl.execute(it) }
        val metered = MeteredUseCaseDecorator<InstanceId, WorkflowInstance>(meter, "instance.resume") { traced.execute(it) }
        return object : ResumeInstanceUseCase { override fun execute(instanceId: InstanceId) = metered.execute(instanceId) }
    }

    @Singleton
    fun cancelInstanceUseCase(
        instanceRepository: InstanceRepository,
        adapterResolution: AdapterResolutionService,
        transactionPort: TransactionPort,
        tracer: Tracer,
        meter: Meter
    ): CancelInstanceUseCase {
        val impl = CancelInstanceUseCaseImpl(instanceRepository, adapterResolution, transactionPort)
        data class CancelCommand(val instanceId: InstanceId, val reason: String?)
        val traced = TracedUseCaseDecorator<CancelCommand, WorkflowInstance>(tracer, "instance.cancel") { command ->
            impl.execute(command.instanceId, command.reason)
        }
        val metered = MeteredUseCaseDecorator<CancelCommand, WorkflowInstance>(meter, "instance.cancel") { traced.execute(it) }
        return object : CancelInstanceUseCase {
            override fun execute(instanceId: InstanceId, reason: String?): WorkflowInstance {
                return metered.execute(CancelCommand(instanceId, reason))
            }
        }
    }
}
