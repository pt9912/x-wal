package com.xwal.factory

import com.xwal.application.service.AdapterResolutionService
import com.xwal.application.usecase.task.*
import com.xwal.decorator.MeteredUseCaseDecorator
import com.xwal.decorator.TracedUseCaseDecorator
import com.xwal.domain.model.Task
import com.xwal.domain.model.TaskFilter
import com.xwal.domain.model.TaskId
import com.xwal.domain.port.input.*
import com.xwal.domain.port.output.EngineAdapterConfigRepository
import io.micronaut.context.annotation.Factory
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Tracer
import jakarta.inject.Singleton

@Factory
class TaskUseCaseFactory {

    @Singleton
    fun queryTasksUseCase(
        adapterConfigRepository: EngineAdapterConfigRepository,
        adapterResolution: AdapterResolutionService,
        tracer: Tracer,
        meter: Meter
    ): QueryTasksUseCase {
        val impl = QueryTasksUseCaseImpl(adapterConfigRepository, adapterResolution)
        val traced = TracedUseCaseDecorator<TaskFilter, List<Task>>(tracer, "task.query") { impl.execute(it) }
        val metered = MeteredUseCaseDecorator<TaskFilter, List<Task>>(meter, "task.query") { traced.execute(it) }
        return object : QueryTasksUseCase { override fun execute(filter: TaskFilter) = metered.execute(filter) }
    }

    @Singleton
    fun completeTaskUseCase(
        adapterResolution: AdapterResolutionService,
        tracer: Tracer,
        meter: Meter
    ): CompleteTaskUseCase {
        val impl = CompleteTaskUseCaseImpl(adapterResolution)
        val traced = TracedUseCaseDecorator<CompleteTaskUseCase.Command, Unit>(tracer, "task.complete") { impl.execute(it) }
        val metered = MeteredUseCaseDecorator<CompleteTaskUseCase.Command, Unit>(meter, "task.complete") { traced.execute(it) }
        return object : CompleteTaskUseCase {
            override fun execute(command: CompleteTaskUseCase.Command) {
                metered.execute(command)
            }
        }
    }

    @Singleton
    fun assignTaskUseCase(
        adapterResolution: AdapterResolutionService,
        tracer: Tracer,
        meter: Meter
    ): AssignTaskUseCase {
        val impl = AssignTaskUseCaseImpl(adapterResolution)
        val traced = TracedUseCaseDecorator<Pair<TaskId, String>, Unit>(tracer, "task.assign") {
            impl.execute(it.first, it.second)
        }
        val metered = MeteredUseCaseDecorator<Pair<TaskId, String>, Unit>(meter, "task.assign") { traced.execute(it) }
        return object : AssignTaskUseCase {
            override fun execute(taskId: TaskId, assignee: String) {
                metered.execute(taskId to assignee)
            }
        }
    }

    @Singleton
    fun getTaskUseCase(
        adapterResolution: AdapterResolutionService,
        tracer: Tracer,
        meter: Meter
    ): GetTaskUseCase {
        val impl = GetTaskUseCaseImpl(adapterResolution)
        val traced = TracedUseCaseDecorator<TaskId, com.xwal.domain.model.Task>(tracer, "task.get") { impl.execute(it) }
        val metered = MeteredUseCaseDecorator<TaskId, com.xwal.domain.model.Task>(meter, "task.get") { traced.execute(it) }
        return object : GetTaskUseCase { override fun execute(taskId: TaskId) = metered.execute(taskId) }
    }
}
