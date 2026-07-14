package com.pawan.nextpredict.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Grok Chat Request ────────────────────────────────────────────────────────

@Serializable
data class GrokChatRequestDto(
    @SerialName("model")
    val model: String = "grok-3-mini",

    @SerialName("messages")
    val messages: List<GrokMessageDto>,

    @SerialName("temperature")
    val temperature: Double = 0.3,

    @SerialName("max_tokens")
    val maxTokens: Int = 512,
)

@Serializable
data class GrokMessageDto(
    @SerialName("role")
    val role: String,     // "system" | "user"

    @SerialName("content")
    val content: String,
)

// ─── Grok Chat Response ───────────────────────────────────────────────────────

@Serializable
data class GrokChatResponseDto(
    @SerialName("id")
    val id: String? = null,

    @SerialName("choices")
    val choices: List<GrokChoiceDto>? = null,

    @SerialName("usage")
    val usage: GrokUsageDto? = null,
)

@Serializable
data class GrokChoiceDto(
    @SerialName("index")
    val index: Int? = null,

    @SerialName("message")
    val message: GrokMessageDto? = null,

    @SerialName("finish_reason")
    val finishReason: String? = null,
)

@Serializable
data class GrokUsageDto(
    @SerialName("prompt_tokens")
    val promptTokens: Int? = null,

    @SerialName("completion_tokens")
    val completionTokens: Int? = null,

    @SerialName("total_tokens")
    val totalTokens: Int? = null,
)
