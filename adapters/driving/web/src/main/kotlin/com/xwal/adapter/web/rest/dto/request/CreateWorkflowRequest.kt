package com.xwal.adapter.web.rest.dto.request

import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Serdeable
data class CreateWorkflowRequest(
    @field:NotBlank val name: String,
    @field:NotBlank val version: String,
    val description: String? = null,
    @field:NotNull val iwmDefinition: String,
    val createdBy: String? = null
)
