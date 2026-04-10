package com.xwal.adapter.web.rest.dto.request

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class RegisterAdapterRequest(
    val name: String,
    val engineType: String,
    val endpointUrl: String,
    val engineVersion: String? = null,
    val priority: Int = 0,
    val createdBy: String? = null
)
