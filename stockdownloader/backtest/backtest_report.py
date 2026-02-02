"""Formats and prints backtest reports to the console."""

from stockdownloader.model.price_data import PriceData
from stockdownloader.backtest.backtest_result import BacktestResult

TRADING_DAYS_PER_YEAR = 252


def print_report(result: BacktestResult, data: list[PriceData]) -> None:
    sep = "=" * 70
    thin = "-" * 70
    print()
    print(sep)
    print(f"  BACKTEST REPORT: {result.strategy_name}")
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
    print(f"  Buy & Hold Return:   {result.buy_and_hold_return(data):.2f}%")
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
    closed = result.closed_trades
    if closed:
        print(thin)
        print("  TRADE LOG")
        print(thin)
        for i, t in enumerate(closed, 1):
            print(f"  #{i:<4} {t}")
    print()
    print(sep)


def print_comparison(results: list[BacktestResult], data: list[PriceData]) -> None:
    sep = "=" * 90
    thin = "-" * 90
    print()
    print(sep)
    print("  STRATEGY COMPARISON SUMMARY")
    print(sep)
    print()
    bh = 0.0
    if data:
        first = data[0].close
        last = data[-1].close
        bh = ((last - first) / first) * 100 if first != 0 else 0.0

    print(f"  {'Strategy':<35} {'Return':>10} {'Sharpe':>10} {'MaxDD':>10} {'Trades':>10} {'Win Rate':>10}")
    print(thin)
    print(f"  {'Buy & Hold (Benchmark)':<35} {bh:>9.2f}% {'N/A':>10} {'N/A':>10} {'1':>10} {'N/A':>10}")

    for r in results:
        print(f"  {r.strategy_name:<35} {r.total_return:>9.2f}% {r.sharpe_ratio(TRADING_DAYS_PER_YEAR):>10} "
              f"{r.max_drawdown:>9.2f}% {r.total_trades:>10} {r.win_rate:>9.2f}%")

    print(thin)
    best = max(results, key=lambda r: r.total_return) if results else None
    if best:
        print()
        print(f"  Best performing strategy: {best.strategy_name}")
        print(f"  Return: {best.total_return:.2f}% | Sharpe: {best.sharpe_ratio(TRADING_DAYS_PER_YEAR)} | Max Drawdown: {best.max_drawdown:.2f}%")
        diff = best.total_return - bh
        if diff > 0:
            print(f"  >> Outperformed Buy & Hold by {diff:.2f} percentage points")
        else:
            print(f"  >> Underperformed Buy & Hold by {abs(diff):.2f} percentage points")
    print()
    print(sep)
    print()
    print("  DISCLAIMER: This is for educational purposes only.")
    print("  Past performance does not guarantee future results.")
    print("  Always do your own research before trading.")
    print()
