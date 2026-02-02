"""SPY Backtesting Application - runs multiple equity strategies against historical data."""

import sys
import logging

from stockdownloader.data import csv_loader, yahoo_data_client
from stockdownloader.strategy.sma_crossover import SMACrossoverStrategy
from stockdownloader.strategy.rsi_strategy import RSIStrategy
from stockdownloader.strategy.macd_strategy import MACDStrategy
from stockdownloader.strategy.bollinger_band_rsi import BollingerBandRSIStrategy
from stockdownloader.strategy.breakout import BreakoutStrategy
from stockdownloader.strategy.momentum_confluence import MomentumConfluenceStrategy
from stockdownloader.strategy.multi_indicator import MultiIndicatorStrategy
from stockdownloader.backtest.backtest_engine import BacktestEngine
from stockdownloader.backtest import backtest_report

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def main(symbol: str = "SPY", csv_path: str | None = None, data_range: str = "2y") -> None:
    """Run backtests on multiple equity strategies."""
    print(f"\n  Loading data for {symbol}...")

    if csv_path:
        data = csv_loader.load_from_file(csv_path)
        print(f"  Loaded {len(data)} bars from CSV: {csv_path}")
    else:
        data = yahoo_data_client.fetch_price_data(symbol, data_range=data_range)
        print(f"  Loaded {len(data)} bars from Yahoo Finance ({data_range})")

    if not data:
        print("  ERROR: No data loaded. Exiting.")
        return

    strategies = [
        SMACrossoverStrategy(50, 200),
        SMACrossoverStrategy(20, 50),
        RSIStrategy(14, 30.0, 70.0),
        RSIStrategy(14, 25.0, 75.0),
        MACDStrategy(),
        BollingerBandRSIStrategy(),
        MomentumConfluenceStrategy(),
        BreakoutStrategy(),
        MultiIndicatorStrategy(),
    ]

    engine = BacktestEngine(initial_capital=100_000.0, commission=0.0)
    results = []

    for strategy in strategies:
        print(f"  Running: {strategy.get_name()}...")
        result = engine.run(strategy, data)
        results.append(result)
        backtest_report.print_report(result, data)

    backtest_report.print_comparison(results, data)


if __name__ == "__main__":
    args = sys.argv[1:]
    symbol = args[0] if args else "SPY"
    csv_file = args[1] if len(args) > 1 else None
    main(symbol=symbol, csv_path=csv_file)
