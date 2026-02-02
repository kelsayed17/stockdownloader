"""Mean reversion strategy combining Bollinger Bands, RSI, and Stochastic Oscillator.

BUY when price touches/crosses below the lower Bollinger Band AND RSI is oversold
AND Stochastic %K is below 20.  Uses ADX as a trend filter to only trade in
range-bound markets (ADX < threshold).

SELL when price touches/crosses above the upper Bollinger Band AND RSI is overbought
AND Stochastic %K is above 80.
"""

from stockdownloader.model.price_data import PriceData
from stockdownloader.strategy.trading_strategy import Signal, TradingStrategy
from stockdownloader.util.technical_indicators import (
    adx,
    bollinger_bands,
    rsi,
    stochastic,
)


class BollingerBandRSI(TradingStrategy):
    """Bollinger Band + RSI mean-reversion strategy with ADX trend filter."""

    def __init__(
        self,
        bb_period: int = 20,
        bb_std_dev: float = 2.0,
        rsi_period: int = 14,
        rsi_oversold: float = 30.0,
        rsi_overbought: float = 70.0,
        adx_threshold: float = 25.0,
    ) -> None:
        self._bb_period = bb_period
        self._bb_std_dev = bb_std_dev
        self._rsi_period = rsi_period
        self._rsi_oversold = rsi_oversold
        self._rsi_overbought = rsi_overbought
        self._adx_threshold = adx_threshold

    def evaluate(self, data: list[PriceData], index: int) -> Signal:
        warmup = max(self._bb_period, self._rsi_period + 1, 28)
        if index < warmup:
            return Signal.HOLD

        close = data[index].close
        prev_close = data[index - 1].close

        bb = bollinger_bands(data, index, self._bb_period, self._bb_std_dev)
        current_rsi = rsi(data, index, self._rsi_period)
        prev_rsi = rsi(data, index - 1, self._rsi_period)
        stoch = stochastic(data, index)
        adx_result = adx(data, index)

        # Only trade in range-bound markets
        is_range_bound = adx_result.adx < self._adx_threshold

        # BUY: price at/below lower BB, RSI crosses above oversold, Stochastic < 20
        price_at_lower_bb = (
            close <= bb.lower
            or (prev_close < bb.lower and close >= bb.lower)
        )
        rsi_recovering = (
            current_rsi > self._rsi_oversold and prev_rsi <= self._rsi_oversold
        )
        stoch_oversold = stoch.percent_k < 20

        if is_range_bound and (price_at_lower_bb or rsi_recovering) and stoch_oversold:
            return Signal.BUY

        # SELL: price at/above upper BB, RSI crosses below overbought, Stochastic > 80
        price_at_upper_bb = (
            close >= bb.upper
            or (prev_close > bb.upper and close <= bb.upper)
        )
        rsi_topping = (
            current_rsi < self._rsi_overbought and prev_rsi >= self._rsi_overbought
        )
        stoch_overbought = stoch.percent_k > 80

        if is_range_bound and (price_at_upper_bb or rsi_topping) and stoch_overbought:
            return Signal.SELL

        return Signal.HOLD

    def get_name(self) -> str:
        return "Bollinger Band + RSI"
