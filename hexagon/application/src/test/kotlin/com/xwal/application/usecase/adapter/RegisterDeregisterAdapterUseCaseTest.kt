package com.xwal.application.usecase.adapter

import com.xwal.application.service.AdapterResolutionService
import com.xwal.domain.exception.AdapterNotFoundException
import com.xwal.domain.model.*
import com.xwal.domain.port.input.RegisterAdapterUseCase
import com.xwal.domain.port.output.*
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RegisterDeregisterAdapterUseCaseTest {

    private val adapterConfigRepository = mockk<EngineAdapterConfigRepository>()
    private val adapterCache = mockk<AdapterInstanceCachePort>()
    private val adapterFactory = mockk<EngineAdapterFactoryPort>()
    private val instanceRepository = mockk<InstanceRepository>()
    private val transactionPort = mockk<TransactionPort>()
    private val adapter = mockk<WorkflowEnginePort>()

    private val adapterResolution = AdapterResolutionService(adapterConfigRepository, adapterCache, adapterFactory)
    private val registerUseCase = RegisterAdapterUseCaseImpl(adapterConfigRepository, adapterResolution, transactionPort)
    private val deregisterUseCase = DeregisterAdapterUseCaseImpl(adapterConfigRepository, adapterCache, instanceRepository, transactionPort)

    private val adapterId = EngineAdapterId.generate()
    private val now = Instant.now()
    private val config = EngineAdapterConfig(adapterId, "camunda", EngineType.CAMUNDA7, mapOf("url" to "http://localhost"), null, true, HealthStatus.UNKNOWN, null, 0, now, null, now, null)

    @BeforeEach
    fun setup() {
        every { transactionPort.executeInTransaction(any<() -> Any>()) } answers { firstArg<() -> Any>().invoke() }
    }

    @Test
    fun `registers adapter with health check`() {
        every { adapterConfigRepository.save(any()) } answers { firstArg() }
        every { adapterCache.get(any()) } returns null
        every { adapterFactory.createAdapter(EngineType.CAMUNDA7, any()) } returns adapter
        every { adapterCache.put(any(), any()) } just runs
        every { adapter.checkHealth() } returns true
        every { adapterConfigRepository.update(any()) } answers { firstArg() }

        val result = registerUseCase.execute(RegisterAdapterUseCase.Command("test", EngineType.CAMUNDA7, "http://localhost", createdBy = null))
        assertEquals(HealthStatus.HEALTHY, result.healthStatus)
    }

    @Test
    fun `deregisters adapter`() {
        every { adapterConfigRepository.findById(adapterId) } returns config
        every { instanceRepository.findByEngineAdapterId(adapterId) } returns emptyList()
        every { adapterConfigRepository.update(any()) } answers { firstArg() }
        every { adapterCache.evict(adapterId) } just runs

        deregisterUseCase.execute(adapterId)
        verify { adapterCache.evict(adapterId) }
    }

    @Test
    fun `deregister throws when not found`() {
        every { adapterConfigRepository.findById(adapterId) } returns null
        assertFailsWith<AdapterNotFoundException> { deregisterUseCase.execute(adapterId) }
    }
}
