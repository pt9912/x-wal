package com.xwal.adapter.web.grpc

import com.xwal.adapter.web.grpc.proto.QueryTasksRequest
import com.xwal.adapter.web.grpc.proto.QueryTasksResponse
import com.xwal.domain.model.Task
import com.xwal.domain.model.TaskFilter
import com.xwal.domain.model.TaskId
import com.xwal.domain.model.TaskStatus
import com.xwal.domain.port.input.CompleteTaskUseCase
import com.xwal.domain.port.input.QueryTasksUseCase
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TaskServiceEndpointTest {

    private val completeTask = mockk<CompleteTaskUseCase>(relaxed = true)

    @Test
    fun `maps workflowId and defaults size for task query pagination`() {
        var capturedFilter: TaskFilter? = null
        val task = Task(
            taskId = TaskId("CAMUNDA7:task-1"),
            name = "Review",
            type = "user",
            description = null,
            assignee = "alice",
            owner = null,
            processInstanceId = "wf-1",
            processDefinitionKey = "pd-1",
            status = TaskStatus.CREATED,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            dueDate = null,
            followUpDate = null,
            priority = 50
        )
        val queryTasks = object : QueryTasksUseCase {
            override fun execute(taskFilter: TaskFilter): List<Task> {
                capturedFilter = taskFilter
                return listOf(task)
            }
        }
        val endpoint = TaskServiceEndpoint(queryTasks, completeTask)

        val observer = RecordingStreamObserver<QueryTasksResponse>()
        val request = QueryTasksRequest.newBuilder()
            .setWorkflowId("wf-1")
            .setAssignee("alice")
            .setPage(3)
            .setSize(0)
            .build()

        endpoint.queryTasks(request, observer)

        assertNull(observer.error)
        assertEquals(1, observer.values.size)
        assertNotNull(capturedFilter)
        assertEquals(50, capturedFilter!!.limit)
        assertEquals(100, capturedFilter!!.offset)
        assertEquals("wf-1", capturedFilter!!.processInstanceId)
        assertEquals("alice", capturedFilter!!.assignee)
    }

    @Test
    fun `returns invalid argument when task status is unsupported`() {
        val queryTasks = object : QueryTasksUseCase {
            override fun execute(taskFilter: TaskFilter): List<Task> = emptyList()
        }
        val endpoint = TaskServiceEndpoint(queryTasks, completeTask)
        val observer = RecordingStreamObserver<QueryTasksResponse>()

        val request = QueryTasksRequest.newBuilder()
            .setStatus("does-not-exist")
            .build()

        endpoint.queryTasks(request, observer)

        assertNotNull(observer.error)
        val err = observer.error as StatusRuntimeException
        assertEquals(Status.Code.INVALID_ARGUMENT, err.status.code)
        assertEquals(0, observer.values.size)
        assertEquals(false, observer.completed)
    }

    private class RecordingStreamObserver<T> : StreamObserver<T> {
        val values = mutableListOf<T>()
        var error: Throwable? = null
        var completed = false

        override fun onNext(value: T) {
            values.add(value)
        }

        override fun onError(t: Throwable) {
            error = t
        }

        override fun onCompleted() {
            completed = true
        }
    }
}
