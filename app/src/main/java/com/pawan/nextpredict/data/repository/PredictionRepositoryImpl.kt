package com.pawan.nextpredict.data.repository

import com.pawan.nextpredict.core.common.ApiResult
import com.pawan.nextpredict.core.common.safeApiCall
import com.pawan.nextpredict.data.remote.api.GrokApi
import com.pawan.nextpredict.data.remote.dto.GrokChatRequestDto
import com.pawan.nextpredict.data.remote.dto.GrokMessageDto
import com.pawan.nextpredict.domain.model.HistoricalDataPoint
import com.pawan.nextpredict.domain.model.PricePrediction
import com.pawan.nextpredict.domain.model.PredictionDirection
import com.pawan.nextpredict.domain.repository.PredictionRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calls the Groq API (OpenAI-compatible) with a structured OHLCV prompt
 * and parses the JSON response into a [PricePrediction].
 * Model: llama-3.3-70b-versatile — fast, high quality, generous free tier.
 */
@Singleton
class PredictionRepositoryImpl @Inject constructor(
    private val grokApi: GrokApi,
) : PredictionRepository {

    override suspend fun predictNextPrice(
        symbol: String,
        currentPrice: Double,
        history: List<HistoricalDataPoint>,
        isIntraday: Boolean,
        fiveYearSummary: String,
    ): ApiResult<PricePrediction> = safeApiCall {

        val historyText = buildHistoryPrompt(history)
        val candleType  = if (isIntraday) "1-minute intraday candles" else "daily candles"
        val timeLabel   = if (isIntraday) "last 30 minutes" else "last 100 trading days"
        val hasFiveYear = fiveYearSummary.isNotBlank()

        val systemPrompt = """
            You are a quantitative stock analyst combining multi-timeframe technical analysis.
            You have access to both long-term (5-year) historical context AND recent $candleType.
            Use the long-term data to identify key support/resistance levels and trend bias,
            then use the recent candles for short-term momentum and entry signals.
            
            CRITICAL: Always respond with ONLY a valid JSON object. No markdown, no explanation outside JSON.
            
            Response format:
            {
              "direction": "UP" | "DOWN" | "SIDEWAYS",
              "target_low": <number>,
              "target_high": <number>,
              "confidence": <integer 0-100>,
              "reasoning": "<2-3 sentence technical reasoning referencing both long-term trend and short-term signals>"
            }
        """.trimIndent()

        val userPrompt = buildString {
            appendLine("Stock: $symbol")
            appendLine("Current Price: ₹${"%.2f".format(currentPrice)}")
            appendLine()
            if (hasFiveYear) {
                appendLine(fiveYearSummary)
                appendLine()
            }
            appendLine("=== RECENT OHLCV ($timeLabel, oldest → newest) ===")
            appendLine(historyText)
            appendLine()
            appendLine("Instructions:")
            appendLine("- Predict direction and price range for the NEXT 5 MINUTES of trading.")
            appendLine("- Keep target_low and target_high within a realistic ±2% band of current price.")
            appendLine("- Use 5-year high/low as major support/resistance reference points.")
            appendLine("- Factor in the long-term trend bias when determining direction confidence.")
            if (isIntraday) appendLine("- You have real 1-min candles: use recent volume and momentum signals.")
            appendLine()
            appendLine("Respond with only the JSON object.")
        }

        val request = GrokChatRequestDto(
            model = "llama-3.3-70b-versatile",
            messages = listOf(
                GrokMessageDto(role = "system", content = systemPrompt),
                GrokMessageDto(role = "user", content = userPrompt),
            ),
            temperature = 0.2,
            maxTokens   = 300,
        )

        val response = grokApi.chatCompletions(request)
        val rawContent = response.choices
            ?.firstOrNull()
            ?.message
            ?.content
            ?: throw IllegalStateException("Grok returned an empty response")

        parsePrediction(symbol, rawContent, currentPrice)
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun buildHistoryPrompt(points: List<HistoricalDataPoint>): String =
        points.joinToString("\n") { p ->
            "${p.date}: O=${p.open} H=${p.high} L=${p.low} C=${p.close} V=${p.volume}"
        }

    /**
     * Extracts the JSON block from Grok's text response and parses it into
     * a [PricePrediction]. Uses regex so it's resilient to extra whitespace
     * or wrapping markdown the model may occasionally produce.
     */
    private fun parsePrediction(
        symbol: String,
        raw: String,
        currentPrice: Double,
    ): PricePrediction {
        // Extract JSON block — handles both bare JSON and ```json ``` wrapping.
        // Using [{] and [}] instead of \{ \} to avoid PatternSyntaxException on
        // Android's ICU regex engine, which treats \{ as a {min,max} interval quantifier.
        val jsonRegex = Regex("""[{][^{}]*[}]""", RegexOption.DOT_MATCHES_ALL)
        val jsonStr = jsonRegex.find(raw)?.value ?: raw.trim()

        val direction = when {
            jsonStr.contains("\"UP\"", ignoreCase = true) -> PredictionDirection.UP
            jsonStr.contains("\"DOWN\"", ignoreCase = true) -> PredictionDirection.DOWN
            else -> PredictionDirection.SIDEWAYS
        }

        val targetLow = extractDouble(jsonStr, "target_low") ?: (currentPrice * 0.995)
        val targetHigh = extractDouble(jsonStr, "target_high") ?: (currentPrice * 1.005)
        val confidence = extractInt(jsonStr, "confidence") ?: 50
        val reasoning = extractString(jsonStr, "reasoning") ?: "AI analysis based on recent price action."

        val timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))

        return PricePrediction(
            symbol = symbol,
            direction = direction,
            targetLow = targetLow,
            targetHigh = targetHigh,
            confidence = confidence.coerceIn(0, 100),
            reasoning = reasoning,
            generatedAt = timestamp,
        )
    }

    private fun extractDouble(json: String, key: String): Double? {
        val regex = Regex(""""$key"\s*:\s*([0-9]*\.?[0-9]+)""")
        return regex.find(json)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
    }

    private fun extractInt(json: String, key: String): Int? {
        val regex = Regex(""""$key"\s*:\s*([0-9]+)""")
        return regex.find(json)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private fun extractString(json: String, key: String): String? {
        val regex = Regex(""""$key"\s*:\s*"([^"]+)"""")
        return regex.find(json)?.groupValues?.getOrNull(1)
    }
}
