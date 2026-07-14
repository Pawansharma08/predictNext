package com.pawan.nextpredict.core.common

import com.pawan.nextpredict.core.common.AppException.*
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Safely executes a network call and maps exceptions to [AppException].
 *
 * Usage:
 * ```kotlin
 * val result = safeApiCall { api.getMarketStatus() }
 * ```
 */
suspend fun <T> safeApiCall(apiCall: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(apiCall())
    } catch (e: UnknownHostException) {
        Timber.w(e, "No internet connection")
        ApiResult.Error(NetworkException(cause = e))
    } catch (e: SocketTimeoutException) {
        Timber.w(e, "Request timed out")
        ApiResult.Error(TimeoutException(cause = e))
    } catch (e: SSLException) {
        Timber.w(e, "SSL error")
        ApiResult.Error(SslException(cause = e))
    } catch (e: IOException) {
        Timber.w(e, "IO error")
        ApiResult.Error(NetworkException(cause = e))
    } catch (e: HttpException) {
        Timber.w(e, "HTTP error: ${e.code()}")
        when (e.code()) {
            401, 403 -> ApiResult.Error(UnauthorizedException(cause = e))
            404 -> ApiResult.Error(NotFoundException(cause = e))
            in 500..599 -> ApiResult.Error(ServerException(code = e.code(), cause = e))
            else -> ApiResult.Error(ServerException(code = e.code(), cause = e))
        }
    } catch (e: SerializationException) {
        Timber.e(e, "Parse error")
        ApiResult.Error(ParseException(cause = e))
    } catch (e: Exception) {
        Timber.e(e, "Unknown error")
        ApiResult.Error(UnknownException(message = e.message ?: "An unexpected error occurred.", cause = e))
    }
}

