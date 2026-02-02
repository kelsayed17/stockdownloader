"""Individual Symbol Analysis Application."""

import sys
import logging

from stockdownloader.data import yahoo_data_client
from stockdownloader.analysis import signal_generator

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def main(symbol: str = "SPY") -> None:
    """Run detailed analysis on a single symbol."""
    print(f"\n  Analyzing {symbol}...\n")

    data = yahoo_data_client.fetch_price_data(symbol, data_range="2y")
    if len(data) < 200:
        print(f"  Insufficient data for {symbol}: {len(data)} bars (need 200+)")
        return

    alert = signal_generator.generate_alert(data, len(data) - 1, symbol)
    print(alert)


if __name__ == "__main__":
    sym = sys.argv[1] if len(sys.argv) > 1 else "SPY"
    main(sym)
