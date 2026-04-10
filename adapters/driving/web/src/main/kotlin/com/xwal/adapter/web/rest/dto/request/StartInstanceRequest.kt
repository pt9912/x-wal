package com.xwal.adapter.web.rest.dto.request

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class StartInstanceRequest(
    val businessKey: String? = null,
    val variables: Map<String, Any>? = null,
    val startedBy: String? = null
)
