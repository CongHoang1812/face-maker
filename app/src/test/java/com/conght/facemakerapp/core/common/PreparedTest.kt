package com.conght.facemakerapp.core.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PreparedTest {

    @Test
    fun take_returnsPreparedResult_thenPreparesNext() = runTest {
        var calls = 0
        val prepared = Prepared(backgroundScope) { ++calls }
        prepared.prepare()
        runCurrent()
        assertEquals(1, calls)

        assertEquals(1, prepared.take())
        runCurrent()
        assertEquals(2, calls) // đã tính sẵn lần kế tiếp
        assertEquals(2, prepared.take())
    }

    @Test
    fun take_withoutPrepare_producesNow() = runTest {
        val prepared = Prepared(backgroundScope) { "fresh" }
        assertEquals("fresh", prepared.take())
    }

    @Test
    fun take_preparedFailed_retriesFresh() = runTest {
        var calls = 0
        // Như app: scope SupervisorJob → lần tính trước lỗi không huỷ cả scope
        val scope = CoroutineScope(backgroundScope.coroutineContext + SupervisorJob(backgroundScope.coroutineContext[Job]))
        val prepared = Prepared(scope) { if (++calls == 1) error("mạng lỗi") else calls }
        prepared.prepare()
        runCurrent()
        assertEquals(2, prepared.take())
    }
}
