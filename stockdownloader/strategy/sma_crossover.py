"""Simple Moving Average crossover strategy.

Generates BUY on golden cross (short SMA crosses above long SMA)
and SELL on death cross (short SMA crosses below long SMA).
"""

from stockdownloader.model.price_data import PriceData
from stockdownloader.strategy.trading_strategy import Signal, TradingStrategy
from stockdownloader.util.moving_average import sma


class SMACrossover(TradingStrategy):
    """SMA crossover strategy comparing a short-period and long-period SMA."""

    def __init__(self, short_period: int, long_period: int) -> None:
        if short_period <= 0 or long_period <= 0:
            raise ValueError("Periods must be positive")
        if short_period >= long_period:
            raise ValueError("Short period must be less than long period")
        self._short_period = short_period
        self._long_period = long_period

    def evaluate(self, data: list[PriceData], index: int) -> Signal:
        if index < self._long_period:
            return Signal.HOLD

        current_short = sma(data, index, self._short_period)
        current_long = sma(data, index, self._long_period)
        prev_short = sma(data, index - 1, self._short_period)
        prev_long = sma(data, index - 1, self._long_period)

        short_above_now = current_short > current_long
        short_above_prev = prev_short > prev_long

        if short_above_now and not short_above_prev:
            return Signal.BUY
        if not short_above_now and short_above_prev:
            return Signal.SELL
        return Signal.HOLD

    def get_name(self) -> str:
        return f"SMA Crossover ({self._short_period}/{self._long_period})"
