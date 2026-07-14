package com.pawan.nextpredict.data.remote.api

import com.pawan.nextpredict.data.remote.dto.GrokChatRequestDto
import com.pawan.nextpredict.data.remote.dto.GrokChatResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * xAI Grok REST API — OpenAI-compatible chat completions endpoint.
 * Base URL: https://api.x.ai
 * Auth is injected via [GrokApiKeyInterceptor].
 */
interface GrokApi {

    @POST("chat/completions")
    suspend fun chatCompletions(
        @Body request: GrokChatRequestDto,
    ): GrokChatResponseDto
}
