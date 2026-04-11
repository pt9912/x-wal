package com.xwal.adapter.web.grpc

import com.xwal.adapter.web.grpc.mapper.GrpcWorkflowMapper
import com.xwal.adapter.web.grpc.proto.*
import com.xwal.domain.model.InstanceId
import com.xwal.domain.model.EngineAdapterId
import com.xwal.domain.model.WorkflowId
import com.xwal.domain.port.input.*
import com.xwal.domain.port.output.EngineAdapterConfigRepository
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
    private val getInstanceVariables: GetInstanceVariablesUseCase,
    private val adapterConfigRepository: EngineAdapterConfigRepository
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
            val page = if (request.page > 0) request.page else 1
            val size = if (request.size > 0) request.size else 50
            val all = listWorkflows.execute()
            val workflows = all.drop((page - 1) * size).take(size)
            ListWorkflowsResponse.newBuilder()
                .addAllWorkflows(workflows.map(GrpcWorkflowMapper::toProto))
                .setTotal(all.size).build()
        }

    override fun startInstance(request: StartInstanceRequest, responseObserver: StreamObserver<InstanceResponse>) =
        GrpcErrorMapper.handle(responseObserver) {
            val instance = startWorkflow.execute(StartWorkflowUseCase.Command(
                workflowId = WorkflowId(UUID.fromString(request.workflowId)),
                businessKey = null,
                variables = if (request.hasVariables()) GrpcWorkflowMapper.structToMap(request.variables) else emptyMap(),
                startedBy = null
            ))
            GrpcWorkflowMapper.toProtoInstance(instance, resolveEngineType(instance.engineAdapterId))
        }

    override fun getInstance(request: GetInstanceRequest, responseObserver: StreamObserver<InstanceResponse>) =
        GrpcErrorMapper.handle(responseObserver) {
            val instance = getInstance.execute(InstanceId(UUID.fromString(request.instanceId)))
            GrpcWorkflowMapper.toProtoInstance(instance, resolveEngineType(instance.engineAdapterId))
        }

    override fun suspendInstance(request: SuspendInstanceRequest, responseObserver: StreamObserver<InstanceResponse>) =
        GrpcErrorMapper.handle(responseObserver) {
            val instance = suspendInstance.execute(InstanceId(UUID.fromString(request.instanceId)))
            GrpcWorkflowMapper.toProtoInstance(instance, resolveEngineType(instance.engineAdapterId))
        }

    override fun resumeInstance(request: ResumeInstanceRequest, responseObserver: StreamObserver<InstanceResponse>) =
        GrpcErrorMapper.handle(responseObserver) {
            val instance = resumeInstance.execute(InstanceId(UUID.fromString(request.instanceId)))
            GrpcWorkflowMapper.toProtoInstance(instance, resolveEngineType(instance.engineAdapterId))
        }

    override fun cancelInstance(request: CancelInstanceRequest, responseObserver: StreamObserver<InstanceResponse>) =
        GrpcErrorMapper.handle(responseObserver) {
            val instance = cancelInstance.execute(InstanceId(UUID.fromString(request.instanceId)))
            GrpcWorkflowMapper.toProtoInstance(instance, resolveEngineType(instance.engineAdapterId))
        }

    override fun getInstanceVariables(request: GetInstanceRequest, responseObserver: StreamObserver<InstanceVariablesResponse>) =
        GrpcErrorMapper.handle(responseObserver) {
            val variables = getInstanceVariables.execute(InstanceId(UUID.fromString(request.instanceId)))
            GrpcWorkflowMapper.toProtoVariables(request.instanceId, variables)
        }

    private fun resolveEngineType(adapterId: EngineAdapterId?): String =
        adapterId
            ?.let { adapterConfigRepository.findById(it)?.engineType?.name }
            ?: ""
}
