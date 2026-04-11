package com.xwal.adapter.engine

import com.xwal.adapter.engine.transformer.BpmnToIwmTransformer
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import kotlin.test.*

class BpmnToIwmTransformerTest {

    private val transformer = BpmnToIwmTransformer()
    private val objectMapper = ObjectMapper()

    private val simpleBpmn = """<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="test_process" name="Test Process" isExecutable="true">
    <bpmn:startEvent id="start" name="Start"/>
    <bpmn:userTask id="task1" name="Approve"/>
    <bpmn:endEvent id="end" name="End"/>
    <bpmn:sequenceFlow id="flow1" sourceRef="start" targetRef="task1"/>
    <bpmn:sequenceFlow id="flow2" sourceRef="task1" targetRef="end"/>
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="test_process">
      <bpmndi:BPMNShape id="start_di" bpmnElement="start">
        <dc:Bounds x="100" y="100" width="36" height="36"/>
      </bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>"""

    @Test
    fun `transforms BPMN to IWM JSON`() {
        val iwm = transformer.transformToIwm(simpleBpmn, "camunda7")
        val json = objectMapper.readTree(iwm)
        assertEquals("0.2", json.get("version").asText())
        assertEquals("workflow", json.get("type").asText())
        assertTrue(json.has("tasks"))
        assertTrue(json.has("edges"))
        assertTrue(json.get("tasks").isArray)
    }

    @Test
    fun `extracts tasks from BPMN`() {
        val iwm = transformer.transformToIwm(simpleBpmn, "camunda7")
        val json = objectMapper.readTree(iwm)
        val tasks = json.get("tasks")
        assertTrue(tasks.size() >= 3) // start, userTask, end
    }

    @Test
    fun `extracts edges from BPMN`() {
        val iwm = transformer.transformToIwm(simpleBpmn, "camunda7")
        val json = objectMapper.readTree(iwm)
        val edges = json.get("edges")
        assertEquals(2, edges.size())
        assertEquals("start", edges.get(0).get("from").asText())
        assertEquals("task1", edges.get(0).get("to").asText())
    }

    @Test
    fun `extracts layout from BPMN DI`() {
        val iwm = transformer.transformToIwm(simpleBpmn, "camunda7")
        val json = objectMapper.readTree(iwm)
        assertTrue(json.has("layouts"))
        val layouts = json.get("layouts")
        assertTrue(layouts.size() > 0)
        assertEquals("camunda7", layouts.get(0).get("engine").asText())
    }

    @Test
    fun `engine parameter is preserved in layout`() {
        val iwm = transformer.transformToIwm(simpleBpmn, "flowable")
        val json = objectMapper.readTree(iwm)
        assertEquals("flowable", json.get("layouts").get(0).get("engine").asText())
    }

    @Test
    fun `handles BPMN without DI`() {
        val bpmnNoDI = """<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" id="D1" targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="p1" name="P1" isExecutable="true">
    <bpmn:startEvent id="s" name="S"/>
    <bpmn:endEvent id="e" name="E"/>
    <bpmn:sequenceFlow id="f1" sourceRef="s" targetRef="e"/>
  </bpmn:process>
</bpmn:definitions>"""
        val iwm = transformer.transformToIwm(bpmnNoDI, "test")
        val json = objectMapper.readTree(iwm)
        assertTrue(json.has("tasks"))
        assertFalse(json.has("layouts") && json.get("layouts").size() > 0)
    }

    @Test
    fun `handles conditional sequence flow`() {
        val bpmnCond = """<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" id="D1" targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="p1" isExecutable="true">
    <bpmn:startEvent id="s"/>
    <bpmn:endEvent id="e"/>
    <bpmn:sequenceFlow id="f1" sourceRef="s" targetRef="e">
      <bpmn:conditionExpression>approved == true</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
  </bpmn:process>
</bpmn:definitions>"""
        val iwm = transformer.transformToIwm(bpmnCond, "test")
        val json = objectMapper.readTree(iwm)
        val edge = json.get("edges").get(0)
        assertEquals("approved == true", edge.get("condition").asText())
    }
}
