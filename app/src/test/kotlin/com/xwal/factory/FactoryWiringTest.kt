package com.xwal.factory

import com.xwal.application.service.AdapterResolutionService
import com.xwal.config.InstanceSyncConfig
import com.xwal.decorator.MeteredUseCaseDecorator
import com.xwal.decorator.TracedUseCaseDecorator
import com.xwal.domain.model.*
import com.xwal.domain.port.input.*
import com.xwal.domain.port.output.*
import com.xwal.domain.service.IwmValidationService
import io.mockk.*
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.LongCounterBuilder
import io.opentelemetry.api.metrics.LongHistogram
import io.opentelemetry.api.metrics.LongHistogramBuilder
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Scope
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FactoryWiringTest {

    private val workflowRepo = mockk<WorkflowRepository>()
    private val instanceRepo = mockk<InstanceRepository>()
    private val adapterConfigRepo = mockk<EngineAdapterConfigRepository>()
    private val transactionPort = mockk<TransactionPort>()
    private val adapterCache = mockk<AdapterInstanceCachePort>()
    private val adapterFactory = mockk<EngineAdapterFactoryPort>()
    private val distributedLock = mockk<DistributedLockPort>()
    private val tracer = mockk<Tracer>()
    private val meter = mockk<Meter>()
    private val iwmValidationService = mockk<IwmValidationService>()
    private val enginePort = mockk<WorkflowEnginePort>(relaxed = true)

    private lateinit var adapterResolution: AdapterResolutionService
    private lateinit var workflow: Workflow
    private lateinit var instance: WorkflowInstance
    private lateinit var adapterConfig: EngineAdapterConfig
    private lateinit var task: Task

    @BeforeEach
    fun setup() {
        val now = Instant.now()
        workflow = Workflow(
            id = WorkflowId.generate(),
            name = "Urlaubsantrag",
            version = "1.0.0",
            description = "Test",
            iwmDefinition = IwmDefinition("{}"),
            status = WorkflowStatus.ACTIVE,
            createdAt = now,
            createdBy = null,
            updatedAt = now,
            updatedBy = null
        )
        instance = WorkflowInstance(
            id = InstanceId.generate(),
            workflowId = workflow.id,
            engineInstanceId = "engine-instance-1",
            engineAdapterId = EngineAdapterId.generate(),
            status = InstanceStatus.RUNNING,
            businessKey = null,
            variables = emptyMap(),
            startedAt = now,
            startedBy = null,
            endedAt = null,
            createdAt = now,
            updatedAt = now
        )
        adapterConfig = EngineAdapterConfig(
            id = instance.engineAdapterId!!,
            name = "camunda7",
            engineType = EngineType.CAMUNDA7,
            config = mapOf("url" to "http://localhost:8080"),
            capabilities = null,
            enabled = true,
            healthStatus = HealthStatus.HEALTHY,
            lastHealthCheck = now,
            priority = 0,
            createdAt = now,
            createdBy = null,
            updatedAt = now,
            updatedBy = null
        )
        task = Task(
            taskId = TaskId.of(EngineType.CAMUNDA7, "task-1"),
            name = "Approve request",
            type = "userTask",
            description = "Approve vacation request",
            assignee = null,
            owner = null,
            processInstanceId = instance.engineInstanceId,
            processDefinitionKey = workflow.name,
            status = TaskStatus.CREATED,
            createdAt = now,
            dueDate = null,
            followUpDate = null
        )

        adapterResolution = AdapterResolutionService(adapterConfigRepo, adapterCache, adapterFactory)

        // Mocks for tracing
        val span = mockk<Span>(relaxed = true)
        val scope = mockk<Scope>(relaxed = true)
        val spanBuilder = mockk<SpanBuilder>(relaxed = true)
        every { tracer.spanBuilder(any()) } returns spanBuilder
        every { spanBuilder.startSpan() } returns span
        every { span.makeCurrent() } returns scope

        // Mocks for metrics
        val counterBuilder = mockk<LongCounterBuilder>(relaxed = true)
        val counter = mockk<LongCounter>(relaxed = true)
        val histBuilder = mockk<LongHistogramBuilder>(relaxed = true)
        val hist = mockk<LongHistogram>(relaxed = true)
        every { meter.counterBuilder(any()) } returns counterBuilder
        every { counterBuilder.build() } returns counter
        every { meter.histogramBuilder(any()) } returns mockk(relaxed = true) {
            every { ofLongs() } returns histBuilder
        }
        every { histBuilder.build() } returns hist

        // Repository / infrastructure stubs
        every { workflowRepo.save(any()) } answers { firstArg() }
        every { workflowRepo.findById(any()) } returns workflow
        every { workflowRepo.findAll() } returns listOf(workflow)
        every { workflowRepo.findAllByStatus(any()) } returns listOf(workflow)
        every { workflowRepo.update(any()) } answers { firstArg() }
        every { workflowRepo.existsByNameAndVersion(any(), any()) } returns false
        every { workflowRepo.deleteById(any()) } just Runs

        every { instanceRepo.save(any()) } answers { firstArg() }
        every { instanceRepo.findById(any()) } returns instance
        every { instanceRepo.update(any()) } answers { firstArg() }
        every { instanceRepo.findByEngineAdapterId(any()) } returns listOf(instance)

        every { adapterConfigRepo.save(any()) } answers { firstArg() }
        every { adapterConfigRepo.findById(any()) } returns adapterConfig
        every { adapterConfigRepo.findByEngineType(any()) } returns listOf(adapterConfig)
        every { adapterConfigRepo.findByHealthStatus(HealthStatus.HEALTHY) } returns listOf(adapterConfig)
        every { adapterConfigRepo.findByEnabled(true) } returns listOf(adapterConfig)
        every { adapterConfigRepo.update(any()) } answers { firstArg() }
        every { adapterCache.get(any()) } returns enginePort
        every { adapterCache.put(any(), any()) } just Runs
        every { adapterCache.evict(any()) } just Runs
        every { enginePort.getInstanceVariables(any()) } returns mapOf("foo" to "bar")
        every { enginePort.deployWorkflow(any(), any(), any()) } returns "deployment-1"
        every { enginePort.startInstance(any(), any(), any()) } returns "engine-instance-2"
        every { enginePort.suspendInstance(any()) } just Runs
        every { enginePort.resumeInstance(any()) } just Runs
        every { enginePort.cancelInstance(any(), any()) } just Runs
        every { enginePort.queryTasks(any()) } returns listOf(task)
        every { enginePort.getTask(any()) } returns task
        every { enginePort.completeTask(any(), any()) } just Runs
        every { enginePort.assignTask(any(), any()) } just Runs
        every { enginePort.checkHealth() } returns true
        every { iwmValidationService.validateOrThrow(any()) } just Runs

        every { transactionPort.executeInTransaction(any<() -> Any?>()) } answers {
            firstArg<() -> Any?>().invoke()
        }

        every { distributedLock.withLock(any(), any(), any<() -> Any?>()) } answers {
            thirdArg<() -> Any?>().invoke()
        }
        every { instanceRepo.findForSync(any(), any(), any()) } returns emptyList()
        every { adapterFactory.createAdapter(any(), any()) } returns enginePort
    }

    @Test
    fun `WorkflowUseCaseFactory creates all use cases and executes instrumented read-only paths`() {
        val factory = WorkflowUseCaseFactory()

        val getWorkflow = factory.getWorkflowUseCase(workflowRepo, tracer, meter)
        val listWorkflows = factory.listWorkflowsUseCase(workflowRepo, tracer, meter)
        val getInstance = factory.getInstanceUseCase(instanceRepo, tracer, meter)
        val getInstanceVariables = factory.getInstanceVariablesUseCase(instanceRepo, adapterResolution, tracer, meter)

        val traced = TracedUseCaseDecorator<String, String>(tracer, "test.op") { "result: $it" }
        val metered = MeteredUseCaseDecorator<String, String>(meter, "test.op") { "result: $it" }
        assertNotNull(traced.execute("ok"))
        assertNotNull(metered.execute("ok"))

        getWorkflow.execute(workflow.id)
        listWorkflows.execute(WorkflowStatus.ACTIVE)
        getInstance.execute(instance.id)
        getInstanceVariables.execute(instance.id)

        verify { tracer.spanBuilder("workflow.get") }
        verify { tracer.spanBuilder("workflow.list") }
        verify { tracer.spanBuilder("instance.get") }
        verify { tracer.spanBuilder("instance.variables") }
    }

    @Test
    fun `TaskUseCaseFactory creates all use cases and supports read-only query instrumentation`() {
        val factory = TaskUseCaseFactory()
        val queryTasks = factory.queryTasksUseCase(adapterConfigRepo, adapterResolution, tracer, meter)

        val filter = TaskFilter(limit = 2)
        val tasks = queryTasks.execute(filter)
        assertNotNull(tasks)
        assertEquals(1, tasks.size)
        verify { tracer.spanBuilder("task.query") }
    }

    @Test
    fun `AdapterUseCaseFactory creates all use cases and executes observability wrappers`() {
        val syncConfig = InstanceSyncConfig()
        val factory = AdapterUseCaseFactory()
        val healthCheck = factory.healthCheckAdapterUseCase(adapterConfigRepo, adapterResolution, tracer, meter)
        val syncInstances = factory.syncInstanceStateUseCase(instanceRepo, adapterResolution, distributedLock, syncConfig, tracer, meter)

        assertEquals(HealthStatus.HEALTHY, healthCheck.executeSingle(adapterConfig.id).healthStatus)
        assertEquals(1, healthCheck.executeAll().size)
        assertNotNull(syncInstances.execute())

        verify { tracer.spanBuilder("adapter.healthcheck") }
        verify { tracer.spanBuilder("adapter.sync_instances") }
    }

    @Test
    fun `WorkflowUseCaseFactory creates write use cases and executes instrumented mutation paths`() {
        every { instanceRepo.findById(instance.id) } returnsMany listOf(
            instance.copy(status = InstanceStatus.RUNNING),
            instance.copy(status = InstanceStatus.SUSPENDED),
            instance.copy(status = InstanceStatus.RUNNING)
        )

        val factory = WorkflowUseCaseFactory()
        val createWorkflow = factory.createWorkflowUseCase(workflowRepo, transactionPort, iwmValidationService, tracer, meter)
        val updateWorkflow = factory.updateWorkflowUseCase(workflowRepo, transactionPort, tracer, meter)
        val deleteWorkflow = factory.deleteWorkflowUseCase(workflowRepo, transactionPort, tracer, meter)
        val startWorkflow = factory.startWorkflowUseCase(
            workflowRepo,
            instanceRepo,
            adapterConfigRepo,
            adapterResolution,
            transactionPort,
            tracer,
            meter
        )
        val suspendInstance = factory.suspendInstanceUseCase(instanceRepo, adapterResolution, transactionPort, tracer, meter)
        val resumeInstance = factory.resumeInstanceUseCase(instanceRepo, adapterResolution, transactionPort, tracer, meter)
        val cancelInstance = factory.cancelInstanceUseCase(instanceRepo, adapterResolution, transactionPort, tracer, meter)

        val created = createWorkflow.execute(
            CreateWorkflowUseCase.Command(
                name = "Genehmigung",
                version = "2.0.0",
                description = "Neu",
                iwmDefinition = """{"workflow":{"name":"Genehmigung"}}""",
                createdBy = "tester"
            )
        )
        val updated = updateWorkflow.execute(
            UpdateWorkflowUseCase.Command(
                workflowId = workflow.id,
                description = "Updated description",
                status = WorkflowStatus.DEPRECATED,
                updatedBy = "tester"
            )
        )
        deleteWorkflow.execute(workflow.id)
        val started = startWorkflow.execute(
            StartWorkflowUseCase.Command(
                workflowId = workflow.id,
                businessKey = "bk-1",
                variables = mapOf("approved" to true),
                startedBy = "tester"
            )
        )
        val suspended = suspendInstance.execute(instance.id)
        val resumed = resumeInstance.execute(instance.id)
        val cancelled = cancelInstance.execute(instance.id, "No longer needed")

        assertEquals("Genehmigung", created.name)
        assertEquals("Updated description", updated.description)
        assertEquals(WorkflowStatus.DEPRECATED, updated.status)
        assertEquals("engine-instance-2", started.engineInstanceId)
        assertEquals(InstanceStatus.SUSPENDED, suspended.status)
        assertEquals(InstanceStatus.RUNNING, resumed.status)
        assertEquals(InstanceStatus.CANCELLED, cancelled.status)

        verify { tracer.spanBuilder("workflow.create") }
        verify { tracer.spanBuilder("workflow.update") }
        verify { tracer.spanBuilder("workflow.delete") }
        verify { tracer.spanBuilder("workflow.start") }
        verify { tracer.spanBuilder("instance.suspend") }
        verify { tracer.spanBuilder("instance.resume") }
        verify { tracer.spanBuilder("instance.cancel") }
    }

    @Test
    fun `Task and adapter factories execute remaining mutation paths`() {
        val taskFactory = TaskUseCaseFactory()
        val completeTask = taskFactory.completeTaskUseCase(adapterResolution, tracer, meter)
        val assignTask = taskFactory.assignTaskUseCase(adapterResolution, tracer, meter)
        val getTask = taskFactory.getTaskUseCase(adapterResolution, tracer, meter)

        val taskId = TaskId.of(EngineType.CAMUNDA7, adapterConfig.id, "task-1")
        completeTask.execute(CompleteTaskUseCase.Command(taskId, mapOf("approved" to true)))
        assignTask.execute(taskId, "alice")
        val resolvedTask = getTask.execute(taskId)
        assertEquals(taskId, resolvedTask.taskId)

        val adapterFactory = AdapterUseCaseFactory()
        val registerAdapter = adapterFactory.registerAdapterUseCase(
            adapterConfigRepo,
            adapterResolution,
            adapterCache,
            transactionPort,
            tracer,
            meter
        )
        val deregisterAdapter = adapterFactory.deregisterAdapterUseCase(
            adapterConfigRepo,
            adapterCache,
            instanceRepo,
            transactionPort,
            tracer,
            meter
        )

        val registered = registerAdapter.execute(
            RegisterAdapterUseCase.Command(
                name = "flowable",
                engineType = EngineType.FLOWABLE,
                config = "http://localhost:8081",
                priority = 1,
                createdBy = "tester"
            )
        )
        deregisterAdapter.execute(adapterConfig.id)

        assertEquals(HealthStatus.HEALTHY, registered.healthStatus)

        verify { tracer.spanBuilder("task.complete") }
        verify { tracer.spanBuilder("task.assign") }
        verify { tracer.spanBuilder("task.get") }
        verify { tracer.spanBuilder("adapter.register") }
        verify { tracer.spanBuilder("adapter.deregister") }
    }

    @Test
    fun `DomainServiceFactory creates services`() {
        val factory = DomainServiceFactory()
        assertNotNull(factory.iwmValidationService())
        assertNotNull(factory.adapterResolutionService(adapterConfigRepo, adapterCache, adapterFactory))
    }
}
