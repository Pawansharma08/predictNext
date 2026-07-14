package com.pawan.nextpredict.feature.home

import app.cash.turbine.test
import com.pawan.nextpredict.core.common.ApiResult
import com.pawan.nextpredict.core.common.AppException
import com.pawan.nextpredict.domain.model.Index
import com.pawan.nextpredict.domain.model.MarketStatus
import com.pawan.nextpredict.domain.model.Stock
import com.pawan.nextpredict.domain.usecase.market.*
import com.pawan.nextpredict.feature.home.presentation.HomeEvent
import com.pawan.nextpredict.feature.home.presentation.HomeViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getMarketStatusUseCase: GetMarketStatusUseCase
    private lateinit var getIndicesUseCase: GetIndicesUseCase
    private lateinit var getTopGainersUseCase: GetTopGainersUseCase
    private lateinit var getTopLosersUseCase: GetTopLosersUseCase
    private lateinit var getMostActiveUseCase: GetMostActiveUseCase

    private lateinit var viewModel: HomeViewModel

    private val mockStatus = MarketStatus(
        isOpen = true,
        statusMessage = "Market is Open",
        marketType = "Normal Market",
        tradeDate = "10-Jul-2026",
        indices = emptyList(),
    )

    private val mockIndices = listOf(
        Index("NIFTY 50", 25000.0, 150.0, 0.6, 24900.0, 25100.0, 24850.0, 24850.0, 30, 20, 0),
        Index("NIFTY BANK", 52000.0, -200.0, -0.38, 52100.0, 52300.0, 51900.0, 52200.0, 15, 18, 2),
    )

    private val mockStocks = listOf(
        Stock("RELIANCE", "Reliance Industries", 2500.0, 50.0, 2.0, 2450.0, 2510.0, 2440.0, 2450.0, 1000000L, 0.0, 3000.0, 2000.0),
        Stock("INFY", "Infosys Ltd", 1800.0, 25.0, 1.4, 1780.0, 1810.0, 1775.0, 1775.0, 500000L, 0.0, 2000.0, 1500.0),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getMarketStatusUseCase = mockk()
        getIndicesUseCase = mockk()
        getTopGainersUseCase = mockk()
        getTopLosersUseCase = mockk()
        getMostActiveUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = HomeViewModel(
        getMarketStatusUseCase = getMarketStatusUseCase,
        getIndicesUseCase = getIndicesUseCase,
        getTopGainersUseCase = getTopGainersUseCase,
        getTopLosersUseCase = getTopLosersUseCase,
        getMostActiveUseCase = getMostActiveUseCase,
    )

    @Test
    fun `initial state is loading`() = runTest {
        coEvery { getMarketStatusUseCase() } returns ApiResult.Success(mockStatus)
        coEvery { getIndicesUseCase() } returns ApiResult.Success(mockIndices)
        coEvery { getTopGainersUseCase(any()) } returns ApiResult.Success(mockStocks)
        coEvery { getTopLosersUseCase(any()) } returns ApiResult.Success(mockStocks)
        coEvery { getMostActiveUseCase() } returns ApiResult.Success(mockStocks)

        viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `successful data load shows indices and stocks`() = runTest {
        coEvery { getMarketStatusUseCase() } returns ApiResult.Success(mockStatus)
        coEvery { getIndicesUseCase() } returns ApiResult.Success(mockIndices)
        coEvery { getTopGainersUseCase(any()) } returns ApiResult.Success(mockStocks)
        coEvery { getTopLosersUseCase(any()) } returns ApiResult.Success(mockStocks)
        coEvery { getMostActiveUseCase() } returns ApiResult.Success(mockStocks)

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNotNull(state.marketStatus)
        assertTrue(state.indices.isNotEmpty())
        assertTrue(state.topGainers.isNotEmpty())
        assertEquals(true, state.marketStatus?.isOpen)
    }

    @Test
    fun `network error shows error state when no cached data`() = runTest {
        val networkError = AppException.NetworkException()
        coEvery { getMarketStatusUseCase() } returns ApiResult.Error(networkError)
        coEvery { getIndicesUseCase() } returns ApiResult.Error(networkError)
        coEvery { getTopGainersUseCase(any()) } returns ApiResult.Error(networkError)
        coEvery { getTopLosersUseCase(any()) } returns ApiResult.Error(networkError)
        coEvery { getMostActiveUseCase() } returns ApiResult.Error(networkError)

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.error is AppException.NetworkException)
    }

    @Test
    fun `refresh event triggers data reload`() = runTest {
        coEvery { getMarketStatusUseCase() } returns ApiResult.Success(mockStatus)
        coEvery { getIndicesUseCase() } returns ApiResult.Success(mockIndices)
        coEvery { getTopGainersUseCase(any()) } returns ApiResult.Success(mockStocks)
        coEvery { getTopLosersUseCase(any()) } returns ApiResult.Success(mockStocks)
        coEvery { getMostActiveUseCase() } returns ApiResult.Success(mockStocks)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.Refresh)
        assertTrue(viewModel.uiState.value.isRefreshing)

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `StockClicked event emits NavigateToStock effect`() = runTest {
        coEvery { getMarketStatusUseCase() } returns ApiResult.Success(mockStatus)
        coEvery { getIndicesUseCase() } returns ApiResult.Success(mockIndices)
        coEvery { getTopGainersUseCase(any()) } returns ApiResult.Success(mockStocks)
        coEvery { getTopLosersUseCase(any()) } returns ApiResult.Success(mockStocks)
        coEvery { getMostActiveUseCase() } returns ApiResult.Success(mockStocks)

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onEvent(HomeEvent.StockClicked("RELIANCE"))
            val effect = awaitItem()
            assertTrue(effect is com.pawan.nextpredict.feature.home.presentation.HomeEffect.NavigateToStock)
            assertEquals("RELIANCE", (effect as com.pawan.nextpredict.feature.home.presentation.HomeEffect.NavigateToStock).symbol)
        }
    }
}
