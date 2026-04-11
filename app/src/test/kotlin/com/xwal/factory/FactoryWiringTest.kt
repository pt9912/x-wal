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
import java.time.Instant
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

    private lateinit var adapterResolution: AdapterResolutionService

    @BeforeEach
    fun setup() {
        adapterResolution = AdapterResolutionService(adapterConfigRepo, adapterCache, adapterFactory)
        // Mock tracer chain
        val span = mockk<Span>(relaxed = true)
        val scope = mockk<Scope>(relaxed = true)
        val spanBuilder = mockk<SpanBuilder>(relaxed = true)
        every { tracer.spanBuilder(any()) } returns spanBuilder
        every { spanBuilder.startSpan() } returns span
        every { span.makeCurrent() } returns scope
        // Mock meter chain
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
    }

    @Test fun `WorkflowUseCaseFactory creates all use cases`() {
        val factory = WorkflowUseCaseFactory()
        assertNotNull(factory.createWorkflowUseCase(workflowRepo, transactionPort, iwmValidationService, tracer, meter))
        assertNotNull(factory.getWorkflowUseCase(workflowRepo))
        assertNotNull(factory.listWorkflowsUseCase(workflowRepo))
        assertNotNull(factory.updateWorkflowUseCase(workflowRepo, transactionPort))
        assertNotNull(factory.deleteWorkflowUseCase(workflowRepo, transactionPort))
        assertNotNull(factory.startWorkflowUseCase(workflowRepo, instanceRepo, adapterConfigRepo, adapterResolution, transactionPort, tracer, meter))
        assertNotNull(factory.getInstanceUseCase(instanceRepo))
        assertNotNull(factory.getInstanceVariablesUseCase(instanceRepo, adapterResolution))
        assertNotNull(factory.suspendInstanceUseCase(instanceRepo, adapterResolution, transactionPort, tracer, meter))
        assertNotNull(factory.resumeInstanceUseCase(instanceRepo, adapterResolution, transactionPort, tracer, meter))
        assertNotNull(factory.cancelInstanceUseCase(instanceRepo, adapterResolution, transactionPort, tracer, meter))
    }

    @Test fun `TaskUseCaseFactory creates all use cases`() {
        val factory = TaskUseCaseFactory()
        assertNotNull(factory.queryTasksUseCase(adapterConfigRepo, adapterResolution, tracer, meter))
        assertNotNull(factory.completeTaskUseCase(adapterResolution, tracer, meter))
        assertNotNull(factory.assignTaskUseCase(adapterResolution))
        assertNotNull(factory.getTaskUseCase(adapterResolution))
    }

    @Test fun `AdapterUseCaseFactory creates all use cases`() {
        val syncConfig = InstanceSyncConfig()
        val factory = AdapterUseCaseFactory()
        assertNotNull(factory.registerAdapterUseCase(adapterConfigRepo, adapterResolution, transactionPort))
        assertNotNull(factory.deregisterAdapterUseCase(adapterConfigRepo, adapterCache, instanceRepo, transactionPort))
        assertNotNull(factory.healthCheckAdapterUseCase(adapterConfigRepo, adapterResolution))
        assertNotNull(factory.syncInstanceStateUseCase(instanceRepo, adapterResolution, distributedLock, syncConfig))
    }

    @Test fun `DomainServiceFactory creates services`() {
        val factory = DomainServiceFactory()
        assertNotNull(factory.iwmValidationService())
        assertNotNull(factory.adapterResolutionService(adapterConfigRepo, adapterCache, adapterFactory))
    }

    @Test fun `TracedUseCaseDecorator wraps execution`() {
        val traced = TracedUseCaseDecorator<String, String>(tracer, "test.op") { "result: $it" }
        val result = traced.execute("input")
        assertNotNull(result)
    }

    @Test fun `MeteredUseCaseDecorator wraps execution`() {
        val metered = MeteredUseCaseDecorator<String, String>(meter, "test.op") { "result: $it" }
        val result = metered.execute("input")
        assertNotNull(result)
    }
}
