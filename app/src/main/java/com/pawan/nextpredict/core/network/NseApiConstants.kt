package com.pawan.nextpredict.core.network

/**
 * Centralized NSE API endpoint constants.
 * All URLs are defined here to ensure a single source of truth.
 */
object NseApiConstants {

    const val BASE_URL = "https://www.nseindia.com/"

    // ─── Market ───────────────────────────────────────────────────────────────
    /** Market status: open, closed, pre-open */
    const val MARKET_STATUS = "api/marketStatus"

    /** All indices overview */
    const val ALL_INDICES = "api/allIndices"

    /** Equity stock indices — pass ?index=NIFTY%2050 etc. */
    const val EQUITY_STOCK_INDICES = "api/equity-stockIndices"

    // ─── Equity Quote ─────────────────────────────────────────────────────────
    /** Equity quote — pass ?symbol=RELIANCE */
    const val QUOTE_EQUITY = "api/quote-equity"

    /** Trade information — pass ?symbol=RELIANCE&section=trade_info */
    const val TRADE_INFO = "api/quote-equity"

    // ─── Option Chain ─────────────────────────────────────────────────────────
    /** Option chain for equities — pass ?symbol=RELIANCE */
    const val OPTION_CHAIN_EQUITY = "api/option-chain-equities"

    /** Option chain for indices — pass ?symbol=NIFTY */
    const val OPTION_CHAIN_INDEX = "api/option-chain-indices"

    // ─── Market Movers ────────────────────────────────────────────────────────
    /** Top gainers — pass ?index=NIFTY */
    const val TOP_GAINERS = "api/live-analysis-variations"

    /** Most active by volume */
    const val MOST_ACTIVE_SECURITIES = "api/live-analysis-volume-gainers"

    /** Pre-open market data — pass ?key=NIFTY */
    const val PRE_OPEN_MARKET = "api/market-data-pre-open"

    // ─── Historical / Chart ───────────────────────────────────────────────────
    /** Historical data — required: symbol, series, from, to */
    const val HISTORICAL_DATA = "api/historical/cm/equity"

    // ─── Search ───────────────────────────────────────────────────────────────
    /** Search autocomplete — pass ?q=REL */
    const val SEARCH = "api/search/autocomplete"

    /** Complete equity master list */
    const val EQUITY_MASTER = "api/equity-master"

    // ─── Company Info ─────────────────────────────────────────────────────────
    /** Corporate info — pass ?symbol=RELIANCE&market=equities */
    const val COMPANY_INFO = "api/quote-equity"

    /** Corporate announcements — pass ?index=equities&symbol=RELIANCE */
    const val CORPORATE_ANNOUNCEMENTS = "api/corporate-announcements"

    // ─── Index names for EQUITY_STOCK_INDICES ─────────────────────────────────
    object Index {
        const val NIFTY_50 = "NIFTY 50"
        const val NIFTY_BANK = "NIFTY BANK"
        const val NIFTY_NEXT_50 = "NIFTY NEXT 50"
        const val NIFTY_MIDCAP_100 = "NIFTY MIDCAP 100"
        const val NIFTY_IT = "NIFTY IT"
        const val NIFTY_PHARMA = "NIFTY PHARMA"
        const val NIFTY_FMCG = "NIFTY FMCG"
        const val NIFTY_AUTO = "NIFTY AUTO"
        const val NIFTY_METAL = "NIFTY METAL"
        const val NIFTY_REALTY = "NIFTY REALTY"
        const val SENSEX = "SENSEX"
    }
}
