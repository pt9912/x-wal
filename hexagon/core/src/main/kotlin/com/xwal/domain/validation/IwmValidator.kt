package com.xwal.domain.validation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import org.slf4j.LoggerFactory
import java.io.IOException

/**
 * Validates IWM (Intermediate Workflow Model) documents against the JSON Schema v0.2.
 * Uses json-schema-validator 2.0.0 with manual pre-validation for required fields.
 *
 * Pure domain validator — no file I/O. File operations belong in adapters (CLI, etc.).
 */
class IwmValidator(
    private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()
) {

    private val log = LoggerFactory.getLogger(IwmValidator::class.java)
    private val schema = loadSchema()

    private fun loadSchema(): com.networknt.schema.Schema {
        val schemaStream = javaClass.getResourceAsStream(SCHEMA_RESOURCE)
            ?: throw IllegalStateException("IWM Schema not found: $SCHEMA_RESOURCE")

        return schemaStream.use { stream ->
            val schemaNode = objectMapper.readTree(stream)
            val schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
            schemaRegistry.getSchema(schemaNode).also {
                log.info("IwmValidator initialized, schema loaded from {}", SCHEMA_RESOURCE)
            }
        }
    }

    fun validate(iwmJson: String): ValidationResult {
        return try {
            val iwmNode = objectMapper.readTree(iwmJson)

            // Pre-validation: required root fields
            val missingFields = mutableListOf<String>()
            if (!iwmNode.has("workflow")) missingFields.add("$.workflow: is missing but it is required")
            if (!iwmNode.has("tasks")) missingFields.add("$.tasks: is missing but it is required")
            if (!iwmNode.has("edges")) missingFields.add("$.edges: is missing but it is required")

            if (missingFields.isNotEmpty()) {
                return ValidationResult.failure("<string>", missingFields)
            }

            // Pre-validation: required workflow fields
            val workflowNode = iwmNode.get("workflow")
            if (workflowNode != null && workflowNode.isObject) {
                if (!workflowNode.has("name")) missingFields.add("$.workflow.name: is missing but it is required")
                if (!workflowNode.has("version")) missingFields.add("$.workflow.version: is missing but it is required")
                if (missingFields.isNotEmpty()) {
                    return ValidationResult.failure("<string>", missingFields)
                }
            }

            validateNode(iwmNode, "<string>")
        } catch (e: IOException) {
            log.error("Invalid JSON input", e)
            ValidationResult.error("Invalid JSON: ${e.message}")
        }
    }

    private fun validateNode(iwmNode: JsonNode, source: String): ValidationResult {
        val errors = schema.validate(iwmNode)
        return if (errors.isEmpty()) {
            log.info("IWM validation successful: {}", source)
            ValidationResult.success(source)
        } else {
            val errorMessages = errors.map { "${it.instanceLocation}: ${it.message}" }
            log.warn("IWM validation failed: {} ({} errors)", source, errors.size)
            ValidationResult.failure(source, errorMessages)
        }
    }

    companion object {
        private const val SCHEMA_RESOURCE = "/schema/iwm.schema.json"
    }
}
