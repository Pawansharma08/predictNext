package com.pawan.nextpredict.core.network

import com.pawan.nextpredict.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that appends the Grok (xAI) Bearer token to every request
 * routed through the Grok Retrofit instance.
 */
@Singleton
class GrokApiKeyInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val request = original.newBuilder()
            .header("Authorization", "Bearer ${BuildConfig.GROK_API_KEY}")
            .header("Content-Type", "application/json")
            .build()
        return chain.proceed(request)
    }
}
