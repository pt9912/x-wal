package com.xwal.adapter.web.grpc.mapper

import com.google.protobuf.Struct
import com.google.protobuf.Value
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

    fun toProtoInstance(instance: WorkflowInstance, engineType: String): ProtoInstance =
        ProtoInstance.newBuilder()
            .setId(instance.id.value.toString())
            .setWorkflowId(instance.workflowId.value.toString())
            .setEngineType(engineType)
            .setEngineInstanceId(instance.engineInstanceId ?: "")
            .setStatus(instance.status.name)
            .setStartedAt(instance.startedAt?.toEpochMilli() ?: 0)
            .setCompletedAt(instance.endedAt?.toEpochMilli() ?: 0)
            .build()

    fun toProtoVariables(instanceId: String, variables: Map<String, Any>): ProtoVariables =
        ProtoVariables.newBuilder()
            .setInstanceId(instanceId)
            .setVariables(mapToStruct(variables))
            .build()

    /** Convert Map<String, Any> → google.protobuf.Struct (preserves types) */
    fun mapToStruct(map: Map<String, Any>): Struct {
        val builder = Struct.newBuilder()
        map.forEach { (key, value) -> builder.putFields(key, anyToValue(value)) }
        return builder.build()
    }

    /** Convert google.protobuf.Struct → Map<String, Any> */
    fun structToMap(struct: Struct): Map<String, Any> =
        struct.fieldsMap.mapNotNull { (name, value) ->
            valueToAny(value)?.let { name to it }
        }.toMap()

    private fun anyToValue(v: Any?): Value = when (v) {
        null -> Value.newBuilder().setNullValue(com.google.protobuf.NullValue.NULL_VALUE).build()
        is String -> Value.newBuilder().setStringValue(v).build()
        is Number -> Value.newBuilder().setNumberValue(v.toDouble()).build()
        is Boolean -> Value.newBuilder().setBoolValue(v).build()
        is Map<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            Value.newBuilder().setStructValue(mapToStruct(v as Map<String, Any>)).build()
        }
        is List<*> -> {
            val list = com.google.protobuf.ListValue.newBuilder()
            v.forEach { list.addValues(anyToValue(it)) }
            Value.newBuilder().setListValue(list).build()
        }
        else -> Value.newBuilder().setStringValue(v.toString()).build()
    }

    private fun valueToAny(v: Value): Any? = when (v.kindCase) {
        Value.KindCase.STRING_VALUE -> v.stringValue
        Value.KindCase.NUMBER_VALUE -> v.numberValue
        Value.KindCase.BOOL_VALUE -> v.boolValue
        Value.KindCase.STRUCT_VALUE -> structToMap(v.structValue)
        Value.KindCase.LIST_VALUE -> v.listValue.valuesList.mapNotNull { valueToAny(it) }
        Value.KindCase.NULL_VALUE -> null
        else -> v.toString()
    }
}
