package com.xwal.domain.service

import com.xwal.domain.model.InstanceStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HistoryResolutionLogicTest {

    @Test
    fun `null deleteReason maps to COMPLETED`() {
        assertEquals(InstanceStatus.COMPLETED, HistoryResolutionLogic.resolveStatus(null))
    }

    @Test
    fun `completed maps to COMPLETED`() {
        assertEquals(InstanceStatus.COMPLETED, HistoryResolutionLogic.resolveStatus("completed"))
    }

    @Test
    fun `deleted maps to CANCELLED`() {
        assertEquals(InstanceStatus.CANCELLED, HistoryResolutionLogic.resolveStatus("deleted"))
    }

    @Test
    fun `externallyTerminated maps to CANCELLED`() {
        assertEquals(InstanceStatus.CANCELLED, HistoryResolutionLogic.resolveStatus("externallyTerminated"))
    }

    @Test
    fun `error prefix maps to FAILED`() {
        assertEquals(InstanceStatus.FAILED, HistoryResolutionLogic.resolveStatus("error:NullPointerException"))
    }

    @Test
    fun `unknown reason maps to TERMINATED`() {
        assertEquals(InstanceStatus.TERMINATED, HistoryResolutionLogic.resolveStatus("someOtherReason"))
    }
}
