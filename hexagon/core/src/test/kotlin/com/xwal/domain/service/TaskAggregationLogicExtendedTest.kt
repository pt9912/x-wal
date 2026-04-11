package com.xwal.domain.service

import com.xwal.domain.model.EngineType
import com.xwal.domain.model.Task
import com.xwal.domain.model.TaskId
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaskAggregationLogicExtendedTest {

    private fun task(id: String, minutesAgo: Long, engine: EngineType = EngineType.CAMUNDA7) = Task(
        TaskId.of(engine, id), "Task $id", "userTask", null, null, null, null, null, null,
        Instant.now().minusSeconds(minutesAgo * 60), null, null
    )

    @Test fun `sorts newest first`() {
        val tasks = listOf(task("old", 60), task("new", 1))
        val sorted = TaskAggregationLogic.sortAndPaginate(tasks, 0, 10)
        assertEquals("new", sorted[0].taskId.engineTaskId)
    }

    @Test fun `offset skips`() { assertEquals(1, TaskAggregationLogic.sortAndPaginate(listOf(task("1", 1), task("2", 2)), 1, 10).size) }
    @Test fun `limit caps`() { assertEquals(1, TaskAggregationLogic.sortAndPaginate(listOf(task("1", 1), task("2", 2)), 0, 1).size) }
    @Test fun `empty returns empty`() { assertTrue(TaskAggregationLogic.sortAndPaginate(emptyList(), 0, 10).isEmpty()) }
    @Test fun `offset beyond size returns empty`() { assertTrue(TaskAggregationLogic.sortAndPaginate(listOf(task("1", 1)), 5, 10).isEmpty()) }

    @Test fun `multi-engine aggregation`() {
        val tasks = listOf(task("c1", 10, EngineType.CAMUNDA7), task("f1", 5, EngineType.FLOWABLE), task("c2", 1, EngineType.CAMUNDA7))
        val sorted = TaskAggregationLogic.sortAndPaginate(tasks, 0, 10)
        assertEquals(3, sorted.size)
        assertEquals(EngineType.CAMUNDA7, sorted[0].taskId.engineType) // newest = c2
        assertEquals(EngineType.FLOWABLE, sorted[1].taskId.engineType) // f1
    }
}
