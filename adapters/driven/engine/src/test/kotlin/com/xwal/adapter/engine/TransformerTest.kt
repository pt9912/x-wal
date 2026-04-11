package com.xwal.adapter.engine

import com.xwal.adapter.engine.camunda7.transformer.IwmToBpmnTransformer as Camunda7Transformer
import com.xwal.adapter.engine.flowable.transformer.IwmToBpmnTransformer as FlowableTransformer
import com.xwal.adapter.engine.transformer.BpmnToIwmTransformer
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransformerTest {

    private val iwm = """
    {
        "workflow": {"name": "test", "version": "1.0"},
        "tasks": [
            {"id": "start", "type": "activity", "operation": "startProcess", "label": "Start"},
            {"id": "approve", "type": "userTask", "label": "Approve", "user": {"assignee": "manager"}},
            {"id": "notify", "type": "serviceTask", "label": "Notify"},
            {"id": "end", "type": "activity", "operation": "endProcess", "label": "End"}
        ],
        "edges": [
            {"from": "start", "to": "approve"},
            {"from": "approve", "to": "notify"},
            {"from": "notify", "to": "end"}
        ]
    }
    """.trimIndent()

    @Test
    fun `Camunda7 transformer generates valid BPMN with user task`() {
        val bpmn = Camunda7Transformer().transformToBpmn(iwm, "test", "1.0")
        assertContains(bpmn, "<bpmn:startEvent")
        assertContains(bpmn, "<bpmn:userTask")
        assertContains(bpmn, "camunda:assignee=\"manager\"")
        assertContains(bpmn, "<bpmn:serviceTask")
        assertContains(bpmn, "<bpmn:endEvent")
        assertContains(bpmn, "camunda:versionTag")
        assertContains(bpmn, "xmlns:xsi")
    }

    @Test
    fun `Flowable transformer generates valid BPMN with user task`() {
        val bpmn = FlowableTransformer().transformToBpmn(iwm, "test", "1.0")
        assertContains(bpmn, "<bpmn:startEvent")
        assertContains(bpmn, "<bpmn:userTask")
        assertContains(bpmn, "flowable:assignee=\"manager\"")
        assertContains(bpmn, "<bpmn:serviceTask")
        assertContains(bpmn, "<bpmn:endEvent")
        assertContains(bpmn, "xmlns:xsi")
        assertFalse(bpmn.contains("camunda:"))
    }

    @Test
    fun `Camunda7 generates sequence flows`() {
        val bpmn = Camunda7Transformer().transformToBpmn(iwm, "test", "1.0")
        assertContains(bpmn, "Flow_start_approve")
        assertContains(bpmn, "Flow_approve_notify")
        assertContains(bpmn, "Flow_notify_end")
    }

    @Test
    fun `Flowable reads camunda7 extensions as fallback`() {
        val iwmWithCamunda = """
        {
            "workflow": {"name": "t", "version": "1.0"},
            "tasks": [{"id": "s", "type": "activity", "operation": "startProcess", "label": "S"},
                      {"id": "u", "type": "userTask", "label": "U", "extensions": {"camunda7": {"assignee": "bob"}}},
                      {"id": "e", "type": "activity", "operation": "endProcess", "label": "E"}],
            "edges": [{"from": "s", "to": "u"}, {"from": "u", "to": "e"}]
        }
        """.trimIndent()
        val bpmn = FlowableTransformer().transformToBpmn(iwmWithCamunda, "t", "1.0")
        assertContains(bpmn, "flowable:assignee=\"bob\"")
    }

    @Test
    fun `BpmnToIwm round-trip preserves structure`() {
        val bpmn = Camunda7Transformer().transformToBpmn(iwm, "test", "1.0")
        val iwmResult = BpmnToIwmTransformer().transformToIwm(bpmn, "camunda7")
        assertContains(iwmResult, "\"type\"")
        assertContains(iwmResult, "\"tasks\"")
        assertContains(iwmResult, "\"edges\"")
    }

    @Test
    fun `transformer handles gateway`() {
        val iwmGateway = """
        {
            "workflow": {"name": "gw", "version": "1.0"},
            "tasks": [
                {"id": "s", "type": "activity", "operation": "startProcess", "label": "S"},
                {"id": "gw", "type": "decision", "label": "Check"},
                {"id": "e", "type": "activity", "operation": "endProcess", "label": "E"}
            ],
            "edges": [{"from": "s", "to": "gw"}, {"from": "gw", "to": "e"}]
        }
        """.trimIndent()
        val bpmn = Camunda7Transformer().transformToBpmn(iwmGateway, "gw", "1.0")
        assertContains(bpmn, "<bpmn:exclusiveGateway")
    }

    @Test
    fun `transformer handles conditional edge`() {
        val iwmCond = """
        {
            "workflow": {"name": "c", "version": "1.0"},
            "tasks": [
                {"id": "s", "type": "activity", "operation": "startProcess", "label": "S"},
                {"id": "e", "type": "activity", "operation": "endProcess", "label": "E"}
            ],
            "edges": [{"from": "s", "to": "e", "condition": "${'$'}{approved}"}]
        }
        """.trimIndent()
        val bpmn = Camunda7Transformer().transformToBpmn(iwmCond, "c", "1.0")
        assertContains(bpmn, "conditionExpression")
    }
}
