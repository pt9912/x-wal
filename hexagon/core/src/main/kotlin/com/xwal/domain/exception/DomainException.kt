package com.xwal.domain.exception

sealed class DomainException(
    override val message: String,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)

class WorkflowNotFoundException(
    val workflowId: String
) : DomainException("Workflow not found: $workflowId")

class InstanceNotFoundException(
    val instanceId: String
) : DomainException("Instance not found: $instanceId")

class WorkflowValidationException(
    override val message: String,
    val errors: List<String> = emptyList()
) : DomainException(message)

class AdapterNotFoundException(
    val adapterId: String
) : DomainException("Adapter not found: $adapterId")

class AdapterUnavailableException(
    val adapterId: String,
    override val message: String = "Adapter unavailable: $adapterId",
    override val cause: Throwable? = null
) : DomainException(message, cause)

class InvalidStateTransitionException(
    val currentState: String,
    val targetState: String,
    val entityId: String
) : DomainException("Invalid state transition from $currentState to $targetState for $entityId")

class AdapterOperationException(
    val engineType: String,
    val operation: String,
    override val message: String,
    override val cause: Throwable? = null
) : DomainException(message, cause)

class DuplicateWorkflowException(
    val name: String,
    val version: String
) : DomainException("Workflow already exists: $name v$version")
