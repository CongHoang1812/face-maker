package com.conght.facemakerapp.core.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

sealed interface Resource<out T> {
    data object Loading : Resource<Nothing>
    data class Success<T>(val data: T) : Resource<T>
    data class Error(val throwable: Throwable) : Resource<Nothing>
}

/** Emit [Resource.Loading], rồi kết quả của [block]; exception -> [Resource.Error]. */
fun <T> resourceFlow(block: suspend () -> T): Flow<Resource<T>> = flow<Resource<T>> {
    emit(Resource.Loading)
    emit(Resource.Success(block()))
}.catch { e ->
    if (e is CancellationException) throw e
    emit(Resource.Error(e))
}
