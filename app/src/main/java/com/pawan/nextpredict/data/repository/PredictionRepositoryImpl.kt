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
        companyName: String,
        currentPrice: Double,
        change: Double,
        changePercent: Double,
        intradayHistory: List<HistoricalDataPoint>,
        dailyHistory: List<HistoricalDataPoint>,
        isIntraday: Boolean,
        fiveYearSummary: String,
    ): ApiResult<PricePrediction> = safeApiCall {

        val dailyHistoryText = buildHistoryPrompt(dailyHistory)
        val intradayHistoryText = if (isIntraday) buildHistoryPrompt(intradayHistory) else "No intraday data available."
        val hasFiveYear = fiveYearSummary.isNotBlank()

        val systemPrompt = """
            You are a quantitative stock analyst combining multi-timeframe technical analysis.
            You have access to both long-term (100-day) daily historical context AND today's 1-minute intraday candles.
            Use the 100-day daily context to identify major support/resistance levels and trend bias,
            then use today's 1-minute candles for short-term momentum, opening range breakouts, and volume analysis.
            
            CRITICAL: Always respond with ONLY a valid JSON object. No markdown, no explanation outside JSON.
            
            Response format:
            {
              "direction": "UP" | "DOWN" | "SIDEWAYS",
              "target_price": <number>,
              "target_low": <number>,
              "target_high": <number>,
              "confidence": <integer 0-100>,
              "reasoning": "<2-3 sentence technical reasoning referencing both long-term trend and short-term signals>"
            }
        """.trimIndent()

        val predictionTimeframe = if (isIntraday) "NEXT 5 MINUTES" else "NEXT TRADING DAY"
        val userPrompt = buildString {
            appendLine("Stock: $symbol ($companyName)")
            appendLine("Current Price: ₹${"%.2f".format(currentPrice)}")
            appendLine("Today's Change: ₹${"%.2f".format(change)} (${"%.2f".format(changePercent)}%)")
            appendLine()
            if (hasFiveYear) {
                appendLine(fiveYearSummary)
                appendLine()
            }
            if (isIntraday) {
                appendLine("=== TODAY'S INTRADAY OHLCV (1-minute intervals, oldest → newest) ===")
                appendLine(intradayHistoryText)
                appendLine()
            }
            appendLine("=== 100-DAY DAILY OHLCV HISTORICAL CONTEXT (oldest → newest) ===")
            appendLine(dailyHistoryText)
            appendLine()
            appendLine("Instructions:")
            appendLine("- Predict direction, specific target_price, and price range (target_low and target_high) for the $predictionTimeframe of trading.")
            if (isIntraday) {
                appendLine("- Keep target_low and target_high within a realistic ±0.5% band of current price (short-term 5-minute moves are narrow).")
            } else {
                appendLine("- Keep target_low and target_high within a realistic ±2% band of current price.")
            }
            appendLine("- If the recent candles show a stable, flat, or sideways consolidation with low volume, predict SIDEWAYS and keep target_low and target_high extremely close to the current price (within ±0.1% to ±0.2%).")
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

        parsePrediction(symbol, rawContent, currentPrice, isIntraday)
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
        isIntraday: Boolean,
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

        val targetPrice = extractDouble(jsonStr, "target_price") ?: currentPrice
        val targetLow = extractDouble(jsonStr, "target_low") ?: (currentPrice * 0.995)
        val targetHigh = extractDouble(jsonStr, "target_high") ?: (currentPrice * 1.005)
        val confidence = extractInt(jsonStr, "confidence") ?: 50
        val reasoning = extractString(jsonStr, "reasoning") ?: "AI analysis based on recent price action."

        val now = LocalDateTime.now()
        val timestamp = now.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
        val targetTime = if (isIntraday) {
            now.plusMinutes(5).format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
        } else {
            "Next Trading Session"
        }

        return PricePrediction(
            symbol = symbol,
            direction = direction,
            targetPrice = targetPrice,
            targetLow = targetLow,
            targetHigh = targetHigh,
            confidence = confidence.coerceIn(0, 100),
            reasoning = reasoning,
            generatedAt = timestamp,
            targetTime = targetTime,
        )
    }

    private fun extractDouble(json: String, key: String): Double? {
        val keyIndex = json.indexOf(key)
        if (keyIndex == -1) return null

        val colonIndex = json.indexOf(':', startIndex = keyIndex + key.length)
        if (colonIndex == -1) return null

        val startOfValue = colonIndex + 1
        var endOfValue = json.indexOf(',', startIndex = startOfValue)
        if (endOfValue == -1) {
            endOfValue = json.indexOf('}', startIndex = startOfValue)
        }
        if (endOfValue == -1) {
            endOfValue = json.length
        }

        val rawValue = json.substring(startOfValue, endOfValue).trim()
        val cleanValue = rawValue
            .replace("\"", "")
            .replace("'", "")
            .replace("₹", "")
            .replace("$", "")
            .replace(",", "")
            .trim()

        return cleanValue.toDoubleOrNull()
    }

    private fun extractInt(json: String, key: String): Int? {
        val keyIndex = json.indexOf(key)
        if (keyIndex == -1) return null

        val colonIndex = json.indexOf(':', startIndex = keyIndex + key.length)
        if (colonIndex == -1) return null

        val startOfValue = colonIndex + 1
        var endOfValue = json.indexOf(',', startIndex = startOfValue)
        if (endOfValue == -1) {
            endOfValue = json.indexOf('}', startIndex = startOfValue)
        }
        if (endOfValue == -1) {
            endOfValue = json.length
        }

        val rawValue = json.substring(startOfValue, endOfValue).trim()
        val cleanValue = rawValue
            .replace("\"", "")
            .replace("'", "")
            .replace(",", "")
            .trim()

        return cleanValue.toIntOrNull() ?: cleanValue.toDoubleOrNull()?.toInt()
    }

    private fun extractString(json: String, key: String): String? {
        val keyIndex = json.indexOf(key)
        if (keyIndex == -1) return null

        val colonIndex = json.indexOf(':', startIndex = keyIndex + key.length)
        if (colonIndex == -1) return null

        val startOfValue = colonIndex + 1
        var endOfValue = json.indexOf(',', startIndex = startOfValue)

        val firstQuoteIndex = json.indexOf('"', startIndex = startOfValue)
        if (firstQuoteIndex != -1 && firstQuoteIndex < (endOfValue.takeIf { it != -1 } ?: json.length)) {
            val secondQuoteIndex = json.indexOf('"', startIndex = firstQuoteIndex + 1)
            if (secondQuoteIndex != -1) {
                return json.substring(firstQuoteIndex + 1, secondQuoteIndex).trim()
            }
        }

        if (endOfValue == -1) {
            endOfValue = json.indexOf('}', startIndex = startOfValue)
        }
        if (endOfValue == -1) {
            endOfValue = json.length
        }

        return json.substring(startOfValue, endOfValue)
            .trim()
            .replace("\"", "")
            .replace("'", "")
            .trim()
    }
}
