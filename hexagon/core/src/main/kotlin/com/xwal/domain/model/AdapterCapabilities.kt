package com.xwal.domain.model

data class AdapterCapabilities(
    val supportsUserTasks: Boolean = false,
    val supportsServiceTasks: Boolean = false,
    val supportsTimerEvents: Boolean = false,
    val supportsMessageEvents: Boolean = false,
    val supportsSignalEvents: Boolean = false,
    val supportsErrorEvents: Boolean = false,
    val supportsCompensation: Boolean = false,
    val supportsSubProcesses: Boolean = false,
    val supportsParallelGateways: Boolean = false,
    val supportsExclusiveGateways: Boolean = false,
    val supportsInclusiveGateways: Boolean = false,
    val supportsEventBasedGateways: Boolean = false,
    val supportsMultiInstance: Boolean = false,
    val supportsCallActivity: Boolean = false,
    val supportsScriptTasks: Boolean = false,
    val supportsExternalTasks: Boolean = false,
    val supportsDMN: Boolean = false,
    val supportsCMMN: Boolean = false,
    val supportsTransactions: Boolean = false,
    val supportedDataTypes: Set<String> = emptySet(),
    val supportedScriptLanguages: Set<String> = emptySet(),
    val maxParallelInstances: Int = -1,
    val maxVariablesPerInstance: Int = -1
) {
    fun hasAllCapabilities(required: AdapterCapabilities): Boolean {
        if (required.supportsUserTasks && !supportsUserTasks) return false
        if (required.supportsServiceTasks && !supportsServiceTasks) return false
        if (required.supportsTimerEvents && !supportsTimerEvents) return false
        if (required.supportsMessageEvents && !supportsMessageEvents) return false
        if (required.supportsSignalEvents && !supportsSignalEvents) return false
        if (required.supportsErrorEvents && !supportsErrorEvents) return false
        if (required.supportsCompensation && !supportsCompensation) return false
        if (required.supportsSubProcesses && !supportsSubProcesses) return false
        if (required.supportsParallelGateways && !supportsParallelGateways) return false
        if (required.supportsExclusiveGateways && !supportsExclusiveGateways) return false
        if (required.supportsInclusiveGateways && !supportsInclusiveGateways) return false
        if (required.supportsEventBasedGateways && !supportsEventBasedGateways) return false
        if (required.supportsMultiInstance && !supportsMultiInstance) return false
        if (required.supportsCallActivity && !supportsCallActivity) return false
        if (required.supportsScriptTasks && !supportsScriptTasks) return false
        if (required.supportsExternalTasks && !supportsExternalTasks) return false
        if (required.supportsDMN && !supportsDMN) return false
        if (required.supportsCMMN && !supportsCMMN) return false
        if (required.supportsTransactions && !supportsTransactions) return false
        return true
    }

    companion object {
        fun createBpmnCapabilities(): AdapterCapabilities = AdapterCapabilities(
            supportsUserTasks = true,
            supportsServiceTasks = true,
            supportsTimerEvents = true,
            supportsMessageEvents = true,
            supportsSignalEvents = true,
            supportsErrorEvents = true,
            supportsCompensation = true,
            supportsSubProcesses = true,
            supportsParallelGateways = true,
            supportsExclusiveGateways = true,
            supportsInclusiveGateways = true,
            supportsEventBasedGateways = true,
            supportsMultiInstance = true,
            supportsCallActivity = true,
            supportsScriptTasks = true,
            supportsExternalTasks = true,
            supportsDMN = false,
            supportsCMMN = false,
            supportsTransactions = false
        )
    }
}
