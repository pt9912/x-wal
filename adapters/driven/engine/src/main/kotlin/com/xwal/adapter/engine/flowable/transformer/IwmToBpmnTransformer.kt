package com.xwal.adapter.engine.flowable.transformer

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.jgrapht.alg.cycle.CycleDetector
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.slf4j.LoggerFactory

class IwmToBpmnTransformer {

    private val log = LoggerFactory.getLogger(IwmToBpmnTransformer::class.java)
    private val objectMapper = ObjectMapper()

    private data class FlowCtx(val outgoing: Map<String, List<String>>, val incoming: Map<String, List<String>>)

    fun transformToBpmn(iwmJson: String, processName: String, processVersion: String): String {
        val iwm = objectMapper.readTree(iwmJson)
        val processId = sanitizeId(processName)
        val tasks = iwm.get("tasks") ?: throw IllegalArgumentException("IWM has no tasks")
        val edges = iwm.get("edges") ?: throw IllegalArgumentException("IWM has no edges")
        val layouts = iwm.path("layouts").firstOrNull()

        validateWorkflowGraph(iwm)
        val ctx = FlowCtx(buildFlowMap(edges, true), buildFlowMap(edges, false))

        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<bpmn:definitions xmlns:bpmn="$BPMN_NS" xmlns:bpmndi="$BPMNDI_NS" xmlns:dc="$DC_NS" xmlns:di="$DI_NS" xmlns:xsi="$XSI_NS" xmlns:flowable="$FLOWABLE_NS" id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn" exporter="x-wal" exporterVersion="2.0.0">""")
            appendLine("""  <bpmn:process id="$processId" name="${escapeXml(processName)}" isExecutable="true">""")
            for (task in tasks) appendTask(this, task, ctx)
            for (edge in edges) appendSequenceFlow(this, edge)
            appendLine("  </bpmn:process>")
            if (layouts != null) appendBpmnDiagram(this, processId, layouts)
            appendLine("</bpmn:definitions>")
        }
    }

    private fun appendTask(b: StringBuilder, task: JsonNode, ctx: FlowCtx) {
        val id = sanitizeId(task.get("id")?.asText() ?: return)
        val name = escapeXml(task.get("label")?.asText() ?: task.get("name")?.asText() ?: id)
        when (task.get("type")?.asText()?.lowercase() ?: "task") {
            "start" -> startEvent(b, id, name, ctx)
            "end" -> endEvent(b, id, name, ctx)
            "usertask", "user_task" -> userTask(b, id, name, task, ctx)
            "servicetask", "service_task" -> serviceTask(b, id, name, task, ctx)
            "decision", "gateway" -> gateway(b, id, name, "exclusiveGateway", ctx)
            "parallel", "parallel_gateway" -> gateway(b, id, name, "parallelGateway", ctx)
            "script", "script_task" -> scriptTask(b, id, name, task, ctx)
            "activity" -> when (task.get("operation")?.asText()?.lowercase()) {
                "startprocess", "start_process" -> startEvent(b, id, name, ctx)
                "endprocess", "end_process" -> endEvent(b, id, name, ctx)
                else -> genericTask(b, id, name, ctx)
            }
            "event" -> when (task.get("kind")?.asText()?.lowercase()) {
                "signal", "end" -> endEvent(b, id, name, ctx)
                else -> startEvent(b, id, name, ctx)
            }
            else -> genericTask(b, id, name, ctx)
        }
    }

    private fun startEvent(b: StringBuilder, id: String, name: String, ctx: FlowCtx) {
        b.appendLine("""    <bpmn:startEvent id="$id" name="$name">""")
        ctx.outgoing[id]?.forEach { b.appendLine("""      <bpmn:outgoing>$it</bpmn:outgoing>""") }
        b.appendLine("    </bpmn:startEvent>")
    }
    private fun endEvent(b: StringBuilder, id: String, name: String, ctx: FlowCtx) {
        b.appendLine("""    <bpmn:endEvent id="$id" name="$name">""")
        ctx.incoming[id]?.forEach { b.appendLine("""      <bpmn:incoming>$it</bpmn:incoming>""") }
        b.appendLine("    </bpmn:endEvent>")
    }
    private fun userTask(b: StringBuilder, id: String, name: String, task: JsonNode, ctx: FlowCtx) {
        val ext = task.path("extensions").let { if (it.has("flowable")) it.path("flowable") else it.path("camunda7") }
        val user = task.path("user")
        b.append("""    <bpmn:userTask id="$id" name="$name"""")
        (ext.path("assignee").asText(null) ?: user.path("assignee").asText(null))?.let { b.append(""" flowable:assignee="${escapeXml(it)}"""") }
        (ext.path("candidateGroups").asText(null) ?: user.path("candidateGroups").asText(null))?.let { b.append(""" flowable:candidateGroups="${escapeXml(it)}"""") }
        (ext.path("formKey").asText(null) ?: user.path("formKey").asText(null))?.let { b.append(""" flowable:formKey="${escapeXml(it)}"""") }
        b.appendLine(">"); flows(b, id, ctx); b.appendLine("    </bpmn:userTask>")
    }
    private fun serviceTask(b: StringBuilder, id: String, name: String, task: JsonNode, ctx: FlowCtx) {
        val ext = task.path("extensions").let { if (it.has("flowable")) it.path("flowable") else it.path("camunda7") }
        b.append("""    <bpmn:serviceTask id="$id" name="$name"""")
        ext.path("class").asText(null)?.let { b.append(""" flowable:class="${escapeXml(it)}"""") }
        ext.path("delegateExpression").asText(null)?.let { b.append(""" flowable:delegateExpression="${escapeXml(it)}"""") }
        b.appendLine(">"); flows(b, id, ctx); b.appendLine("    </bpmn:serviceTask>")
    }
    private fun scriptTask(b: StringBuilder, id: String, name: String, task: JsonNode, ctx: FlowCtx) {
        val ext = task.path("extensions").let { if (it.has("flowable")) it.path("flowable") else it.path("camunda7") }
        b.appendLine("""    <bpmn:scriptTask id="$id" name="$name" scriptFormat="${ext.path("scriptFormat").asText("groovy")}">""")
        ext.path("script").asText(null)?.let { b.appendLine("""      <bpmn:script><![CDATA[$it]]></bpmn:script>""") }
        flows(b, id, ctx); b.appendLine("    </bpmn:scriptTask>")
    }
    private fun gateway(b: StringBuilder, id: String, name: String, type: String, ctx: FlowCtx) {
        b.appendLine("""    <bpmn:$type id="$id" name="$name">"""); flows(b, id, ctx); b.appendLine("    </bpmn:$type>")
    }
    private fun genericTask(b: StringBuilder, id: String, name: String, ctx: FlowCtx) {
        b.appendLine("""    <bpmn:task id="$id" name="$name">"""); flows(b, id, ctx); b.appendLine("    </bpmn:task>")
    }
    private fun flows(b: StringBuilder, id: String, ctx: FlowCtx) {
        ctx.incoming[id]?.forEach { b.appendLine("""      <bpmn:incoming>$it</bpmn:incoming>""") }
        ctx.outgoing[id]?.forEach { b.appendLine("""      <bpmn:outgoing>$it</bpmn:outgoing>""") }
    }
    private fun appendSequenceFlow(b: StringBuilder, edge: JsonNode) {
        val from = sanitizeId(edge.get("from")?.asText() ?: return); val to = sanitizeId(edge.get("to")?.asText() ?: return)
        val flowId = "Flow_${from}_$to"; val cond = edge.path("condition").asText(null) ?: edge.path("expr").path("source").asText(null)
        if (cond != null) {
            b.appendLine("""    <bpmn:sequenceFlow id="$flowId" sourceRef="$from" targetRef="$to">""")
            b.appendLine("""      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression" language="${edge.path("expr").path("lang").asText("juel")}">${escapeXml(cond)}</bpmn:conditionExpression>""")
            b.appendLine("    </bpmn:sequenceFlow>")
        } else b.appendLine("""    <bpmn:sequenceFlow id="$flowId" sourceRef="$from" targetRef="$to" />""")
    }
    private fun appendBpmnDiagram(b: StringBuilder, processId: String, layout: JsonNode) {
        b.appendLine("""  <bpmndi:BPMNDiagram id="BPMNDiagram_1">""")
        b.appendLine("""    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="$processId">""")
        layout.path("nodes")?.forEach { n ->
            val elId = n.path("elementId").asText(null) ?: n.path("id").asText(null) ?: return@forEach
            b.appendLine("""      <bpmndi:BPMNShape id="${elId}_di" bpmnElement="$elId"><dc:Bounds x="${n.path("bounds").path("x").asInt(0)}" y="${n.path("bounds").path("y").asInt(0)}" width="${n.path("bounds").path("width").asInt(100)}" height="${n.path("bounds").path("height").asInt(80)}" /></bpmndi:BPMNShape>""")
        }
        layout.path("edges")?.forEach { e ->
            val from = e.path("from").asText(null) ?: return@forEach; val to = e.path("to").asText(null) ?: return@forEach
            b.appendLine("""      <bpmndi:BPMNEdge id="Flow_${sanitizeId(from)}_${sanitizeId(to)}_di" bpmnElement="Flow_${sanitizeId(from)}_${sanitizeId(to)}">""")
            e.path("waypoints")?.forEach { wp -> b.appendLine("""        <di:waypoint x="${wp.path("x").asInt()}" y="${wp.path("y").asInt()}" />""") }
            b.appendLine("      </bpmndi:BPMNEdge>")
        }
        b.appendLine("    </bpmndi:BPMNPlane>"); b.appendLine("  </bpmndi:BPMNDiagram>")
    }
    private fun validateWorkflowGraph(iwm: JsonNode) {
        val tasks = iwm.get("tasks") ?: return; val edges = iwm.get("edges") ?: return
        val taskIds = tasks.mapNotNull { it.get("id")?.asText() }.toSet()
        val graph = DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge::class.java)
        taskIds.forEach { graph.addVertex(it) }
        for (edge in edges) { val from = edge.get("from")?.asText() ?: continue; val to = edge.get("to")?.asText() ?: continue
            require(from in taskIds) { "Edge references unknown task: $from" }; require(to in taskIds) { "Edge references unknown task: $to" }
            if (from != to) graph.addEdge(from, to) }
        if (CycleDetector(graph).detectCycles()) log.warn("Workflow graph contains cycles — valid for BPMN")
    }
    private fun buildFlowMap(edges: JsonNode, outgoing: Boolean): Map<String, List<String>> {
        val map = mutableMapOf<String, MutableList<String>>()
        for (edge in edges) { val from = sanitizeId(edge.get("from")?.asText() ?: continue); val to = sanitizeId(edge.get("to")?.asText() ?: continue)
            map.getOrPut(if (outgoing) from else to) { mutableListOf() }.add("Flow_${from}_$to") }
        return map
    }
    private fun sanitizeId(input: String) = input.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    private fun escapeXml(input: String) = input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
    companion object {
        private const val BPMN_NS = "http://www.omg.org/spec/BPMN/20100524/MODEL"; private const val BPMNDI_NS = "http://www.omg.org/spec/BPMN/20100524/DI"
        private const val DC_NS = "http://www.omg.org/spec/DD/20100524/DC"; private const val DI_NS = "http://www.omg.org/spec/DD/20100524/DI"
        private const val XSI_NS = "http://www.w3.org/2001/XMLSchema-instance"; private const val FLOWABLE_NS = "http://flowable.org/bpmn"
    }
}
