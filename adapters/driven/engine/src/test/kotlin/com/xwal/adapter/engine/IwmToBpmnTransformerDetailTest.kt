package com.xwal.adapter.engine

import com.xwal.adapter.engine.camunda7.transformer.IwmToBpmnTransformer as Cam7Transformer
import com.xwal.adapter.engine.flowable.transformer.IwmToBpmnTransformer as FlowTransformer
import org.junit.jupiter.api.Test
import kotlin.test.*

class IwmToBpmnTransformerDetailTest {

    private fun iwm(tasks: String, edges: String, extras: String = "") = """
    {"workflow":{"name":"t","version":"1.0"},"tasks":[$tasks],"edges":[$edges]$extras}
    """.trimIndent()

    private val cam7 = Cam7Transformer()
    private val flow = FlowTransformer()

    // ========== Task Types ==========

    @Test fun `Camunda7 start event`() { val b = cam7.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"}""", ""), "p", "1"); assertContains(b, "<bpmn:startEvent") }
    @Test fun `Camunda7 end event`() { val b = cam7.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"},{"id":"e","type":"end","label":"E"}""", """{"from":"s","to":"e"}"""), "p", "1"); assertContains(b, "<bpmn:endEvent") }
    @Test fun `Camunda7 user task with assignee`() { val b = cam7.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"},{"id":"u","type":"userTask","label":"U","extensions":{"camunda7":{"assignee":"alice"}}},{"id":"e","type":"end","label":"E"}""", """{"from":"s","to":"u"},{"from":"u","to":"e"}"""), "p", "1"); assertContains(b, "camunda:assignee=\"alice\"") }
    @Test fun `Camunda7 user task with candidateGroups`() { val b = cam7.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"},{"id":"u","type":"userTask","label":"U","user":{"candidateGroups":"managers"}},{"id":"e","type":"end","label":"E"}""", """{"from":"s","to":"u"},{"from":"u","to":"e"}"""), "p", "1"); assertContains(b, "camunda:candidateGroups=\"managers\"") }
    @Test fun `Camunda7 user task with formKey`() { val b = cam7.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"},{"id":"u","type":"userTask","label":"U","user":{"formKey":"approval-form"}},{"id":"e","type":"end","label":"E"}""", """{"from":"s","to":"u"},{"from":"u","to":"e"}"""), "p", "1"); assertContains(b, "camunda:formKey=\"approval-form\"") }
    @Test fun `Camunda7 service task with class`() { val b = cam7.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"},{"id":"t","type":"serviceTask","label":"T","extensions":{"camunda7":{"class":"com.example.MyDelegate"}}},{"id":"e","type":"end","label":"E"}""", """{"from":"s","to":"t"},{"from":"t","to":"e"}"""), "p", "1"); assertContains(b, "camunda:class=\"com.example.MyDelegate\"") }
    @Test fun `Camunda7 service task with delegateExpression`() { val b = cam7.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"},{"id":"t","type":"serviceTask","label":"T","extensions":{"camunda7":{"delegateExpression":"myBean"}}},{"id":"e","type":"end","label":"E"}""", """{"from":"s","to":"t"},{"from":"t","to":"e"}"""), "p", "1"); assertContains(b, "camunda:delegateExpression=\"myBean\"") }
    @Test fun `Camunda7 script task`() { val b = cam7.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"},{"id":"t","type":"script","label":"T","extensions":{"camunda7":{"scriptFormat":"javascript","script":"print('hi')"}}},{"id":"e","type":"end","label":"E"}""", """{"from":"s","to":"t"},{"from":"t","to":"e"}"""), "p", "1"); assertContains(b, "scriptFormat=\"javascript\""); assertContains(b, "print('hi')") }
    @Test fun `Camunda7 exclusive gateway`() { val b = cam7.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"},{"id":"g","type":"decision","label":"G"},{"id":"e","type":"end","label":"E"}""", """{"from":"s","to":"g"},{"from":"g","to":"e"}"""), "p", "1"); assertContains(b, "<bpmn:exclusiveGateway") }
    @Test fun `Camunda7 parallel gateway`() { val b = cam7.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"},{"id":"g","type":"parallel","label":"G"},{"id":"e","type":"end","label":"E"}""", """{"from":"s","to":"g"},{"from":"g","to":"e"}"""), "p", "1"); assertContains(b, "<bpmn:parallelGateway") }
    @Test fun `Camunda7 activity startProcess`() { val b = cam7.transformToBpmn(iwm("""{"id":"s","type":"activity","operation":"startProcess","label":"S"}""", ""), "p", "1"); assertContains(b, "<bpmn:startEvent") }
    @Test fun `Camunda7 activity endProcess`() { val b = cam7.transformToBpmn(iwm("""{"id":"s","type":"activity","operation":"startProcess","label":"S"},{"id":"e","type":"activity","operation":"endProcess","label":"E"}""", """{"from":"s","to":"e"}"""), "p", "1"); assertContains(b, "<bpmn:endEvent") }
    @Test fun `Camunda7 event message kind`() { val b = cam7.transformToBpmn(iwm("""{"id":"s","type":"event","kind":"message","label":"S"}""", ""), "p", "1"); assertContains(b, "<bpmn:startEvent") }
    @Test fun `Camunda7 event signal kind`() { val b = cam7.transformToBpmn(iwm("""{"id":"s","type":"event","kind":"start","label":"S"},{"id":"e","type":"event","kind":"signal","label":"E"}""", """{"from":"s","to":"e"}"""), "p", "1"); assertContains(b, "<bpmn:endEvent") }
    @Test fun `Camunda7 generic task`() { val b = cam7.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"},{"id":"t","type":"unknownType","label":"T"},{"id":"e","type":"end","label":"E"}""", """{"from":"s","to":"t"},{"from":"t","to":"e"}"""), "p", "1"); assertContains(b, "<bpmn:task") }

    // ========== Flowable specifics ==========

    @Test fun `Flowable user task uses flowable namespace`() { val b = flow.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"},{"id":"u","type":"userTask","label":"U","extensions":{"flowable":{"assignee":"bob"}}},{"id":"e","type":"end","label":"E"}""", """{"from":"s","to":"u"},{"from":"u","to":"e"}"""), "p", "1"); assertContains(b, "flowable:assignee=\"bob\""); assertFalse(b.contains("camunda:")) }
    @Test fun `Flowable service task`() { val b = flow.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"},{"id":"t","type":"serviceTask","label":"T","extensions":{"flowable":{"class":"com.example.Svc"}}},{"id":"e","type":"end","label":"E"}""", """{"from":"s","to":"t"},{"from":"t","to":"e"}"""), "p", "1"); assertContains(b, "flowable:class=\"com.example.Svc\"") }

    // ========== Layout ==========

    @Test fun `Camunda7 with layout generates BPMN DI`() {
        val iwmWithLayout = iwm(
            """{"id":"s","type":"start","label":"S"},{"id":"e","type":"end","label":"E"}""",
            """{"from":"s","to":"e"}""",
            ""","layouts":[{"engine":"camunda7","nodes":[{"elementId":"s","bounds":{"x":100,"y":200,"width":36,"height":36}},{"elementId":"e","bounds":{"x":300,"y":200,"width":36,"height":36}}],"edges":[{"from":"s","to":"e","waypoints":[{"x":136,"y":218},{"x":300,"y":218}]}]}]"""
        )
        val b = cam7.transformToBpmn(iwmWithLayout, "p", "1")
        assertContains(b, "<bpmndi:BPMNDiagram")
        assertContains(b, "<bpmndi:BPMNShape")
        assertContains(b, "x=\"100\"")
        assertContains(b, "<bpmndi:BPMNEdge")
        assertContains(b, "<di:waypoint")
    }

    // ========== XML correctness ==========

    @Test fun `output is valid XML`() {
        val b = cam7.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"},{"id":"e","type":"end","label":"E"}""", """{"from":"s","to":"e"}"""), "p", "1")
        assertContains(b, "<?xml version=\"1.0\"")
        assertContains(b, "</bpmn:definitions>")
        assertContains(b, "xmlns:bpmn=")
        assertContains(b, "xmlns:xsi=")
    }

    @Test fun `XML escapes special characters`() {
        val b = cam7.transformToBpmn(iwm("""{"id":"s","type":"start","label":"Start & Go <now>"}""", ""), "p", "1")
        assertContains(b, "&amp;")
        assertContains(b, "&lt;")
        assertContains(b, "&gt;")
    }

    @Test fun `sanitizes invalid ID characters`() {
        val b = cam7.transformToBpmn(iwm("""{"id":"my task!@#","type":"start","label":"S"}""", ""), "p", "1")
        assertContains(b, "id=\"my_task___\"")
    }

    // ========== Error handling ==========

    @Test fun `rejects IWM without tasks`() {
        assertFailsWith<IllegalArgumentException> { cam7.transformToBpmn("""{"workflow":{},"edges":[]}""", "p", "1") }
    }

    @Test fun `rejects IWM without edges`() {
        assertFailsWith<IllegalArgumentException> { cam7.transformToBpmn("""{"workflow":{},"tasks":[]}""", "p", "1") }
    }

    @Test fun `rejects edge referencing unknown task`() {
        assertFailsWith<IllegalArgumentException> {
            cam7.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"}""", """{"from":"s","to":"nonexistent"}"""), "p", "1")
        }
    }
}
