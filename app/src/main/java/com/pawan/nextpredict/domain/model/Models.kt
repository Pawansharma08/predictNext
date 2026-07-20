package com.pawan.nextpredict.domain.model

/**
 * Domain model for market status.
 * Pure Kotlin — no Android or framework dependencies.
 */
data class MarketStatus(
    val isOpen: Boolean,
    val statusMessage: String,         // "Market is Open", "Market is Closed", etc.
    val marketType: String,            // "Normal Market", "Pre-open", etc.
    val tradeDate: String,
    val indices: List<IndexStatus>,
)

data class IndexStatus(
    val indexName: String,
    val isOpen: Boolean,
    val status: String,
)

/**
 * Domain model for a market index (Nifty 50, Bank Nifty, Sensex).
 */
data class Index(
    val name: String,
    val lastPrice: Double,
    val change: Double,
    val changePercent: Double,
    val open: Double,
    val high: Double,
    val low: Double,
    val previousClose: Double,
    val advances: Int,
    val declines: Int,
    val unchanged: Int,
)

/**
 * Domain model for a stock in any list (gainers, losers, active, watchlist).
 */
data class Stock(
    val symbol: String,
    val companyName: String,
    val lastPrice: Double,
    val change: Double,
    val changePercent: Double,
    val open: Double,
    val high: Double,
    val low: Double,
    val previousClose: Double,
    val volume: Long,
    val totalTradedValue: Double,
    val yearHigh: Double,
    val yearLow: Double,
    val series: String = "EQ",
)

/**
 * Detailed stock quote with all data points.
 */
data class StockQuote(
    val symbol: String,
    val companyName: String,
    val series: String,
    val lastPrice: Double,
    val change: Double,
    val changePercent: Double,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val previousClose: Double,
    val vwap: Double,
    val lowerCircuit: Double,
    val upperCircuit: Double,
    val yearHigh: Double,
    val yearLow: Double,
    val volume: Long,
    val totalTradedValue: Double,
    val deliveryQuantity: Long,
    val deliveryPercent: Double,
    val totalBuyQty: Long,
    val totalSellQty: Long,
    val bidAsk: List<BidAsk>,
    val isin: String,
    val listingDate: String?,
    val faceValue: Double,
    val issuedSize: Long,
)

data class BidAsk(
    val quantity: Long,
    val price: Double,
    val orders: Int,
    val type: BidAskType, // BID or ASK
)

enum class BidAskType { BID, ASK }

/**
 * Domain model for option chain data.
 */
data class OptionChain(
    val symbol: String,
    val expiryDates: List<String>,
    val selectedExpiry: String,
    val underlyingValue: Double,
    val strikePrices: List<OptionStrike>,
    val atm: Double,              // At-the-money strike price
    val maxCallOI: Long,
    val maxPutOI: Long,
    val pcr: Double,             // Put-call ratio
    val timestamp: String,
)

data class OptionStrike(
    val strikePrice: Double,
    val callData: OptionData?,
    val putData: OptionData?,
)

data class OptionData(
    val strikePrice: Double,
    val expiryDate: String,
    val openInterest: Long,
    val changeInOI: Long,
    val totalTradedVolume: Long,
    val iv: Double,              // Implied volatility
    val ltp: Double,             // Last traded price
    val change: Double,
    val bidQty: Long,
    val bidPrice: Double,
    val askQty: Long,
    val askPrice: Double,
    val totalBuyQty: Long,
    val totalSellQty: Long,
)

/**
 * Domain model for search results.
 */
data class SearchResult(
    val symbol: String,
    val companyName: String,
    val isin: String?,
    val series: String?,
)

/**
 * Domain model for market news.
 */
data class NewsItem(
    val id: String,
    val title: String,
    val description: String,
    val url: String,
    val source: String,
    val publishedAt: Long,
    val imageUrl: String?,
    val relatedSymbols: List<String>,
)

/**
 * Domain model for historical OHLCV data (chart).
 */
data class HistoricalDataPoint(
    val date: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
)

/**
 * Domain model for a user's watchlist.
 */
data class Watchlist(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    val items: List<WatchlistStock> = emptyList(),
)

data class WatchlistStock(
    val symbol: String,
    val companyName: String,
    val sortOrder: Int,
    // Live price (loaded separately)
    val lastPrice: Double = 0.0,
    val change: Double = 0.0,
    val changePercent: Double = 0.0,
)

/**
 * Domain model for price alerts.
 */
data class PriceAlert(
    val id: Long,
    val symbol: String,
    val companyName: String,
    val targetPrice: Double,
    val condition: AlertCondition,
    val isActive: Boolean,
    val createdAt: Long,
    val triggeredAt: Long?,
)

enum class AlertCondition { ABOVE, BELOW }

/**
 * Domain model for pre-open market session.
 */
data class PreOpenStock(
    val symbol: String,
    val iep: Double,          // Indicative equilibrium price
    val change: Double,
    val changePercent: Double,
    val finalQuantity: Long,
    val totalBuyQty: Long,
    val totalSellQty: Long,
)

/**
 * Direction of an AI price prediction.
 */
enum class PredictionDirection { UP, DOWN, SIDEWAYS }

/**
 * Domain model for a Grok AI-powered short-term price prediction.
 */
data class PricePrediction(
    val symbol: String,
    val direction: PredictionDirection,
    /** Specific predicted price target. */
    val targetPrice: Double,
    /** Lower bound of the predicted price range. */
    val targetLow: Double,
    /** Upper bound of the predicted price range. */
    val targetHigh: Double,
    /** 0–100 confidence score produced by the AI. */
    val confidence: Int,
    /** Human-readable explanation from Grok. */
    val reasoning: String,
    /** ISO timestamp when the prediction was generated. */
    val generatedAt: String,
    /** Estimated target time or timeframe when this prediction is expected to materialize. */
    val targetTime: String,
)

/**
 * Tracks the accuracy of a prediction after the target time elapses.
 */
data class PredictionResult(
    /** The original prediction that was made. */
    val prediction: PricePrediction,
    /** Price at the moment the prediction was made. */
    val priceAtPrediction: Double,
    /** Actual live price after 5 minutes elapsed. */
    val actualPrice: Double,
    /** Difference: actualPrice - predictedPrice. */
    val priceError: Double,
    /** Absolute error as a percentage of priceAtPrediction. */
    val errorPercent: Double,
    /** Was the direction (UP/DOWN) correct? */
    val directionCorrect: Boolean,
    /** Did the actual price land within targetLow–targetHigh? */
    val withinRange: Boolean,
    /** Grade label: "Excellent", "Good", "Fair", "Off Target". */
    val grade: String,
)

/**
 * Signal values returned by the ML /compute-and-predict endpoint.
 */
enum class MlSignalDirection {
    UP,                  // model predicts >+1% in 5 days
    NOT_UP,              // model predicts flat or down
    NO_RELIABLE_SIGNAL,  // high-volatility regime — model has no edge
    INSUFFICIENT_DATA,   // not enough candles for indicator warm-up
}

/**
 * Domain model for the secondary ML badge (LightGBM, Option B integration).
 * Runs in parallel with the Grok prediction — displayed as a small badge.
 */
data class MlSignal(
    val symbol: String,
    val direction: MlSignalDirection,
    /** Probability of the predicted class (null when gated). */
    val confidence: Double?,
    /** "low_medium" → signal active | "high" → gated | "unknown" → no data */
    val volRegime: String,
    /** Current 20-day rolling volatility value. */
    val volatility: Double?,
    /** The volatility gate threshold used. */
    val volThreshold: Double,
    /** Number of OHLCV candles the server received. */
    val candlesUsed: Int,
    /** Disclaimer / explanation note from the server. */
    val note: String,
)
