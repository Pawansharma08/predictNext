package com.pawan.nextpredict.data.remote.dto

import com.pawan.nextpredict.domain.model.MlSignal
import com.pawan.nextpredict.domain.model.MlSignalDirection
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Request ──────────────────────────────────────────────────────────────────

@Serializable
data class OhlcvCandleDto(
    val date:   String,
    val open:   Double,
    val high:   Double,
    val low:    Double,
    val close:  Double,
    val volume: Long,
)

@Serializable
data class MlPredictRequestDto(
    val symbol:  String,
    val candles: List<OhlcvCandleDto>,
)

// ─── Response ─────────────────────────────────────────────────────────────────

@Serializable
data class MlSignalResponseDto(
    val symbol:         String,
    val signal:         String,          // "UP" | "NOT_UP" | "NO_RELIABLE_SIGNAL" | "INSUFFICIENT_DATA"
    val confidence:     Double? = null,
    @SerialName("vol_regime")
    val volRegime:      String,
    val volatility:     Double? = null,
    @SerialName("vol_threshold")
    val volThreshold:   Double,
    @SerialName("candles_used")
    val candlesUsed:    Int,
    val note:           String,
    val timestamp:      String,
) {
    fun toDomain(): MlSignal = MlSignal(
        symbol       = symbol,
        direction    = when (signal.uppercase()) {
            "UP"                 -> MlSignalDirection.UP
            "NOT_UP"             -> MlSignalDirection.NOT_UP
            "NO_RELIABLE_SIGNAL" -> MlSignalDirection.NO_RELIABLE_SIGNAL
            else                 -> MlSignalDirection.INSUFFICIENT_DATA
        },
        confidence   = confidence,
        volRegime    = volRegime,
        volatility   = volatility,
        volThreshold = volThreshold,
        candlesUsed  = candlesUsed,
        note         = note,
    )
}
