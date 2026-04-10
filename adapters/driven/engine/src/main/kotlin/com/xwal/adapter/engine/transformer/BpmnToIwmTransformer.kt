package com.xwal.adapter.engine.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Transforms BPMN 2.0 XML to IWM JSON format.
 * Supports: startEvent, endEvent, userTask, serviceTask, exclusiveGateway, sequenceFlow.
 * Extracts BPMN DI layout information for visual preservation.
 */
class BpmnToIwmTransformer {

    private val log = LoggerFactory.getLogger(BpmnToIwmTransformer::class.java)
    private val objectMapper = ObjectMapper()

    fun transformToIwm(bpmnXml: String, engine: String = "unknown"): String {
        val doc = parseXml(bpmnXml)
        val process = doc.getElementsByTagNameNS(BPMN_NS, "process").item(0) as? Element
            ?: throw IllegalArgumentException("No BPMN process element found")

        val processId = process.getAttribute("id") ?: "unknown"
        val processName = process.getAttribute("name") ?: processId

        val iwm = objectMapper.createObjectNode()
        iwm.put("version", "0.2")
        iwm.put("type", "workflow")

        val workflow = objectMapper.createObjectNode()
        workflow.put("name", processName)
        workflow.put("version", "1.0.0")
        iwm.set<ObjectNode>("workflow", workflow)

        val tasks = extractTasks(doc, process)
        iwm.set<ArrayNode>("tasks", tasks.first)
        iwm.set<ArrayNode>("edges", tasks.second)

        val layouts = extractLayouts(doc, processId, engine)
        if (layouts.size() > 0) iwm.set<ArrayNode>("layouts", layouts)

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(iwm)
    }

    private fun extractTasks(doc: Document, process: Element): Pair<ArrayNode, ArrayNode> {
        val tasks = objectMapper.createArrayNode()
        val edges = objectMapper.createArrayNode()

        extractElements(process, "startEvent", "start").forEach { tasks.add(it) }
        extractElements(process, "userTask", "userTask").forEach { tasks.add(it) }
        extractElements(process, "serviceTask", "serviceTask").forEach { tasks.add(it) }
        extractElements(process, "exclusiveGateway", "decision").forEach { tasks.add(it) }
        extractElements(process, "endEvent", "end").forEach { tasks.add(it) }

        val flows = process.getElementsByTagNameNS(BPMN_NS, "sequenceFlow")
        for (i in 0 until flows.length) {
            val flow = flows.item(i) as Element
            val edge = objectMapper.createObjectNode()
            edge.put("from", flow.getAttribute("sourceRef"))
            edge.put("to", flow.getAttribute("targetRef"))
            val conditions = flow.getElementsByTagNameNS(BPMN_NS, "conditionExpression")
            if (conditions.length > 0) {
                edge.put("condition", conditions.item(0).textContent)
            }
            edges.add(edge)
        }

        return tasks to edges
    }

    private fun extractElements(process: Element, bpmnType: String, iwmType: String): List<ObjectNode> {
        val elements = process.getElementsByTagNameNS(BPMN_NS, bpmnType)
        return (0 until elements.length).map { i ->
            val el = elements.item(i) as Element
            objectMapper.createObjectNode().apply {
                put("id", el.getAttribute("id"))
                put("type", iwmType)
                val name = el.getAttribute("name")
                if (name.isNotBlank()) put("label", name)
            }
        }
    }

    private fun extractLayouts(doc: Document, processId: String, engine: String): ArrayNode {
        val layouts = objectMapper.createArrayNode()
        val diagrams = doc.getElementsByTagNameNS(BPMNDI_NS, "BPMNDiagram")
        if (diagrams.length == 0) return layouts

        val layout = objectMapper.createObjectNode()
        layout.put("engine", engine)

        val nodes = objectMapper.createArrayNode()
        val edges = objectMapper.createArrayNode()

        val shapes = doc.getElementsByTagNameNS(BPMNDI_NS, "BPMNShape")
        for (i in 0 until shapes.length) {
            val shape = shapes.item(i) as Element
            val node = objectMapper.createObjectNode()
            node.put("elementId", shape.getAttribute("bpmnElement"))
            val bounds = shape.getElementsByTagNameNS(DC_NS, "Bounds")
            if (bounds.length > 0) {
                val b = bounds.item(0) as Element
                val boundsNode = objectMapper.createObjectNode()
                boundsNode.put("x", b.getAttribute("x").toDoubleOrNull()?.toInt() ?: 0)
                boundsNode.put("y", b.getAttribute("y").toDoubleOrNull()?.toInt() ?: 0)
                boundsNode.put("width", b.getAttribute("width").toDoubleOrNull()?.toInt() ?: 100)
                boundsNode.put("height", b.getAttribute("height").toDoubleOrNull()?.toInt() ?: 80)
                node.set<ObjectNode>("bounds", boundsNode)
            }
            nodes.add(node)
        }

        val bpmnEdges = doc.getElementsByTagNameNS(BPMNDI_NS, "BPMNEdge")
        for (i in 0 until bpmnEdges.length) {
            val bpmnEdge = bpmnEdges.item(i) as Element
            val edge = objectMapper.createObjectNode()
            edge.put("bpmnElement", bpmnEdge.getAttribute("bpmnElement"))
            val waypoints = objectMapper.createArrayNode()
            val wps = bpmnEdge.getElementsByTagNameNS(DI_NS, "waypoint")
            for (j in 0 until wps.length) {
                val wp = wps.item(j) as Element
                val point = objectMapper.createObjectNode()
                point.put("x", wp.getAttribute("x").toDoubleOrNull()?.toInt() ?: 0)
                point.put("y", wp.getAttribute("y").toDoubleOrNull()?.toInt() ?: 0)
                waypoints.add(point)
            }
            edge.set<ArrayNode>("waypoints", waypoints)
            edges.add(edge)
        }

        layout.set<ArrayNode>("nodes", nodes)
        layout.set<ArrayNode>("edges", edges)
        layouts.add(layout)
        return layouts
    }

    private fun parseXml(xml: String): Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            // XXE protection
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        }
        return factory.newDocumentBuilder().parse(xml.byteInputStream())
    }

    companion object {
        private const val BPMN_NS = "http://www.omg.org/spec/BPMN/20100524/MODEL"
        private const val BPMNDI_NS = "http://www.omg.org/spec/BPMN/20100524/DI"
        private const val DC_NS = "http://www.omg.org/spec/DD/20100524/DC"
        private const val DI_NS = "http://www.omg.org/spec/DD/20100524/DI"
    }
}
