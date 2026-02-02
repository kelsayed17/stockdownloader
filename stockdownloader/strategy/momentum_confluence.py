"""Momentum/trend-following confluence strategy.

Combines MACD, ADX, EMA trend filter, and OBV volume confirmation.

BUY when: MACD bullish crossover AND ADX > 25 (strong trend) AND
          bullish DI AND price > EMA(200) AND OBV rising.
SELL when: MACD bearish crossover OR trend weakening (ADX < 20 AND below EMA).
"""

from stockdownloader.model.price_data import PriceData
from stockdownloader.strategy.trading_strategy import Signal, TradingStrategy
from stockdownloader.util.moving_average import ema
from stockdownloader.util.technical_indicators import (
    adx,
    is_obv_rising,
    macd_line,
    macd_signal,
)


class MomentumConfluence(TradingStrategy):
    """Multi-factor momentum strategy using MACD, ADX, EMA, and OBV."""

    def __init__(
        self,
        rsi_period: int = 14,
        macd_fast: int = 12,
        macd_slow: int = 26,
        macd_signal_period: int = 9,
        ema_trend_filter: int = 200,
        adx_strength_threshold: float = 25.0,
        adx_weak_threshold: float = 20.0,
    ) -> None:
        self._rsi_period = rsi_period
        self._macd_fast = macd_fast
        self._macd_slow = macd_slow
        self._macd_signal_period = macd_signal_period
        self._ema_trend_filter = ema_trend_filter
        self._adx_strength = adx_strength_threshold
        self._adx_weak = adx_weak_threshold

    def evaluate(self, data: list[PriceData], index: int) -> Signal:
        warmup = max(
            self._ema_trend_filter,
            self._macd_slow + self._macd_signal_period,
        ) + 1
        if index < warmup:
            return Signal.HOLD

        close = data[index].close

        # MACD crossover detection
        curr_macd = macd_line(data, index, self._macd_fast, self._macd_slow)
        curr_signal = macd_signal(
            data, index, self._macd_fast, self._macd_slow, self._macd_signal_period
        )
        prev_macd = macd_line(data, index - 1, self._macd_fast, self._macd_slow)
        prev_signal = macd_signal(
            data, index - 1, self._macd_fast, self._macd_slow, self._macd_signal_period
        )

        bullish_cross = curr_macd > curr_signal and prev_macd <= prev_signal
        bearish_cross = curr_macd < curr_signal and prev_macd >= prev_signal

        # ADX for trend strength
        adx_result = adx(data, index)
        strong_trend = adx_result.adx > self._adx_strength
        weak_trend = adx_result.adx < self._adx_weak
        bullish_di = adx_result.plus_di > adx_result.minus_di

        # EMA trend filter
        ema_val = ema(data, index, self._ema_trend_filter)
        above_trend_ema = close > ema_val

        # OBV confirmation
        obv_confirm = is_obv_rising(data, index, 5)

        # BUY: all conditions met
        if bullish_cross and strong_trend and bullish_di and above_trend_ema and obv_confirm:
            return Signal.BUY

        # SELL: bearish crossover or trend weakening below EMA
        if bearish_cross or (weak_trend and not above_trend_ema):
            return Signal.SELL

        return Signal.HOLD

    def get_name(self) -> str:
        return "Momentum Confluence"
