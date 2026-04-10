package com.xwal.adapter.engine.factory

import com.xwal.adapter.engine.camunda7.Camunda7Adapter
import com.xwal.adapter.engine.flowable.FlowableAdapter
import com.xwal.domain.exception.AdapterOperationException
import com.xwal.domain.model.EngineType
import com.xwal.domain.port.output.EngineAdapterFactoryPort
import com.xwal.domain.port.output.WorkflowEnginePort
import io.micronaut.http.client.HttpClient
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.net.URL

@Singleton
class EngineAdapterFactoryImpl : EngineAdapterFactoryPort {

    private val log = LoggerFactory.getLogger(EngineAdapterFactoryImpl::class.java)

    override fun createAdapter(engineType: EngineType, config: Map<String, Any>): WorkflowEnginePort {
        val baseUrl = config["url"] as? String
            ?: throw AdapterOperationException(engineType.name, "createAdapter", "Config must contain 'url'")

        if (!supportsEngineType(engineType)) {
            throw AdapterOperationException(engineType.name, "createAdapter", "Unsupported engine type: ${engineType.name}")
        }

        log.info("Creating adapter for {} at {}", engineType, baseUrl)

        return try {
            val httpClient = HttpClient.create(URL(baseUrl))
            when (engineType) {
                EngineType.CAMUNDA7 -> Camunda7Adapter(httpClient, baseUrl)
                EngineType.FLOWABLE -> FlowableAdapter(httpClient, baseUrl)
                else -> error("unreachable")
            }
        } catch (e: AdapterOperationException) {
            throw e
        } catch (e: Exception) {
            throw AdapterOperationException(engineType.name, "createAdapter", "Failed to create adapter: ${e.message}", e)
        }
    }

    override fun supportsEngineType(engineType: EngineType): Boolean =
        engineType in setOf(EngineType.CAMUNDA7, EngineType.FLOWABLE)

    override fun getSupportedEngineTypes(): Set<EngineType> =
        setOf(EngineType.CAMUNDA7, EngineType.FLOWABLE)
}
