package com.tachyon.transcripter.data.remote

/**
 * Sealed class representing the result of a network operation.
 * Provides a type-safe way to handle success, error, and loading states.
 */
sealed class NetworkResult<out T> {

    /**
     * Represents a successful network response with data.
     */
    data class Success<T>(val data: T) : NetworkResult<T>()

    /**
     * Represents a network error.
     */
    data class Error(
        val message: String,
        val code: Int? = null,
        val exception: Throwable? = null
    ) : NetworkResult<Nothing>()

    /**
     * Represents a loading state.
     */
    object Loading : NetworkResult<Nothing>()

    /**
     * Returns true if this is a Success result.
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Returns true if this is an Error result.
     */
    val isError: Boolean
        get() = this is Error

    /**
     * Returns true if this is a Loading result.
     */
    val isLoading: Boolean
        get() = this is Loading

    /**
     * Returns the data if Success, null otherwise.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * Returns the error message if Error, null otherwise.
     */
    fun errorOrNull(): String? = when (this) {
        is Error -> message
        else -> null
    }

    /**
     * Maps the success data to another type.
     */
    inline fun <R> map(transform: (T) -> R): NetworkResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(message, code, exception)
        is Loading -> Loading
    }

    /**
     * Executes action if this is a Success result.
     */
    inline fun onSuccess(action: (T) -> Unit): NetworkResult<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * Executes action if this is an Error result.
     */
    inline fun onError(action: (String, Int?, Throwable?) -> Unit): NetworkResult<T> {
        if (this is Error) action(message, code, exception)
        return this
    }

    /**
     * Executes action if this is a Loading result.
     */
    inline fun onLoading(action: () -> Unit): NetworkResult<T> {
        if (this is Loading) action()
        return this
    }
}

/**
 * Extension function to convert a Result to NetworkResult.
 */
fun <T> Result<T>.toNetworkResult(): NetworkResult<T> {
    return fold(
        onSuccess = { NetworkResult.Success(it) },
        onFailure = {
            NetworkResult.Error(
                message = it.message ?: "Unknown error",
                exception = it
            )
        }
    )
}