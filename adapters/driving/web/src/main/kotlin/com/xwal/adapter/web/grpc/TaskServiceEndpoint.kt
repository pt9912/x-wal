package com.xwal.adapter.web.grpc

import com.xwal.adapter.web.grpc.mapper.GrpcTaskMapper
import com.xwal.adapter.web.grpc.proto.*
import com.xwal.domain.model.TaskFilter
import com.xwal.domain.model.TaskId
import com.xwal.domain.model.TaskStatus
import com.xwal.domain.port.input.CompleteTaskUseCase
import com.xwal.domain.port.input.QueryTasksUseCase
import io.grpc.stub.StreamObserver
import jakarta.inject.Singleton

@Singleton
class TaskServiceEndpoint(
    private val queryTasks: QueryTasksUseCase,
    private val completeTask: CompleteTaskUseCase
) : TaskServiceGrpc.TaskServiceImplBase() {

    override fun queryTasks(request: QueryTasksRequest, responseObserver: StreamObserver<QueryTasksResponse>) =
        GrpcErrorMapper.handle(responseObserver) {
            val filter = TaskFilter(
                assignee = if (request.hasAssignee()) request.assignee else null,
                status = if (request.hasStatus()) runCatching { TaskStatus.valueOf(request.status.uppercase()) }.getOrNull() else null,
                limit = if (request.size > 0) request.size else 50,
                offset = if (request.page > 0) (request.page - 1) * request.size else 0
            )
            val tasks = queryTasks.execute(filter)
            QueryTasksResponse.newBuilder()
                .addAllTasks(tasks.map(GrpcTaskMapper::toProto))
                .setTotal(tasks.size).build()
        }

    override fun completeTask(request: com.xwal.adapter.web.grpc.proto.CompleteTaskRequest, responseObserver: StreamObserver<com.xwal.adapter.web.grpc.proto.TaskResponse>) =
        GrpcErrorMapper.handle(responseObserver) {
            val taskId = TaskId(request.taskId)
            completeTask.execute(CompleteTaskUseCase.Command(taskId, request.variablesMap.toMap()))
            com.xwal.adapter.web.grpc.proto.TaskResponse.newBuilder()
                .setId(request.taskId).setStatus("COMPLETED").build()
        }
}
