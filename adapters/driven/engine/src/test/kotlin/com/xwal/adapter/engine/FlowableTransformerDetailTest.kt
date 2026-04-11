package com.xwal.adapter.engine

import com.xwal.adapter.engine.flowable.transformer.IwmToBpmnTransformer
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class FlowableTransformerDetailTest {

    private val transformer = IwmToBpmnTransformer()
    private fun iwm(tasks: String, edges: String, extras: String = "") = """{"workflow":{"name":"t","version":"1.0"},"tasks":[$tasks],"edges":[$edges]$extras}"""

    @Test fun `start event`() { assertContains(transformer.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"}""", ""), "p", "1"), "<bpmn:startEvent") }
    @Test fun `end event`() { assertContains(transformer.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"},{"id":"e","type":"end","label":"E"}""", """{"from":"s","to":"e"}"""), "p", "1"), "<bpmn:endEvent") }
    @Test fun `user task with flowable namespace`() { val b = transformer.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"},{"id":"u","type":"userTask","label":"U","extensions":{"flowable":{"assignee":"a","candidateGroups":"g","formKey":"f"}}},{"id":"e","type":"end","label":"E"}""", """{"from":"s","to":"u"},{"from":"u","to":"e"}"""), "p", "1"); assertContains(b, "flowable:assignee"); assertContains(b, "flowable:candidateGroups"); assertContains(b, "flowable:formKey") }
    @Test fun `service task with class`() { assertContains(transformer.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"},{"id":"t","type":"serviceTask","label":"T","extensions":{"flowable":{"class":"com.Svc"}}},{"id":"e","type":"end","label":"E"}""", """{"from":"s","to":"t"},{"from":"t","to":"e"}"""), "p", "1"), "flowable:class") }
    @Test fun `service task with delegateExpression`() { assertContains(transformer.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"},{"id":"t","type":"serviceTask","label":"T","extensions":{"flowable":{"delegateExpression":"bean"}}},{"id":"e","type":"end","label":"E"}""", """{"from":"s","to":"t"},{"from":"t","to":"e"}"""), "p", "1"), "flowable:delegateExpression") }
    @Test fun `script task`() { val b = transformer.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"},{"id":"t","type":"script_task","label":"T","extensions":{"flowable":{"scriptFormat":"js","script":"1+1"}}},{"id":"e","type":"end","label":"E"}""", """{"from":"s","to":"t"},{"from":"t","to":"e"}"""), "p", "1"); assertContains(b, "scriptFormat=\"js\""); assertContains(b, "1+1") }
    @Test fun `exclusive gateway`() { assertContains(transformer.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"},{"id":"g","type":"gateway","label":"G"},{"id":"e","type":"end","label":"E"}""", """{"from":"s","to":"g"},{"from":"g","to":"e"}"""), "p", "1"), "<bpmn:exclusiveGateway") }
    @Test fun `parallel gateway`() { assertContains(transformer.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"},{"id":"g","type":"parallel_gateway","label":"G"},{"id":"e","type":"end","label":"E"}""", """{"from":"s","to":"g"},{"from":"g","to":"e"}"""), "p", "1"), "<bpmn:parallelGateway") }
    @Test fun `activity startProcess`() { assertContains(transformer.transformToBpmn(iwm("""{"id":"s","type":"activity","operation":"startProcess","label":"S"}""", ""), "p", "1"), "<bpmn:startEvent") }
    @Test fun `activity endProcess`() { assertContains(transformer.transformToBpmn(iwm("""{"id":"s","type":"activity","operation":"start_process","label":"S"},{"id":"e","type":"activity","operation":"end_process","label":"E"}""", """{"from":"s","to":"e"}"""), "p", "1"), "<bpmn:endEvent") }
    @Test fun `event message kind`() { assertContains(transformer.transformToBpmn(iwm("""{"id":"s","type":"event","kind":"message","label":"S"}""", ""), "p", "1"), "<bpmn:startEvent") }
    @Test fun `event end kind`() { assertContains(transformer.transformToBpmn(iwm("""{"id":"s","type":"event","kind":"start","label":"S"},{"id":"e","type":"event","kind":"end","label":"E"}""", """{"from":"s","to":"e"}"""), "p", "1"), "<bpmn:endEvent") }
    @Test fun `generic task for unknown type`() { assertContains(transformer.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"},{"id":"t","type":"weird","label":"T"},{"id":"e","type":"end","label":"E"}""", """{"from":"s","to":"t"},{"from":"t","to":"e"}"""), "p", "1"), "<bpmn:task") }
    @Test fun `conditional edge`() { assertContains(transformer.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"},{"id":"e","type":"end","label":"E"}""", """{"from":"s","to":"e","condition":"x>0"}"""), "p", "1"), "conditionExpression") }
    @Test fun `no camunda namespace in output`() { assertFalse(transformer.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"}""", ""), "p", "1").contains("camunda:")) }
    @Test fun `has flowable namespace`() { assertContains(transformer.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"}""", ""), "p", "1"), "xmlns:flowable") }
    @Test fun `rejects missing tasks`() { assertFailsWith<IllegalArgumentException> { transformer.transformToBpmn("""{"workflow":{},"edges":[]}""", "p", "1") } }
    @Test fun `rejects missing edges`() { assertFailsWith<IllegalArgumentException> { transformer.transformToBpmn("""{"workflow":{},"tasks":[]}""", "p", "1") } }
    @Test fun `user task from user field`() { assertContains(transformer.transformToBpmn(iwm("""{"id":"s","type":"start","label":"S"},{"id":"u","type":"user_task","label":"U","user":{"assignee":"bob"}},{"id":"e","type":"end","label":"E"}""", """{"from":"s","to":"u"},{"from":"u","to":"e"}"""), "p", "1"), "flowable:assignee=\"bob\"") }
}
