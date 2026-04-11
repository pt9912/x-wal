package com.xwal.adapter.web.grpc.mapper

import com.xwal.adapter.web.grpc.proto.TaskResponse as ProtoTask
import com.xwal.domain.model.Task

object GrpcTaskMapper {

    fun toProto(task: Task): ProtoTask =
        ProtoTask.newBuilder()
            .setId(task.taskId.value)
            .setName(task.name ?: "")
            .setAssignee(task.assignee ?: "")
            .setStatus(task.status?.name ?: "CREATED")
            .setCreatedAt(task.createdAt?.toEpochMilli() ?: 0)
            .build()
}
