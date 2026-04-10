package com.xwal.domain.service

import com.xwal.domain.model.EngineType
import com.xwal.domain.model.Task
import com.xwal.domain.model.TaskId
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals

class TaskAggregationLogicTest {

    private fun task(id: String, minutesAgo: Long) = Task(
        taskId = TaskId.of(EngineType.CAMUNDA7, id),
        name = "Task $id",
        type = "userTask",
        description = null,
        assignee = null,
        owner = null,
        processInstanceId = null,
        processDefinitionKey = null,
        status = null,
        createdAt = Instant.now().minusSeconds(minutesAgo * 60),
        dueDate = null,
        followUpDate = null
    )

    @Test
    fun `sorts by creation date newest first`() {
        val tasks = listOf(task("old", 60), task("new", 1), task("mid", 30))
        val sorted = TaskAggregationLogic.sortAndPaginate(tasks, 0, 10)
        assertEquals("new", sorted[0].taskId.engineTaskId)
        assertEquals("mid", sorted[1].taskId.engineTaskId)
        assertEquals("old", sorted[2].taskId.engineTaskId)
    }

    @Test
    fun `applies offset`() {
        val tasks = listOf(task("1", 1), task("2", 2), task("3", 3))
        val result = TaskAggregationLogic.sortAndPaginate(tasks, 1, 10)
        assertEquals(2, result.size)
    }

    @Test
    fun `applies limit`() {
        val tasks = listOf(task("1", 1), task("2", 2), task("3", 3))
        val result = TaskAggregationLogic.sortAndPaginate(tasks, 0, 2)
        assertEquals(2, result.size)
    }

    @Test
    fun `empty list returns empty`() {
        assertEquals(emptyList(), TaskAggregationLogic.sortAndPaginate(emptyList(), 0, 10))
    }
}
