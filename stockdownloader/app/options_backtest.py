"""Options Backtesting Application - runs multiple options strategies."""

import sys
import logging

from stockdownloader.model.option_type import OptionType
from stockdownloader.model.options_trade import OptionsDirection
from stockdownloader.data import csv_loader, yahoo_data_client
from stockdownloader.strategy.covered_call import CoveredCallStrategy
from stockdownloader.strategy.protective_put import ProtectivePutStrategy
from stockdownloader.backtest.options_backtest_engine import OptionsBacktestEngine
from stockdownloader.backtest import options_backtest_report

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def main(symbol: str = "SPY", csv_path: str | None = None, data_range: str = "2y") -> None:
    """Run backtests on multiple options strategies."""
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

    # Covered Call variants
    cc_strategies = [
        (CoveredCallStrategy(20, 0.03), OptionType.CALL, OptionsDirection.SELL),
        (CoveredCallStrategy(20, 0.05), OptionType.CALL, OptionsDirection.SELL),
        (CoveredCallStrategy(50, 0.05), OptionType.CALL, OptionsDirection.SELL),
    ]

    # Protective Put variants
    pp_strategies = [
        (ProtectivePutStrategy(20, 0.05), OptionType.PUT, OptionsDirection.BUY),
        (ProtectivePutStrategy(20, 0.03), OptionType.PUT, OptionsDirection.BUY),
        (ProtectivePutStrategy(50, 0.05), OptionType.PUT, OptionsDirection.BUY),
    ]

    results = []

    for strategy, opt_type, direction in cc_strategies + pp_strategies:
        print(f"  Running: {strategy.get_name()}...")
        engine = OptionsBacktestEngine(
            initial_capital=100_000.0,
            commission_per_contract=0.65,
            option_type=opt_type,
            direction=direction,
        )
        result = engine.run(strategy, data)
        results.append(result)
        options_backtest_report.print_report(result, data)

    options_backtest_report.print_comparison(results, data)


if __name__ == "__main__":
    args = sys.argv[1:]
    symbol = args[0] if args else "SPY"
    csv_file = args[1] if len(args) > 1 else None
    main(symbol=symbol, csv_path=csv_file)
