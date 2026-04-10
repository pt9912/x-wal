package com.xwal.adapter.web.rest.controller

import com.xwal.adapter.web.rest.dto.request.RegisterAdapterRequest
import com.xwal.domain.model.EngineAdapterId
import com.xwal.domain.model.EngineType
import com.xwal.domain.port.input.DeregisterAdapterUseCase
import com.xwal.domain.port.input.HealthCheckAdapterUseCase
import com.xwal.domain.port.input.RegisterAdapterUseCase
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import java.util.UUID

@Controller("/api/v1/adapters")
@Secured(SecurityRule.IS_AUTHENTICATED)
class AdapterController(
    private val registerAdapter: RegisterAdapterUseCase,
    private val deregisterAdapter: DeregisterAdapterUseCase,
    private val healthCheckAdapter: HealthCheckAdapterUseCase
) {

    @Post
    @Secured("workflow.admin")
    fun register(@Body request: RegisterAdapterRequest): HttpResponse<Any> {
        val adapter = registerAdapter.execute(
            RegisterAdapterUseCase.Command(
                name = request.name,
                engineType = EngineType.valueOf(request.engineType.uppercase()),
                config = request.endpointUrl,
                priority = request.priority,
                createdBy = request.createdBy
            )
        )
        return HttpResponse.created(adapter)
    }

    @Delete("/{id}")
    @Secured("workflow.admin")
    fun deregister(id: UUID): HttpResponse<Void> {
        deregisterAdapter.execute(EngineAdapterId(id))
        return HttpResponse.noContent()
    }

    @Post("/health-check")
    @Secured("workflow.admin")
    fun healthCheckAll(): Any {
        val results = healthCheckAdapter.executeAll()
        return mapOf(
            "total" to results.size,
            "healthy" to results.count { it.healthStatus.name == "HEALTHY" },
            "adapters" to results.map { mapOf("id" to it.id.value, "name" to it.name, "health" to it.healthStatus.name) }
        )
    }

    @Post("/{id}/health-check")
    @Secured("workflow.admin")
    fun healthCheckSingle(id: UUID): Any {
        val result = healthCheckAdapter.executeSingle(EngineAdapterId(id))
        return mapOf("id" to result.id.value, "name" to result.name, "health" to result.healthStatus.name)
    }
}
