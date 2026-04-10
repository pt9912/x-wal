package com.xwal.adapter.web.rest.controller

import com.xwal.adapter.web.rest.dto.request.CreateWorkflowRequest
import com.xwal.adapter.web.rest.dto.request.StartInstanceRequest
import com.xwal.adapter.web.rest.dto.response.InstanceResponse
import com.xwal.adapter.web.rest.dto.response.WorkflowResponse
import com.xwal.adapter.web.rest.mapper.InstanceDtoMapper
import com.xwal.adapter.web.rest.mapper.WorkflowDtoMapper
import com.xwal.domain.model.WorkflowId
import com.xwal.domain.model.WorkflowStatus
import com.xwal.domain.port.input.*
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.validation.Validated
import jakarta.validation.Valid
import java.util.UUID

@Controller("/api/v1/workflows")
@Validated
@Secured(SecurityRule.IS_AUTHENTICATED)
class WorkflowController(
    private val createWorkflow: CreateWorkflowUseCase,
    private val getWorkflow: GetWorkflowUseCase,
    private val listWorkflows: ListWorkflowsUseCase,
    private val startWorkflow: StartWorkflowUseCase,
    private val updateWorkflow: UpdateWorkflowUseCase,
    private val deleteWorkflow: DeleteWorkflowUseCase,
    private val suspendInstance: SuspendInstanceUseCase,
    private val resumeInstance: ResumeInstanceUseCase,
    private val cancelInstance: CancelInstanceUseCase
) {

    @Post
    @Secured("workflow.write", "workflow.admin")
    fun create(@Valid @Body request: CreateWorkflowRequest): HttpResponse<WorkflowResponse> {
        val workflow = createWorkflow.execute(
            CreateWorkflowUseCase.Command(
                name = request.name,
                version = request.version,
                description = request.description,
                iwmDefinition = request.iwmDefinition,
                createdBy = request.createdBy
            )
        )
        return HttpResponse.created(WorkflowDtoMapper.toResponse(workflow))
    }

    @Get("/{id}")
    @Secured("workflow.read", "workflow.write", "workflow.admin")
    fun get(id: UUID): WorkflowResponse {
        val workflow = getWorkflow.execute(WorkflowId(id))
        return WorkflowDtoMapper.toResponse(workflow)
    }

    @Get
    @Secured("workflow.read", "workflow.write", "workflow.admin")
    fun list(@QueryValue status: String?): List<WorkflowResponse> {
        val workflowStatus = status?.let { runCatching { WorkflowStatus.valueOf(it.uppercase()) }.getOrNull() }
        return WorkflowDtoMapper.toResponseList(listWorkflows.execute(workflowStatus))
    }

    @Delete("/{id}")
    @Secured("workflow.admin")
    fun delete(id: UUID): HttpResponse<Void> {
        deleteWorkflow.execute(WorkflowId(id))
        return HttpResponse.noContent()
    }

    @Post("/{id}/start")
    @Secured("workflow.write", "workflow.admin")
    fun start(id: UUID, @Body request: StartInstanceRequest): HttpResponse<InstanceResponse> {
        val instance = startWorkflow.execute(
            StartWorkflowUseCase.Command(
                workflowId = WorkflowId(id),
                businessKey = request.businessKey,
                variables = request.variables ?: emptyMap(),
                startedBy = request.startedBy
            )
        )
        return HttpResponse.created(InstanceDtoMapper.toResponse(instance))
    }

    @Post("/instances/{instanceId}/suspend")
    @Secured("workflow.write", "workflow.admin")
    fun suspend(instanceId: UUID): InstanceResponse {
        val instance = suspendInstance.execute(com.xwal.domain.model.InstanceId(instanceId))
        return InstanceDtoMapper.toResponse(instance)
    }

    @Post("/instances/{instanceId}/resume")
    @Secured("workflow.write", "workflow.admin")
    fun resume(instanceId: UUID): InstanceResponse {
        val instance = resumeInstance.execute(com.xwal.domain.model.InstanceId(instanceId))
        return InstanceDtoMapper.toResponse(instance)
    }

    @Post("/instances/{instanceId}/cancel")
    @Secured("workflow.admin")
    fun cancel(instanceId: UUID): InstanceResponse {
        val instance = cancelInstance.execute(com.xwal.domain.model.InstanceId(instanceId))
        return InstanceDtoMapper.toResponse(instance)
    }
}
