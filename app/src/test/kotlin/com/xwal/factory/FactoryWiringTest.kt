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
        every { workflowRepo.findById(any()) } returns workflow
        every { workflowRepo.findAll() } returns listOf(workflow)
        every { workflowRepo.findAllByStatus(any()) } returns listOf(workflow)
        every { workflowRepo.update(any()) } returns workflow

        every { instanceRepo.findById(any()) } returns instance
        every { instanceRepo.update(any()) } returns instance

        every { adapterConfigRepo.findByHealthStatus(HealthStatus.HEALTHY) } returns emptyList()
        every { adapterConfigRepo.findByEnabled(true) } returns emptyList()
        every { adapterCache.get(any()) } returns enginePort
        every { enginePort.getInstanceVariables(any()) } returns mapOf("foo" to "bar")

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
        assertEquals(0, tasks.size)
        verify { tracer.spanBuilder("task.query") }
    }

    @Test
    fun `AdapterUseCaseFactory creates all use cases and executes observability wrappers`() {
        val syncConfig = InstanceSyncConfig()
        val factory = AdapterUseCaseFactory()
        val healthCheck = factory.healthCheckAdapterUseCase(adapterConfigRepo, adapterResolution, tracer, meter)
        val syncInstances = factory.syncInstanceStateUseCase(instanceRepo, adapterResolution, distributedLock, syncConfig, tracer, meter)

        assertEquals(emptyList<EngineAdapterConfig>(), healthCheck.executeAll())
        assertNotNull(syncInstances.execute())

        verify { tracer.spanBuilder("adapter.healthcheck") }
        verify { tracer.spanBuilder("adapter.sync_instances") }
    }

    @Test
    fun `DomainServiceFactory creates services`() {
        val factory = DomainServiceFactory()
        assertNotNull(factory.iwmValidationService())
        assertNotNull(factory.adapterResolutionService(adapterConfigRepo, adapterCache, adapterFactory))
    }
}
