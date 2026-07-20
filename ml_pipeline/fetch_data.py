"""
fetch_data.py
-------------
Downloads historical OHLCV data for an NSE stock using yfinance.
NSE symbols must have the .NS suffix (e.g. RELIANCE.NS, TCS.NS, INFY.NS).

Usage:
    python fetch_data.py --symbol RELIANCE.NS --start 2016-01-01 --end 2024-12-31
    python fetch_data.py --symbol TCS.NS           # uses default 8-year range
"""

import argparse
import sys
from datetime import date, timedelta

import pandas as pd
import yfinance as yf


def parse_args():
    today = date.today()
    default_end = today.strftime("%Y-%m-%d")
    default_start = (today - timedelta(days=365 * 9)).strftime("%Y-%m-%d")

    parser = argparse.ArgumentParser(
        description="Download NSE stock OHLCV data from Yahoo Finance."
    )
    parser.add_argument(
        "--symbol",
        type=str,
        default="RELIANCE.NS",
        help="NSE ticker symbol with .NS suffix (default: RELIANCE.NS)",
    )
    parser.add_argument(
        "--start",
        type=str,
        default=default_start,
        help=f"Start date YYYY-MM-DD (default: ~9 years ago → {default_start})",
    )
    parser.add_argument(
        "--end",
        type=str,
        default=default_end,
        help=f"End date YYYY-MM-DD (default: today → {default_end})",
    )
    parser.add_argument(
        "--output",
        type=str,
        default="data.csv",
        help="Output CSV filename (default: data.csv)",
    )
    return parser.parse_args()


def download(symbol: str, start: str, end: str) -> pd.DataFrame:
    print(f"\n📥  Downloading: {symbol}  |  {start} → {end}")
    ticker = yf.Ticker(symbol)
    df = ticker.history(start=start, end=end, auto_adjust=True)

    if df.empty:
        print(
            f"❌  No data returned for '{symbol}'. "
            "Check the ticker symbol and date range."
        )
        sys.exit(1)

    # Keep standard OHLCV columns; drop Dividends / Stock Splits
    df = df[["Open", "High", "Low", "Close", "Volume"]].copy()
    df.index = pd.to_datetime(df.index)
    df.index.name = "Date"
    return df


def main():
    args = parse_args()

    df = download(args.symbol, args.start, args.end)

    print(f"\n✅  Downloaded {len(df)} trading days.")
    print(f"   Date range : {df.index.min().date()} → {df.index.max().date()}")
    print(f"   Columns    : {list(df.columns)}")
    print("\n--- First 5 rows ---")
    print(df.head().to_string())
    print("\n--- Last 5 rows ---")
    print(df.tail().to_string())
    print(f"\n📊  Basic statistics:")
    print(df[["Open", "High", "Low", "Close", "Volume"]].describe().to_string())

    df.to_csv(args.output)
    print(f"\n💾  Saved to: {args.output}")


if __name__ == "__main__":
    main()
