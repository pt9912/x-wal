package com.xwal.application.usecase.workflow

import com.xwal.application.service.AdapterResolutionService
import com.xwal.domain.exception.AdapterUnavailableException
import com.xwal.domain.exception.WorkflowNotFoundException
import com.xwal.domain.model.*
import com.xwal.domain.port.input.StartWorkflowUseCase
import com.xwal.domain.port.input.StartWorkflowUseCase.Command
import com.xwal.domain.port.output.EngineAdapterConfigRepository
import com.xwal.domain.port.output.InstanceRepository
import com.xwal.domain.port.output.TransactionPort
import com.xwal.domain.port.output.WorkflowRepository
import com.xwal.domain.service.EngineRoutingLogic
import org.slf4j.LoggerFactory
import java.time.Instant

class StartWorkflowUseCaseImpl(
    private val workflowRepository: WorkflowRepository,
    private val instanceRepository: InstanceRepository,
    private val adapterConfigRepository: EngineAdapterConfigRepository,
    private val adapterResolution: AdapterResolutionService,
    private val transactionPort: TransactionPort
) : StartWorkflowUseCase {

    private val log = LoggerFactory.getLogger(StartWorkflowUseCaseImpl::class.java)

    override fun execute(command: Command): WorkflowInstance {
        val workflow = workflowRepository.findById(command.workflowId)
            ?: throw WorkflowNotFoundException(command.workflowId.toString())

        // Select adapter
        val allAdapters = adapterConfigRepository.findByEnabled(true)
        val targetEngine = EngineRoutingLogic.extractTargetEngine(workflow.iwmDefinition.json)
        val adapterConfig = EngineRoutingLogic.selectAdapter(allAdapters, targetEngine)
            ?: throw AdapterUnavailableException("none", "No healthy engine adapter available")

        val adapter = adapterResolution.resolveByConfig(adapterConfig)
        val processKey = workflow.name
        val now = Instant.now()

        // Step 1: Save instance in RUNNING status (inside transaction)
        val instance = transactionPort.executeInTransaction {
            instanceRepository.save(
                WorkflowInstance(
                    id = InstanceId.generate(),
                    workflowId = workflow.id,
                    engineInstanceId = null, // set after engine call
                    engineAdapterId = adapterConfig.id,
                    status = InstanceStatus.RUNNING,
                    businessKey = command.businessKey,
                    variables = command.variables.takeIf { it.isNotEmpty() },
                    startedAt = now,
                    startedBy = command.startedBy,
                    endedAt = null,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        // Step 2: Deploy + start on engine (outside transaction — engine call)
        return try {
            adapter.deployWorkflow(workflow.iwmDefinition.json, processKey, workflow.version)
            val engineInstanceId = adapter.startInstance(processKey, command.businessKey, command.variables)

            log.info("Started workflow instance {} on engine {}", instance.id, adapterConfig.engineType)

            // Step 3: Update with engine instance ID
            transactionPort.executeInTransaction {
                instanceRepository.update(instance.copy(engineInstanceId = engineInstanceId, updatedAt = Instant.now()))
            }
        } catch (e: Exception) {
            log.error("Engine call failed for instance {}: {}", instance.id, e.message)
            // Mark as FAILED so the instance is not orphaned
            transactionPort.executeInTransaction {
                instanceRepository.update(instance.copy(status = InstanceStatus.FAILED, endedAt = Instant.now(), updatedAt = Instant.now()))
            }
            throw e
        }
    }
}
