package com.pawan.nextpredict.core.common

/**
 * A sealed class that encapsulates the result of an operation.
 * Used across all layers to represent Success, Error, or Loading states.
 */
sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val exception: AppException) : ApiResult<Nothing>()
    data object Loading : ApiResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = if (this is Success) data else null

    fun <R> map(transform: (T) -> R): ApiResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> Loading
    }
}

/**
 * Application-level exception hierarchy.
 */
sealed class AppException(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause) {

    /** Network-related exceptions */
    data class NetworkException(
        override val message: String = "No internet connection. Please check your network.",
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    data class TimeoutException(
        override val message: String = "Request timed out. Please try again.",
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    data class SslException(
        override val message: String = "Secure connection failed.",
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    /** Server-side exceptions */
    data class ServerException(
        val code: Int,
        override val message: String = "Server error occurred ($code).",
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    data class NotFoundException(
        override val message: String = "The requested resource was not found.",
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    data class UnauthorizedException(
        override val message: String = "Access denied. Please try again.",
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    /** Parsing exceptions */
    data class ParseException(
        override val message: String = "Failed to parse server response.",
        override val cause: Throwable? = null,
    ) : AppException(message, cause)

    /** Unknown exceptions */
    data class UnknownException(
        override val message: String = "An unexpected error occurred.",
        override val cause: Throwable? = null,
    ) : AppException(message, cause)
}
