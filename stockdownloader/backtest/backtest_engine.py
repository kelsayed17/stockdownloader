"""Core backtesting simulation engine."""

import math
from stockdownloader.model.price_data import PriceData
from stockdownloader.model.trade import Trade, TradeDirection, TradeStatus
from stockdownloader.strategy.trading_strategy import TradingStrategy, Signal
from stockdownloader.backtest.backtest_result import BacktestResult


class BacktestEngine:
    def __init__(self, initial_capital: float = 100_000.0, commission: float = 0.0):
        self.initial_capital = initial_capital
        self.commission = commission

    def run(self, strategy: TradingStrategy, data: list[PriceData]) -> BacktestResult:
        if not data:
            raise ValueError("data must not be empty")

        result = BacktestResult(strategy.get_name(), self.initial_capital)
        cash = self.initial_capital
        current_trade: Trade | None = None
        equity_curve: list[float] = []

        result.start_date = data[0].date
        result.end_date = data[-1].date

        for i, bar in enumerate(data):
            signal = strategy.evaluate(data, i)

            equity = cash
            if current_trade and current_trade.status == TradeStatus.OPEN:
                equity = cash + bar.close * current_trade.shares
            equity_curve.append(equity)

            if signal == Signal.BUY and current_trade is None:
                shares = int((cash - self.commission) / bar.close)
                if shares > 0:
                    cost = bar.close * shares + self.commission
                    cash -= cost
                    current_trade = Trade(TradeDirection.LONG, bar.date, bar.close, shares)

            elif signal == Signal.SELL and current_trade and current_trade.status == TradeStatus.OPEN:
                proceeds = bar.close * current_trade.shares - self.commission
                current_trade.close(bar.date, bar.close)
                cash += proceeds
                result.add_trade(current_trade)
                current_trade = None

        # Close any open position at end
        if current_trade and current_trade.status == TradeStatus.OPEN:
            last_bar = data[-1]
            proceeds = last_bar.close * current_trade.shares - self.commission
            current_trade.close(last_bar.date, last_bar.close)
            cash += proceeds
            result.add_trade(current_trade)

        result.final_capital = cash
        result.equity_curve = equity_curve
        return result
