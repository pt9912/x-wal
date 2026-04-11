package com.xwal.adapter.web.grpc

import com.xwal.adapter.web.grpc.mapper.GrpcWorkflowMapper
import com.xwal.adapter.web.grpc.proto.*
import com.xwal.domain.model.InstanceId
import com.xwal.domain.model.WorkflowId
import com.xwal.domain.port.input.*
import io.grpc.stub.StreamObserver
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
class WorkflowServiceEndpoint(
    private val createWorkflow: CreateWorkflowUseCase,
    private val getWorkflow: GetWorkflowUseCase,
    private val listWorkflows: ListWorkflowsUseCase,
    private val startWorkflow: StartWorkflowUseCase,
    private val getInstance: GetInstanceUseCase,
    private val suspendInstance: SuspendInstanceUseCase,
    private val resumeInstance: ResumeInstanceUseCase,
    private val cancelInstance: CancelInstanceUseCase,
    private val getInstanceVariables: GetInstanceVariablesUseCase
) : WorkflowServiceGrpc.WorkflowServiceImplBase() {

    override fun createWorkflow(request: CreateWorkflowRequest, responseObserver: StreamObserver<WorkflowResponse>) =
        GrpcErrorMapper.handle(responseObserver) {
            val workflow = createWorkflow.execute(CreateWorkflowUseCase.Command(
                name = request.name, version = request.version,
                iwmDefinition = request.iwmDefinition, description = null, createdBy = null
            ))
            GrpcWorkflowMapper.toProto(workflow)
        }

    override fun getWorkflow(request: GetWorkflowRequest, responseObserver: StreamObserver<WorkflowResponse>) =
        GrpcErrorMapper.handle(responseObserver) {
            GrpcWorkflowMapper.toProto(getWorkflow.execute(WorkflowId(UUID.fromString(request.id))))
        }

    override fun listWorkflows(request: ListWorkflowsRequest, responseObserver: StreamObserver<ListWorkflowsResponse>) =
        GrpcErrorMapper.handle(responseObserver) {
            val workflows = listWorkflows.execute()
            ListWorkflowsResponse.newBuilder()
                .addAllWorkflows(workflows.map(GrpcWorkflowMapper::toProto))
                .setTotal(workflows.size).build()
        }

    override fun startInstance(request: StartInstanceRequest, responseObserver: StreamObserver<InstanceResponse>) =
        GrpcErrorMapper.handle(responseObserver) {
            val instance = startWorkflow.execute(StartWorkflowUseCase.Command(
                workflowId = WorkflowId(UUID.fromString(request.workflowId)),
                businessKey = null, variables = request.variablesMap.toMap(), startedBy = null
            ))
            GrpcWorkflowMapper.toProtoInstance(instance)
        }

    override fun getInstance(request: GetInstanceRequest, responseObserver: StreamObserver<InstanceResponse>) =
        GrpcErrorMapper.handle(responseObserver) {
            GrpcWorkflowMapper.toProtoInstance(getInstance.execute(InstanceId(UUID.fromString(request.instanceId))))
        }

    override fun suspendInstance(request: SuspendInstanceRequest, responseObserver: StreamObserver<InstanceResponse>) =
        GrpcErrorMapper.handle(responseObserver) {
            GrpcWorkflowMapper.toProtoInstance(suspendInstance.execute(InstanceId(UUID.fromString(request.instanceId))))
        }

    override fun resumeInstance(request: ResumeInstanceRequest, responseObserver: StreamObserver<InstanceResponse>) =
        GrpcErrorMapper.handle(responseObserver) {
            GrpcWorkflowMapper.toProtoInstance(resumeInstance.execute(InstanceId(UUID.fromString(request.instanceId))))
        }

    override fun cancelInstance(request: CancelInstanceRequest, responseObserver: StreamObserver<InstanceResponse>) =
        GrpcErrorMapper.handle(responseObserver) {
            GrpcWorkflowMapper.toProtoInstance(cancelInstance.execute(InstanceId(UUID.fromString(request.instanceId))))
        }

    override fun getInstanceVariables(request: GetInstanceRequest, responseObserver: StreamObserver<InstanceVariablesResponse>) =
        GrpcErrorMapper.handle(responseObserver) {
            val variables = getInstanceVariables.execute(InstanceId(UUID.fromString(request.instanceId)))
            GrpcWorkflowMapper.toProtoVariables(request.instanceId, variables)
        }
}
