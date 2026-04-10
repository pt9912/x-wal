package com.xwal.adapter.web.rest.controller

import com.xwal.adapter.web.rest.dto.request.CompleteTaskRequest
import com.xwal.adapter.web.rest.dto.response.TaskResponse
import com.xwal.adapter.web.rest.mapper.TaskDtoMapper
import com.xwal.domain.model.TaskFilter
import com.xwal.domain.model.TaskId
import com.xwal.domain.model.TaskStatus
import com.xwal.domain.port.input.*
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.validation.Validated
import jakarta.validation.Valid

@Controller("/api/v1/tasks")
@Validated
@Secured(SecurityRule.IS_AUTHENTICATED)
class TaskController(
    private val queryTasks: QueryTasksUseCase,
    private val completeTask: CompleteTaskUseCase,
    private val getTask: GetTaskUseCase,
    private val assignTask: AssignTaskUseCase
) {

    @Get
    @Secured("workflow.read", "workflow.write", "workflow.admin")
    fun list(
        @QueryValue assignee: String?,
        @QueryValue status: String?,
        @QueryValue limit: Int?,
        @QueryValue offset: Int?
    ): List<TaskResponse> {
        // Validate status parameter if provided
        val taskStatus = status?.let {
            runCatching { TaskStatus.valueOf(it.uppercase()) }.getOrElse {
                throw IllegalArgumentException("Invalid task status: $status. Valid values: ${TaskStatus.entries.joinToString()}")
            }
        }
        val filter = TaskFilter(
            assignee = assignee,
            status = taskStatus,
            limit = limit ?: 50,
            offset = offset ?: 0
        )
        return TaskDtoMapper.toResponseList(queryTasks.execute(filter))
    }

    @Post("/{taskId}/complete")
    @Secured("workflow.write", "workflow.admin")
    fun complete(taskId: String, @Valid @Body request: CompleteTaskRequest): HttpResponse<TaskResponse> {
        val id = TaskId(taskId)
        completeTask.execute(CompleteTaskUseCase.Command(id, request.variables ?: emptyMap()))
        return HttpResponse.ok(TaskResponse(id = taskId, status = "COMPLETED"))
    }

    @Get("/{taskId}")
    @Secured("workflow.read", "workflow.write", "workflow.admin")
    fun get(taskId: String): TaskResponse {
        return TaskDtoMapper.toResponse(getTask.execute(TaskId(taskId)))
    }

    @Post("/{taskId}/assign")
    @Secured("workflow.write", "workflow.admin")
    fun assign(taskId: String, @QueryValue assignee: String): TaskResponse {
        assignTask.execute(TaskId(taskId), assignee)
        return TaskDtoMapper.toResponse(getTask.execute(TaskId(taskId)))
    }

    @Post("/{taskId}/unassign")
    @Secured("workflow.write", "workflow.admin")
    fun unassign(taskId: String): TaskResponse {
        assignTask.execute(TaskId(taskId), "")
        return TaskDtoMapper.toResponse(getTask.execute(TaskId(taskId)))
    }
}
