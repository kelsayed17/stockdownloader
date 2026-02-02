"""Formats and prints options backtest reports to the console.

Includes options-specific metrics like premium collected, volume traded,
and theta decay.
"""

from stockdownloader.model.price_data import PriceData
from stockdownloader.backtest.options_backtest_result import OptionsBacktestResult

TRADING_DAYS_PER_YEAR = 252


def print_report(result: OptionsBacktestResult, data: list[PriceData]) -> None:
    sep = "=" * 80
    thin = "-" * 80
    print()
    print(sep)
    print(f"  OPTIONS BACKTEST REPORT: {result.strategy_name}")
    print(sep)
    print()
    print(f"  Period:              {result.start_date} to {result.end_date}")
    print(f"  Initial Capital:     ${result.initial_capital:,.2f}")
    print(f"  Final Capital:       ${result.final_capital:,.2f}")
    print()
    print(thin)
    print("  PERFORMANCE METRICS")
    print(thin)
    print(f"  Total Return:        {result.total_return:.2f}%")
    print(f"  Total P/L:           ${result.total_profit_loss:,.2f}")
    print(f"  Sharpe Ratio:        {result.sharpe_ratio(TRADING_DAYS_PER_YEAR)}")
    print(f"  Max Drawdown:        {result.max_drawdown:.2f}%")
    print(f"  Profit Factor:       {result.profit_factor}")
    print()
    print(thin)
    print("  TRADE STATISTICS")
    print(thin)
    print(f"  Total Trades:        {result.total_trades}")
    print(f"  Winning Trades:      {result.winning_trades}")
    print(f"  Losing Trades:       {result.losing_trades}")
    print(f"  Win Rate:            {result.win_rate:.2f}%")
    print(f"  Average Win:         ${result.average_win:,.2f}")
    print(f"  Average Loss:        ${result.average_loss:,.2f}")
    print()
    print(thin)
    print("  OPTIONS-SPECIFIC METRICS")
    print(thin)
    print(f"  Avg Premium/Trade:   ${result.average_premium_collected:,.2f}")
    print(f"  Total Volume:        {result.total_volume_traded:,} contracts")
    print()
    closed = result.closed_trades
    if closed:
        print(thin)
        print("  TRADE LOG")
        print(thin)
        for i, t in enumerate(closed, 1):
            print(f"  #{i:<4} {t}")
    print()
    print(sep)


def print_comparison(results: list[OptionsBacktestResult], data: list[PriceData]) -> None:
    sep = "=" * 100
    thin = "-" * 100
    print()
    print(sep)
    print("  OPTIONS STRATEGY COMPARISON")
    print(sep)
    print()

    print(f"  {'Strategy':<40} {'Return':>10} {'Sharpe':>10} {'MaxDD':>10} {'Trades':>10} {'WinRate':>10} {'Volume':>10}")
    print(thin)

    for r in results:
        print(f"  {r.strategy_name:<40} {r.total_return:>9.2f}% {r.sharpe_ratio(TRADING_DAYS_PER_YEAR):>10} "
              f"{r.max_drawdown:>9.2f}% {r.total_trades:>10} {r.win_rate:>9.2f}% {r.total_volume_traded:>10,}")

    print(thin)

    best = max(results, key=lambda r: r.total_return) if results else None
    if best:
        print()
        print(f"  Best performing strategy: {best.strategy_name}")
        print(f"  Return: {best.total_return:.2f}% | Sharpe: {best.sharpe_ratio(TRADING_DAYS_PER_YEAR)} | Max Drawdown: {best.max_drawdown:.2f}%")

    print()
    print(sep)
    print()
    print("  DISCLAIMER: This is for educational purposes only.")
    print("  Options trading involves significant risk. Past performance")
    print("  does not guarantee future results.")
    print()
