"""Multi-indicator confluence strategy.

Scores buy/sell signals across trend, momentum, volatility, and volume
indicator categories.  Each indicator that agrees with a direction adds
to the confluence score.  A trade is only taken when the score meets or
exceeds the configured threshold.

Scored indicators (8 total):
  Trend:      EMA(12) > EMA(26), Price > SMA(200), Ichimoku above cloud
  Momentum:   RSI recovery from oversold, MACD bullish crossover, Stochastic oversold
  Volume:     OBV rising, MFI oversold recovery
"""

from stockdownloader.model.price_data import PriceData
from stockdownloader.strategy.trading_strategy import Signal, TradingStrategy
from stockdownloader.util.moving_average import ema, sma
from stockdownloader.util.technical_indicators import (
    ichimoku,
    is_obv_rising,
    macd_line,
    macd_signal,
    mfi,
    obv,
    rsi,
    stochastic,
)


class MultiIndicatorStrategy(TradingStrategy):
    """Multi-indicator confluence strategy with configurable buy/sell thresholds."""

    def __init__(
        self,
        buy_threshold: int = 4,
        sell_threshold: int = 4,
    ) -> None:
        if buy_threshold < 1 or sell_threshold < 1:
            raise ValueError("Thresholds must be >= 1")
        self._buy_threshold = buy_threshold
        self._sell_threshold = sell_threshold

    def evaluate(self, data: list[PriceData], index: int) -> Signal:
        # Need 200 bars for SMA(200) + 1 for crossover detection
        if index < 201:
            return Signal.HOLD

        buy_score = self._compute_buy_score(data, index)
        sell_score = self._compute_sell_score(data, index)

        if buy_score >= self._buy_threshold and buy_score > sell_score:
            return Signal.BUY
        if sell_score >= self._sell_threshold and sell_score > buy_score:
            return Signal.SELL
        return Signal.HOLD

    def get_name(self) -> str:
        return "Multi-Indicator"

    # ------------------------------------------------------------------
    # Private scoring helpers
    # ------------------------------------------------------------------

    def _compute_buy_score(self, data: list[PriceData], index: int) -> int:
        score = 0

        # --- Trend ---
        # EMA(12) > EMA(26) -- short-term bullish
        ema12 = ema(data, index, 12)
        ema26 = ema(data, index, 26)
        if ema12 > ema26:
            score += 1

        # Price > SMA(200) -- long-term uptrend
        sma200 = sma(data, index, 200)
        close = data[index].close
        if sma200 > 0 and close > sma200:
            score += 1

        # Ichimoku -- price above cloud
        ich = ichimoku(data, index)
        if ich.price_above_cloud:
            score += 1

        # --- Momentum ---
        # RSI recovers from oversold (crosses above 30)
        current_rsi = rsi(data, index, 14)
        prev_rsi = rsi(data, index - 1, 14)
        if current_rsi > 30 and prev_rsi <= 30:
            score += 1

        # MACD bullish crossover
        curr_macd = macd_line(data, index, 12, 26)
        curr_signal = macd_signal(data, index, 12, 26, 9)
        prev_macd = macd_line(data, index - 1, 12, 26)
        prev_signal_val = macd_signal(data, index - 1, 12, 26, 9)
        if curr_macd > curr_signal and prev_macd <= prev_signal_val:
            score += 1

        # Stochastic %K crossing above %D from oversold zone
        stoch = stochastic(data, index)
        prev_stoch = stochastic(data, index - 1)
        if (
            stoch.percent_k < 30
            and stoch.percent_k > stoch.percent_d
            and prev_stoch.percent_k <= prev_stoch.percent_d
        ):
            score += 1

        # --- Volume ---
        # OBV rising (accumulation)
        if is_obv_rising(data, index, 5):
            score += 1

        # MFI oversold recovery
        current_mfi = mfi(data, index, 14)
        prev_mfi = mfi(data, index - 1, 14)
        if current_mfi < 30 and current_mfi > prev_mfi:
            score += 1

        return score

    def _compute_sell_score(self, data: list[PriceData], index: int) -> int:
        score = 0

        # --- Trend ---
        # EMA(12) < EMA(26) -- short-term bearish
        ema12 = ema(data, index, 12)
        ema26 = ema(data, index, 26)
        if ema12 < ema26:
            score += 1

        # Price < SMA(200) -- long-term downtrend
        sma200 = sma(data, index, 200)
        close = data[index].close
        if sma200 > 0 and close < sma200:
            score += 1

        # Ichimoku -- price below cloud
        ich = ichimoku(data, index)
        if not ich.price_above_cloud and ich.senkou_span_a > 0:
            score += 1

        # --- Momentum ---
        # RSI falls from overbought (crosses below 70)
        current_rsi = rsi(data, index, 14)
        prev_rsi = rsi(data, index - 1, 14)
        if current_rsi < 70 and prev_rsi >= 70:
            score += 1

        # MACD bearish crossover
        curr_macd = macd_line(data, index, 12, 26)
        curr_signal = macd_signal(data, index, 12, 26, 9)
        prev_macd = macd_line(data, index - 1, 12, 26)
        prev_signal_val = macd_signal(data, index - 1, 12, 26, 9)
        if curr_macd < curr_signal and prev_macd >= prev_signal_val:
            score += 1

        # Stochastic %K crossing below %D from overbought zone
        stoch = stochastic(data, index)
        prev_stoch = stochastic(data, index - 1)
        if (
            stoch.percent_k > 70
            and stoch.percent_k < stoch.percent_d
            and prev_stoch.percent_k >= prev_stoch.percent_d
        ):
            score += 1

        # --- Volume ---
        # OBV falling (distribution)
        if not is_obv_rising(data, index, 5):
            score += 1

        # MFI overbought reversal
        current_mfi = mfi(data, index, 14)
        prev_mfi = mfi(data, index - 1, 14)
        if current_mfi > 70 and current_mfi < prev_mfi:
            score += 1

        return score
