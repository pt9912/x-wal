package com.xwal.application.usecase.task

import com.xwal.application.service.AdapterResolutionService
import com.xwal.domain.model.HealthStatus
import com.xwal.domain.model.Task
import com.xwal.domain.model.TaskFilter
import com.xwal.domain.model.TaskId
import com.xwal.domain.port.input.QueryTasksUseCase
import com.xwal.domain.port.output.EngineAdapterConfigRepository
import com.xwal.domain.service.TaskAggregationLogic
import org.slf4j.LoggerFactory

class QueryTasksUseCaseImpl(
    private val adapterConfigRepository: EngineAdapterConfigRepository,
    private val adapterResolution: AdapterResolutionService
) : QueryTasksUseCase {

    private val log = LoggerFactory.getLogger(QueryTasksUseCaseImpl::class.java)

    override fun execute(filter: TaskFilter): List<Task> {
        val healthyAdapters = adapterConfigRepository.findByHealthStatus(HealthStatus.HEALTHY)
            .filter { it.enabled }

        if (healthyAdapters.isEmpty()) return emptyList()

        val allTasks = mutableListOf<Task>()

        for (adapterConfig in healthyAdapters) {
            try {
                val adapter = adapterResolution.resolveByConfig(adapterConfig)
                val engineFilter = TaskFilter(
                    assignee = filter.assignee,
                    candidateGroup = filter.candidateGroup,
                    processInstanceId = filter.processInstanceId,
                    status = filter.status,
                    limit = filter.offset + filter.limit,
                    offset = 0
                )
                val tasks = adapter.queryTasks(engineFilter).map { task ->
                    task.copy(taskId = TaskId.of(adapterConfig.engineType, adapterConfig.id, task.taskId.engineTaskId))
                }
                allTasks.addAll(tasks)
            } catch (e: Exception) {
                log.warn("Failed to query tasks from adapter {}: {}", adapterConfig.name, e.message)
            }
        }

        return TaskAggregationLogic.sortAndPaginate(allTasks, filter.offset, filter.limit)
    }
}
