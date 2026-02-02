"""Trend Analysis Application - scans stock universes for price patterns."""

import sys
import logging

from stockdownloader.data import yahoo_data_client, stock_list_downloader
from stockdownloader.analysis import signal_generator

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def main(symbols: list[str] | None = None) -> None:
    """Scan symbols for trading signals and print alerts."""
    if not symbols:
        print("  Downloading S&P 500 symbols...")
        symbols = stock_list_downloader.download_sp500_symbols()[:20]

    print(f"\n  Scanning {len(symbols)} symbols for signals...\n")

    for symbol in symbols:
        try:
            data = yahoo_data_client.fetch_price_data(symbol, data_range="1y")
            if len(data) < 200:
                print(f"  {symbol}: Insufficient data ({len(data)} bars)")
                continue

            alert = signal_generator.generate_alert(data, len(data) - 1, symbol)
            if alert.direction.name in ("STRONG_BUY", "STRONG_SELL"):
                print(alert)
            else:
                print(f"  {symbol}: {alert.direction.name} (score: {alert.confluence_score:.2f})")
        except Exception as e:
            logger.error(f"  Error analyzing {symbol}: {e}")


if __name__ == "__main__":
    syms = sys.argv[1:] if len(sys.argv) > 1 else None
    main(syms)
