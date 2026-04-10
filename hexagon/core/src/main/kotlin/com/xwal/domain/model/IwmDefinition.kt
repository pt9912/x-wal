package com.xwal.domain.model

@JvmInline
value class IwmDefinition(val json: String) {
    init {
        require(json.isNotBlank()) { "IWM definition must not be blank" }
    }

    override fun toString(): String = "IwmDefinition(${json.length} chars)"
}
