package com.conght.facemakerapp.core.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

/**
 * Giữ sẵn 1 kết quả [produce] tính trước ở nền (vd. Lucky roll / đề Cosplay cần tải + ghép ~40 layer).
 * [take] trả kết quả đã tính sẵn (hoặc đang tính dở thì chờ tiếp), rồi bắt đầu tính cái kế tiếp →
 * từ lần bấm thứ 2 gần như có ngay. Lần tính sẵn bị lỗi → [take] tính lại mới.
 */
class Prepared<T>(
    private val scope: CoroutineScope,
    private val produce: suspend () -> T,
) {
    private var next: Deferred<T>? = null

    /** Bắt đầu tính trước nếu chưa có (hoặc lần trước lỗi). */
    @Synchronized
    fun prepare() {
        val current = next
        if (current == null || current.isCompleted && current.getCompletionExceptionOrNull() != null) {
            next = scope.async { produce() }
        }
    }

    suspend fun take(): T {
        val current = synchronized(this) { next.also { next = null } } ?: scope.async { produce() }
        val result = try {
            current.await()
        } catch (e: CancellationException) {
            // Người gọi rời màn: giữ lại kết quả đang tính cho lần sau
            synchronized(this) { if (next == null) next = current }
            throw e
        } catch (e: Exception) {
            produce()
        }
        prepare()
        return result
    }
}
