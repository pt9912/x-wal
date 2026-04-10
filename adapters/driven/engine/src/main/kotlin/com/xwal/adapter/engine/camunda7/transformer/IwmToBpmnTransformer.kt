package com.xwal.adapter.engine.camunda7.transformer

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.jgrapht.alg.cycle.CycleDetector
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.slf4j.LoggerFactory

class IwmToBpmnTransformer {

    private val log = LoggerFactory.getLogger(IwmToBpmnTransformer::class.java)
    private val objectMapper = ObjectMapper()

    private data class FlowCtx(
        val outgoing: Map<String, List<String>>,
        val incoming: Map<String, List<String>>
    )

    fun transformToBpmn(iwmJson: String, processName: String, processVersion: String): String {
        val iwm = objectMapper.readTree(iwmJson)
        val processId = sanitizeId(processName)
        val tasks = iwm.get("tasks") ?: throw IllegalArgumentException("IWM has no tasks")
        val edges = iwm.get("edges") ?: throw IllegalArgumentException("IWM has no edges")
        val layouts = iwm.path("layouts").firstOrNull()

        validateWorkflowGraph(iwm)

        val ctx = FlowCtx(buildFlowMap(edges, true), buildFlowMap(edges, false))

        val bpmn = StringBuilder()
        bpmn.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        bpmn.appendLine("""<bpmn:definitions xmlns:bpmn="$BPMN_NS" xmlns:bpmndi="$BPMNDI_NS" xmlns:dc="$DC_NS" xmlns:di="$DI_NS" xmlns:xsi="$XSI_NS" xmlns:camunda="$CAMUNDA_NS" id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn" exporter="x-wal" exporterVersion="2.0.0">""")
        bpmn.appendLine("""  <bpmn:process id="$processId" name="${escapeXml(processName)}" isExecutable="true" camunda:versionTag="$processVersion" camunda:historyTimeToLive="P30D">""")

        for (task in tasks) appendTask(bpmn, task, ctx)
        for (edge in edges) appendSequenceFlow(bpmn, edge)

        bpmn.appendLine("  </bpmn:process>")
        if (layouts != null) appendBpmnDiagram(bpmn, processId, layouts, tasks)
        bpmn.appendLine("</bpmn:definitions>")
        return bpmn.toString()
    }

    private fun appendTask(bpmn: StringBuilder, task: JsonNode, ctx: FlowCtx) {
        val id = sanitizeId(task.get("id")?.asText() ?: return)
        val name = escapeXml(task.get("label")?.asText() ?: task.get("name")?.asText() ?: id)
        val type = task.get("type")?.asText()?.lowercase() ?: "task"
        val operation = task.get("operation")?.asText()?.lowercase()

        when (type) {
            "start" -> appendStartEvent(bpmn, id, name, ctx)
            "end" -> appendEndEvent(bpmn, id, name, ctx)
            "usertask", "user_task" -> appendUserTask(bpmn, id, name, task, ctx)
            "servicetask", "service_task" -> appendServiceTask(bpmn, id, name, task, ctx)
            "decision", "gateway" -> appendExclusiveGateway(bpmn, id, name, ctx)
            "parallel", "parallel_gateway" -> appendParallelGateway(bpmn, id, name, ctx)
            "script", "script_task" -> appendScriptTask(bpmn, id, name, task, ctx)
            "activity" -> when (operation) {
                "startprocess", "start_process" -> appendStartEvent(bpmn, id, name, ctx)
                "endprocess", "end_process" -> appendEndEvent(bpmn, id, name, ctx)
                "service", "execute" -> appendServiceTask(bpmn, id, name, task, ctx)
                else -> appendGenericTask(bpmn, id, name, ctx)
            }
            "event" -> {
                val kind = task.get("kind")?.asText()?.lowercase()
                when (kind) {
                    "message", "start", "none", null -> appendStartEvent(bpmn, id, name, ctx)
                    "signal", "end" -> appendEndEvent(bpmn, id, name, ctx)
                    else -> appendStartEvent(bpmn, id, name, ctx)
                }
            }
            else -> appendGenericTask(bpmn, id, name, ctx)
        }
    }

    private fun appendStartEvent(bpmn: StringBuilder, id: String, name: String, ctx: FlowCtx) {
        bpmn.appendLine("""    <bpmn:startEvent id="$id" name="$name">""")
        ctx.outgoing[id]?.forEach { bpmn.appendLine("""      <bpmn:outgoing>$it</bpmn:outgoing>""") }
        bpmn.appendLine("    </bpmn:startEvent>")
    }

    private fun appendEndEvent(bpmn: StringBuilder, id: String, name: String, ctx: FlowCtx) {
        bpmn.appendLine("""    <bpmn:endEvent id="$id" name="$name">""")
        ctx.incoming[id]?.forEach { bpmn.appendLine("""      <bpmn:incoming>$it</bpmn:incoming>""") }
        bpmn.appendLine("    </bpmn:endEvent>")
    }

    private fun appendUserTask(bpmn: StringBuilder, id: String, name: String, task: JsonNode, ctx: FlowCtx) {
        val ext = task.path("extensions").path("camunda7")
        val assignee = ext.path("assignee").asText(null) ?: task.path("user").path("assignee").asText(null)
        val candidateGroups = ext.path("candidateGroups").asText(null) ?: task.path("user").path("candidateGroups").asText(null)
        val formKey = ext.path("formKey").asText(null) ?: task.path("user").path("formKey").asText(null)

        bpmn.append("""    <bpmn:userTask id="$id" name="$name"""")
        assignee?.let { bpmn.append(""" camunda:assignee="${escapeXml(it)}"""") }
        candidateGroups?.let { bpmn.append(""" camunda:candidateGroups="${escapeXml(it)}"""") }
        formKey?.let { bpmn.append(""" camunda:formKey="${escapeXml(it)}"""") }
        bpmn.appendLine(">")
        appendFlows(bpmn, id, ctx)
        bpmn.appendLine("    </bpmn:userTask>")
    }

    private fun appendServiceTask(bpmn: StringBuilder, id: String, name: String, task: JsonNode, ctx: FlowCtx) {
        val ext = task.path("extensions").path("camunda7")
        val clazz = ext.path("class").asText(null)
        val delegateExpr = ext.path("delegateExpression").asText(null)

        bpmn.append("""    <bpmn:serviceTask id="$id" name="$name"""")
        clazz?.let { bpmn.append(""" camunda:class="${escapeXml(it)}"""") }
        delegateExpr?.let { bpmn.append(""" camunda:delegateExpression="${escapeXml(it)}"""") }
        bpmn.appendLine(">")
        appendFlows(bpmn, id, ctx)
        bpmn.appendLine("    </bpmn:serviceTask>")
    }

    private fun appendScriptTask(bpmn: StringBuilder, id: String, name: String, task: JsonNode, ctx: FlowCtx) {
        val ext = task.path("extensions").path("camunda7")
        val scriptFormat = ext.path("scriptFormat").asText("groovy")
        val script = ext.path("script").asText(null)

        bpmn.appendLine("""    <bpmn:scriptTask id="$id" name="$name" scriptFormat="$scriptFormat">""")
        script?.let { bpmn.appendLine("""      <bpmn:script><![CDATA[$it]]></bpmn:script>""") }
        appendFlows(bpmn, id, ctx)
        bpmn.appendLine("    </bpmn:scriptTask>")
    }

    private fun appendExclusiveGateway(bpmn: StringBuilder, id: String, name: String, ctx: FlowCtx) {
        bpmn.appendLine("""    <bpmn:exclusiveGateway id="$id" name="$name">""")
        appendFlows(bpmn, id, ctx)
        bpmn.appendLine("    </bpmn:exclusiveGateway>")
    }

    private fun appendParallelGateway(bpmn: StringBuilder, id: String, name: String, ctx: FlowCtx) {
        bpmn.appendLine("""    <bpmn:parallelGateway id="$id" name="$name">""")
        appendFlows(bpmn, id, ctx)
        bpmn.appendLine("    </bpmn:parallelGateway>")
    }

    private fun appendGenericTask(bpmn: StringBuilder, id: String, name: String, ctx: FlowCtx) {
        bpmn.appendLine("""    <bpmn:task id="$id" name="$name">""")
        appendFlows(bpmn, id, ctx)
        bpmn.appendLine("    </bpmn:task>")
    }

    private fun appendFlows(bpmn: StringBuilder, id: String, ctx: FlowCtx) {
        ctx.incoming[id]?.forEach { bpmn.appendLine("""      <bpmn:incoming>$it</bpmn:incoming>""") }
        ctx.outgoing[id]?.forEach { bpmn.appendLine("""      <bpmn:outgoing>$it</bpmn:outgoing>""") }
    }

    private fun appendSequenceFlow(bpmn: StringBuilder, edge: JsonNode) {
        val from = sanitizeId(edge.get("from")?.asText() ?: return)
        val to = sanitizeId(edge.get("to")?.asText() ?: return)
        val flowId = "Flow_${from}_$to"
        val condition = edge.path("condition").asText(null) ?: edge.path("expr").path("source").asText(null)

        if (condition != null) {
            val lang = edge.path("expr").path("lang").asText("juel")
            bpmn.appendLine("""    <bpmn:sequenceFlow id="$flowId" sourceRef="$from" targetRef="$to">""")
            bpmn.appendLine("""      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression" language="$lang">${escapeXml(condition)}</bpmn:conditionExpression>""")
            bpmn.appendLine("    </bpmn:sequenceFlow>")
        } else {
            bpmn.appendLine("""    <bpmn:sequenceFlow id="$flowId" sourceRef="$from" targetRef="$to" />""")
        }
    }

    private fun appendBpmnDiagram(bpmn: StringBuilder, processId: String, layout: JsonNode, tasks: JsonNode) {
        bpmn.appendLine("""  <bpmndi:BPMNDiagram id="BPMNDiagram_1">""")
        bpmn.appendLine("""    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="$processId">""")
        layout.path("nodes")?.forEach { node ->
            val elementId = node.path("elementId").asText(null) ?: node.path("id").asText(null) ?: return@forEach
            val x = node.path("bounds").path("x").asInt(0)
            val y = node.path("bounds").path("y").asInt(0)
            val w = node.path("bounds").path("width").asInt(100)
            val h = node.path("bounds").path("height").asInt(80)
            bpmn.appendLine("""      <bpmndi:BPMNShape id="${elementId}_di" bpmnElement="$elementId">""")
            bpmn.appendLine("""        <dc:Bounds x="$x" y="$y" width="$w" height="$h" />""")
            bpmn.appendLine("      </bpmndi:BPMNShape>")
        }
        layout.path("edges")?.forEach { edge ->
            val from = edge.path("from").asText(null) ?: return@forEach
            val to = edge.path("to").asText(null) ?: return@forEach
            val edgeId = "Flow_${sanitizeId(from)}_${sanitizeId(to)}"
            bpmn.appendLine("""      <bpmndi:BPMNEdge id="${edgeId}_di" bpmnElement="$edgeId">""")
            edge.path("waypoints")?.forEach { wp ->
                bpmn.appendLine("""        <di:waypoint x="${wp.path("x").asInt()}" y="${wp.path("y").asInt()}" />""")
            }
            bpmn.appendLine("      </bpmndi:BPMNEdge>")
        }
        bpmn.appendLine("    </bpmndi:BPMNPlane>")
        bpmn.appendLine("  </bpmndi:BPMNDiagram>")
    }

    private fun validateWorkflowGraph(iwm: JsonNode) {
        val tasks = iwm.get("tasks") ?: return
        val edges = iwm.get("edges") ?: return
        val taskIds = tasks.mapNotNull { it.get("id")?.asText() }.toSet()
        val graph = DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge::class.java)
        taskIds.forEach { graph.addVertex(it) }
        for (edge in edges) {
            val from = edge.get("from")?.asText() ?: continue
            val to = edge.get("to")?.asText() ?: continue
            require(from in taskIds) { "Edge references unknown task: $from" }
            require(to in taskIds) { "Edge references unknown task: $to" }
            if (from != to) graph.addEdge(from, to)
        }
        // BPMN supports loops — warn but do not reject
        if (CycleDetector(graph).detectCycles()) {
            log.warn("Workflow graph contains cycles — this is valid for BPMN but may indicate a modeling issue")
        }
    }

    private fun buildFlowMap(edges: JsonNode, outgoing: Boolean): Map<String, List<String>> {
        val map = mutableMapOf<String, MutableList<String>>()
        for (edge in edges) {
            val from = sanitizeId(edge.get("from")?.asText() ?: continue)
            val to = sanitizeId(edge.get("to")?.asText() ?: continue)
            val key = if (outgoing) from else to
            map.getOrPut(key) { mutableListOf() }.add("Flow_${from}_$to")
        }
        return map
    }

    private fun sanitizeId(input: String) = input.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    private fun escapeXml(input: String) = input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")

    companion object {
        private const val BPMN_NS = "http://www.omg.org/spec/BPMN/20100524/MODEL"
        private const val BPMNDI_NS = "http://www.omg.org/spec/BPMN/20100524/DI"
        private const val DC_NS = "http://www.omg.org/spec/DD/20100524/DC"
        private const val DI_NS = "http://www.omg.org/spec/DD/20100524/DI"
        private const val XSI_NS = "http://www.w3.org/2001/XMLSchema-instance"
        private const val CAMUNDA_NS = "http://camunda.org/schema/1.0/bpmn"
    }
}
