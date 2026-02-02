"""Breakout strategy using Bollinger Band squeeze detection with volume and ATR confirmation.

Detects low-volatility consolidation periods (Bollinger squeeze) and enters
when price breaks out with volume confirmation.

BUY when: BB width at N-period low (squeeze) AND price breaks above upper band
          AND volume > volume_factor * average volume AND ATR expanding.
SELL when: Price breaks below lower band, or a failed breakout (price falls
           back below middle band from above upper band).
"""

from stockdownloader.model.price_data import PriceData
from stockdownloader.strategy.trading_strategy import Signal, TradingStrategy
from stockdownloader.util.technical_indicators import (
    atr,
    average_volume,
    bollinger_bands,
)


class BreakoutStrategy(TradingStrategy):
    """Bollinger Band squeeze breakout strategy with volume and ATR confirmation."""

    def __init__(
        self,
        lookback: int = 20,
        volume_factor: float = 1.5,
        bb_period: int = 20,
        squeeze_lookback: int = 120,
    ) -> None:
        self._lookback = lookback
        self._volume_factor = volume_factor
        self._bb_period = bb_period
        self._squeeze_lookback = squeeze_lookback

    def evaluate(self, data: list[PriceData], index: int) -> Signal:
        warmup = max(self._bb_period, self._squeeze_lookback) + 1
        if index < warmup:
            return Signal.HOLD

        close = data[index].close
        prev_close = data[index - 1].close
        volume = data[index].volume

        bb = bollinger_bands(data, index, self._bb_period, 2.0)
        prev_bb = bollinger_bands(data, index - 1, self._bb_period, 2.0)

        # Squeeze detection
        in_squeeze = self._is_in_squeeze(data, index)

        # Volume confirmation
        avg_vol = average_volume(data, index, self._lookback)
        high_volume = avg_vol > 0 and volume > self._volume_factor * avg_vol

        # ATR expanding
        curr_atr = atr(data, index, 14)
        prev_atr = atr(data, index - 1, 14)
        atr_expanding = curr_atr > prev_atr

        # Bullish breakout: price closes above upper BB
        bullish_breakout = close > bb.upper and prev_close <= prev_bb.upper

        if (in_squeeze or atr_expanding) and bullish_breakout and high_volume:
            return Signal.BUY

        # Bearish breakdown: price closes below lower BB
        bearish_breakdown = close < bb.lower and prev_close >= prev_bb.lower
        if bearish_breakdown:
            return Signal.SELL

        # Failed breakout: was above upper band, fell back below middle
        failed_breakout = prev_close > prev_bb.upper and close < bb.middle
        if failed_breakout:
            return Signal.SELL

        return Signal.HOLD

    def get_name(self) -> str:
        return f"Breakout ({self._lookback})"

    def _is_in_squeeze(self, data: list[PriceData], index: int) -> bool:
        """Check whether the current BB width is near the recent minimum."""
        current_bb = bollinger_bands(data, index, self._bb_period, 2.0)
        current_width = current_bb.width
        if current_width == 0:
            return False

        min_width = current_width
        lookback_start = max(self._bb_period, index - self._squeeze_lookback)

        for i in range(lookback_start, index):
            past_bb = bollinger_bands(data, i, self._bb_period, 2.0)
            if 0 < past_bb.width < min_width:
                min_width = past_bb.width

        # Squeeze if current width is within 10% of the minimum
        threshold = min_width * 1.10
        return current_width <= threshold
