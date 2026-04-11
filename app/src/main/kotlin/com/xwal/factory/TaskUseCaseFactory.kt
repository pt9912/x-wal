package com.xwal.factory

import com.xwal.application.service.AdapterResolutionService
import com.xwal.application.usecase.task.*
import com.xwal.decorator.MeteredUseCaseDecorator
import com.xwal.decorator.TracedUseCaseDecorator
import com.xwal.domain.model.Task
import com.xwal.domain.model.TaskFilter
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
        tracer: Tracer, meter: Meter
    ): QueryTasksUseCase {
        val impl = QueryTasksUseCaseImpl(adapterConfigRepository, adapterResolution)
        val traced = TracedUseCaseDecorator<TaskFilter, List<Task>>(tracer, "task.query") { impl.execute(it) }
        val metered = MeteredUseCaseDecorator<TaskFilter, List<Task>>(meter, "task.query") { traced.execute(it) }
        return object : QueryTasksUseCase { override fun execute(filter: TaskFilter) = metered.execute(filter) }
    }

    @Singleton
    fun completeTaskUseCase(
        adapterResolution: AdapterResolutionService,
        tracer: Tracer, meter: Meter
    ): CompleteTaskUseCase {
        val impl = CompleteTaskUseCaseImpl(adapterResolution)
        return object : CompleteTaskUseCase {
            override fun execute(command: CompleteTaskUseCase.Command) {
                val span = tracer.spanBuilder("task.complete").startSpan()
                val scope = span.makeCurrent()
                try { meter.counterBuilder("task.complete.total").build().add(1); impl.execute(command) }
                catch (e: Exception) { span.recordException(e); throw e }
                finally { scope.close(); span.end() }
            }
        }
    }

    @Singleton
    fun assignTaskUseCase(adapterResolution: AdapterResolutionService): AssignTaskUseCase =
        AssignTaskUseCaseImpl(adapterResolution)

    @Singleton
    fun getTaskUseCase(adapterResolution: AdapterResolutionService): GetTaskUseCase =
        GetTaskUseCaseImpl(adapterResolution)
}
