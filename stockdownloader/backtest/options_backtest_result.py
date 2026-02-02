"""Stores options backtest results and computes performance metrics.

Includes total P/L from premiums, win rate, average premium captured,
max drawdown, and volume statistics.
"""

import math
from stockdownloader.model.options_trade import OptionsTrade, OptionsTradeStatus
from stockdownloader.model.price_data import PriceData


class OptionsBacktestResult:
    def __init__(self, strategy_name: str, initial_capital: float):
        self.strategy_name = strategy_name
        self.initial_capital = initial_capital
        self.final_capital = initial_capital
        self._trades: list[OptionsTrade] = []
        self.equity_curve: list[float] = []
        self.start_date: str = ""
        self.end_date: str = ""
        self.total_volume_traded: int = 0

    def add_trade(self, trade: OptionsTrade) -> None:
        self._trades.append(trade)
        self.total_volume_traded += trade.entry_volume

    @property
    def closed_trades(self) -> list[OptionsTrade]:
        return [t for t in self._trades if t.status != OptionsTradeStatus.OPEN]

    @property
    def total_return(self) -> float:
        if self.initial_capital == 0:
            return 0.0
        return ((self.final_capital - self.initial_capital) / self.initial_capital) * 100

    @property
    def total_profit_loss(self) -> float:
        return self.final_capital - self.initial_capital

    @property
    def total_trades(self) -> int:
        return len(self.closed_trades)

    @property
    def winning_trades(self) -> int:
        return sum(1 for t in self.closed_trades if t.is_win)

    @property
    def losing_trades(self) -> int:
        return self.total_trades - self.winning_trades

    @property
    def win_rate(self) -> float:
        if self.total_trades == 0:
            return 0.0
        return (self.winning_trades / self.total_trades) * 100

    @property
    def average_win(self) -> float:
        wins = [t.profit_loss for t in self.closed_trades if t.is_win]
        return sum(wins) / len(wins) if wins else 0.0

    @property
    def average_loss(self) -> float:
        losses = [t.profit_loss for t in self.closed_trades if not t.is_win]
        return sum(losses) / len(losses) if losses else 0.0

    @property
    def profit_factor(self) -> float:
        gross_profit = sum(t.profit_loss for t in self.closed_trades if t.is_win)
        gross_loss = sum(abs(t.profit_loss) for t in self.closed_trades if not t.is_win)
        if gross_loss == 0:
            return 999.99 if gross_profit > 0 else 0.0
        return round(gross_profit / gross_loss, 2)

    @property
    def max_drawdown(self) -> float:
        if not self.equity_curve:
            return 0.0
        peak = self.equity_curve[0]
        max_dd = 0.0
        for equity in self.equity_curve:
            if equity > peak:
                peak = equity
            dd = ((peak - equity) / peak) * 100 if peak > 0 else 0.0
            if dd > max_dd:
                max_dd = dd
        return max_dd

    def sharpe_ratio(self, trading_days_per_year: int = 252) -> float:
        if len(self.equity_curve) < 2:
            return 0.0
        daily_returns = []
        for i in range(1, len(self.equity_curve)):
            prev = self.equity_curve[i - 1]
            if prev != 0:
                daily_returns.append((self.equity_curve[i] - prev) / prev)
        if not daily_returns:
            return 0.0
        mean_return = sum(daily_returns) / len(daily_returns)
        sum_sq = sum((r - mean_return) ** 2 for r in daily_returns)
        std_dev = math.sqrt(sum_sq / len(daily_returns))
        if std_dev == 0:
            return 0.0
        return round((mean_return / std_dev) * math.sqrt(trading_days_per_year), 2)

    def buy_and_hold_return(self, data: list[PriceData]) -> float:
        if not data:
            return 0.0
        first = data[0].close
        last = data[-1].close
        return ((last - first) / first) * 100 if first != 0 else 0.0

    @property
    def average_premium_collected(self) -> float:
        """Average premium collected per trade (for short strategies)."""
        closed = self.closed_trades
        if not closed:
            return 0.0
        total_premium = sum(t.total_entry_cost() for t in closed)
        return round(total_premium / len(closed), 2)

    @property
    def trades(self) -> list[OptionsTrade]:
        return list(self._trades)
