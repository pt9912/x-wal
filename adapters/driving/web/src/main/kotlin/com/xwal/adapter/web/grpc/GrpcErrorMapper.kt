package com.xwal.adapter.web.grpc

import com.xwal.domain.exception.*
import io.grpc.Status
import io.grpc.stub.StreamObserver

/**
 * Shared gRPC error handling — maps domain exceptions to appropriate gRPC status codes.
 */
object GrpcErrorMapper {

    fun <T> handle(responseObserver: StreamObserver<T>, block: () -> T) {
        try {
            val result = block()
            responseObserver.onNext(result)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(mapToGrpcStatus(e).asRuntimeException())
        }
    }

    private fun mapToGrpcStatus(e: Exception): Status = when (e) {
        is WorkflowNotFoundException -> Status.NOT_FOUND.withDescription(e.message)
        is InstanceNotFoundException -> Status.NOT_FOUND.withDescription(e.message)
        is AdapterNotFoundException -> Status.NOT_FOUND.withDescription(e.message)
        is WorkflowValidationException -> Status.INVALID_ARGUMENT.withDescription(e.message)
        is DuplicateWorkflowException -> Status.ALREADY_EXISTS.withDescription(e.message)
        is InvalidStateTransitionException -> Status.FAILED_PRECONDITION.withDescription(e.message)
        is AdapterUnavailableException -> Status.UNAVAILABLE.withDescription(e.message)
        is AdapterOperationException -> Status.INTERNAL.withDescription(e.message)
        is IllegalArgumentException -> Status.INVALID_ARGUMENT.withDescription(e.message)
        else -> Status.INTERNAL.withDescription(e.message ?: "Unknown error").withCause(e)
    }
}
