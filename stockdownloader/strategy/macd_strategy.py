"""Moving Average Convergence Divergence strategy.

Generates BUY on bullish crossover (MACD crosses above signal line)
and SELL on bearish crossover (MACD crosses below signal line).
"""

from stockdownloader.model.price_data import PriceData
from stockdownloader.strategy.trading_strategy import Signal, TradingStrategy
from stockdownloader.util.technical_indicators import macd_line, macd_signal


class MACDStrategy(TradingStrategy):
    """MACD crossover strategy with configurable fast, slow, and signal periods."""

    def __init__(
        self,
        fast: int = 12,
        slow: int = 26,
        signal_period: int = 9,
    ) -> None:
        if fast <= 0 or slow <= 0 or signal_period <= 0:
            raise ValueError("All periods must be positive")
        if fast >= slow:
            raise ValueError("Fast period must be less than slow period")
        self._fast = fast
        self._slow = slow
        self._signal_period = signal_period

    def evaluate(self, data: list[PriceData], index: int) -> Signal:
        min_required = self._slow + self._signal_period
        if index < min_required:
            return Signal.HOLD

        current_macd = macd_line(data, index, self._fast, self._slow)
        current_signal = macd_signal(
            data, index, self._fast, self._slow, self._signal_period
        )
        prev_macd = macd_line(data, index - 1, self._fast, self._slow)
        prev_signal = macd_signal(
            data, index - 1, self._fast, self._slow, self._signal_period
        )

        macd_above_now = current_macd > current_signal
        macd_above_prev = prev_macd > prev_signal

        if macd_above_now and not macd_above_prev:
            return Signal.BUY
        if not macd_above_now and macd_above_prev:
            return Signal.SELL
        return Signal.HOLD

    def get_name(self) -> str:
        return f"MACD ({self._fast}/{self._slow}/{self._signal_period})"
