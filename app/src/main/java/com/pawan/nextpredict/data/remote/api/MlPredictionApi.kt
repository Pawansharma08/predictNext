package com.pawan.nextpredict.data.remote.api

import com.pawan.nextpredict.data.remote.dto.MlPredictRequestDto
import com.pawan.nextpredict.data.remote.dto.MlSignalResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for the local LightGBM FastAPI server.
 * Base URL is configured in NetworkModule via the "ml" named Retrofit instance.
 *
 * Android emulator → host:   http://10.0.2.2:8000/
 * Physical device (LAN):     http://<PC-LAN-IP>:8000/
 */
interface MlPredictionApi {

    /**
     * Sends raw OHLCV candles (already fetched from Yahoo Finance) to the
     * server, which computes TA features internally and returns the gated
     * ML direction signal.
     */
    @POST("compute-and-predict")
    suspend fun computeAndPredict(
        @Body request: MlPredictRequestDto,
    ): MlSignalResponseDto
}
