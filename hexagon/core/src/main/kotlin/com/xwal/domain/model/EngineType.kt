package com.xwal.domain.model

enum class EngineType(val displayName: String) {
    CAMUNDA7("Camunda 7"),
    CAMUNDA8("Camunda 8 / Zeebe"),
    FLOWABLE("Flowable"),
    ACTIVITI("Activiti"),
    TEMPORAL("Temporal"),
    CONDUCTOR("Netflix Conductor")
}
