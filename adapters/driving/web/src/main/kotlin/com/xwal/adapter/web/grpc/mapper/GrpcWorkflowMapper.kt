package com.xwal.adapter.web.grpc.mapper

import com.xwal.adapter.web.grpc.proto.InstanceResponse as ProtoInstance
import com.xwal.adapter.web.grpc.proto.InstanceVariablesResponse as ProtoVariables
import com.xwal.adapter.web.grpc.proto.WorkflowResponse as ProtoWorkflow
import com.xwal.domain.model.Workflow
import com.xwal.domain.model.WorkflowInstance

object GrpcWorkflowMapper {

    fun toProto(workflow: Workflow): ProtoWorkflow =
        ProtoWorkflow.newBuilder()
            .setId(workflow.id.value.toString())
            .setName(workflow.name)
            .setVersion(workflow.version)
            .setIwmDefinition(workflow.iwmDefinition.json)
            .setStatus(workflow.status.name)
            .setCreatedAt(workflow.createdAt.toEpochMilli())
            .setUpdatedAt(workflow.updatedAt.toEpochMilli())
            .build()

    fun toProtoInstance(instance: WorkflowInstance): ProtoInstance =
        ProtoInstance.newBuilder()
            .setId(instance.id.value.toString())
            .setWorkflowId(instance.workflowId.value.toString())
            .setEngineType(instance.engineAdapterId?.value?.toString() ?: "")
            .setEngineInstanceId(instance.engineInstanceId ?: "")
            .setStatus(instance.status.name)
            .setStartedAt(instance.startedAt?.toEpochMilli() ?: 0)
            .setCompletedAt(instance.endedAt?.toEpochMilli() ?: 0)
            .build()

    fun toProtoVariables(instanceId: String, variables: Map<String, Any>): ProtoVariables =
        ProtoVariables.newBuilder()
            .setInstanceId(instanceId)
            .putAllVariables(variables.mapValues { it.value.toString() })
            .build()
}
