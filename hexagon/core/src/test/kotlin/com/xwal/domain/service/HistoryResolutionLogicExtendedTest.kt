package com.xwal.domain.service

import com.xwal.domain.model.InstanceStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HistoryResolutionLogicExtendedTest {

    @Test fun `null → COMPLETED`() { assertEquals(InstanceStatus.COMPLETED, HistoryResolutionLogic.resolveStatus(null)) }
    @Test fun `completed → COMPLETED`() { assertEquals(InstanceStatus.COMPLETED, HistoryResolutionLogic.resolveStatus("completed")) }
    @Test fun `COMPLETED uppercase → COMPLETED`() { assertEquals(InstanceStatus.COMPLETED, HistoryResolutionLogic.resolveStatus("COMPLETED")) }
    @Test fun `deleted → CANCELLED`() { assertEquals(InstanceStatus.CANCELLED, HistoryResolutionLogic.resolveStatus("deleted")) }
    @Test fun `externallyTerminated → CANCELLED`() { assertEquals(InstanceStatus.CANCELLED, HistoryResolutionLogic.resolveStatus("externallyTerminated")) }
    @Test fun `terminated → CANCELLED`() { assertEquals(InstanceStatus.CANCELLED, HistoryResolutionLogic.resolveStatus("terminated")) }
    @Test fun `error prefix → FAILED`() { assertEquals(InstanceStatus.FAILED, HistoryResolutionLogic.resolveStatus("error:NullPointer")) }
    @Test fun `error colon only → FAILED`() { assertEquals(InstanceStatus.FAILED, HistoryResolutionLogic.resolveStatus("error:")) }
    @Test fun `unknown → TERMINATED`() { assertEquals(InstanceStatus.TERMINATED, HistoryResolutionLogic.resolveStatus("someOther")) }
    @Test fun `empty string → TERMINATED`() { assertEquals(InstanceStatus.TERMINATED, HistoryResolutionLogic.resolveStatus("")) }
}
