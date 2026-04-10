package com.xwal.adapter.web.rest.controller

import com.xwal.adapter.web.rest.dto.response.InstanceResponse
import com.xwal.adapter.web.rest.mapper.InstanceDtoMapper
import com.xwal.domain.model.InstanceId
import com.xwal.domain.port.input.GetInstanceUseCase
import com.xwal.domain.port.input.GetInstanceVariablesUseCase
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import java.util.UUID

@Controller("/api/v1/instances")
@Secured("workflow.read", "workflow.write", "workflow.admin")
class InstanceController(
    private val getInstance: GetInstanceUseCase,
    private val getInstanceVariables: GetInstanceVariablesUseCase
) {

    @Get("/{id}")
    fun get(id: UUID): InstanceResponse {
        val instance = getInstance.execute(InstanceId(id))
        return InstanceDtoMapper.toResponse(instance)
    }

    @Get("/{id}/variables")
    fun getVariables(id: UUID): Map<String, Any> {
        return getInstanceVariables.execute(InstanceId(id))
    }
}
