"""Relative Strength Index strategy.

Generates BUY when RSI crosses above the oversold threshold
and SELL when RSI crosses below the overbought threshold.
"""

from stockdownloader.model.price_data import PriceData
from stockdownloader.strategy.trading_strategy import Signal, TradingStrategy
from stockdownloader.util.technical_indicators import rsi


class RSIStrategy(TradingStrategy):
    """RSI-based mean-reversion strategy with configurable thresholds."""

    def __init__(
        self,
        period: int = 14,
        oversold: float = 30.0,
        overbought: float = 70.0,
    ) -> None:
        if period <= 0:
            raise ValueError("Period must be positive")
        if oversold < 0 or overbought > 100 or oversold >= overbought:
            raise ValueError(
                "Invalid threshold values: oversold must be < overbought, within [0, 100]"
            )
        self._period = period
        self._oversold = oversold
        self._overbought = overbought

    def evaluate(self, data: list[PriceData], index: int) -> Signal:
        if index < self._period + 1:
            return Signal.HOLD

        current_rsi = rsi(data, index, self._period)
        prev_rsi = rsi(data, index - 1, self._period)

        # BUY when RSI crosses above the oversold level (recovery)
        if current_rsi > self._oversold and prev_rsi <= self._oversold:
            return Signal.BUY
        # SELL when RSI crosses below the overbought level (reversal)
        if current_rsi < self._overbought and prev_rsi >= self._overbought:
            return Signal.SELL
        return Signal.HOLD

    def get_name(self) -> str:
        return f"RSI ({self._oversold:.0f}/{self._overbought:.0f})"
