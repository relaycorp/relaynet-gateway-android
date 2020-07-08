package tech.relaycorp.gateway.common

sealed class Operation<out T> {
    data class Success<T>(val result: T) : Operation<T>()
    data class Error<T>(val throwable: Throwable) : Operation<T>()
}

val <T> Operation<T>.isSuccessful get() = this is Operation.Success<T>
val <T> Operation<T>.isError get() = this is Operation.Error<T>
