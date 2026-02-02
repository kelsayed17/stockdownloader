"""Protective put options strategy.

Buys OTM puts to hedge a long stock position, providing downside protection
at the cost of the put premium.

OPEN when price crosses below the SMA or momentum turns negative.
CLOSE when price recovers sufficiently above the SMA (protection no longer needed).
Strike is selected at a configurable percentage below the current SMA.
"""

from stockdownloader.model.price_data import PriceData
from stockdownloader.strategy.options_strategy import OptionsSignal, OptionsStrategy
from stockdownloader.util.moving_average import sma


class ProtectivePutStrategy(OptionsStrategy):
    """Protective put strategy with SMA trend filter and momentum lookback."""

    def __init__(
        self,
        ma_period: int = 20,
        otm_pct: float = 0.05,
        days_to_expiry: int = 30,
        momentum_lookback: int = 5,
    ) -> None:
        if ma_period <= 0:
            raise ValueError("ma_period must be positive")
        if days_to_expiry <= 0:
            raise ValueError("days_to_expiry must be positive")
        if momentum_lookback <= 0:
            raise ValueError("momentum_lookback must be positive")
        self._ma_period = ma_period
        self._otm_pct = otm_pct
        self._days_to_expiry = days_to_expiry
        self._momentum_lookback = momentum_lookback

    def evaluate(self, data: list[PriceData], index: int) -> OptionsSignal:
        warmup = max(self._ma_period, self._momentum_lookback)
        if index < warmup:
            return OptionsSignal.HOLD

        current_price = data[index].close
        ma = sma(data, index, self._ma_period)

        # Momentum: percentage change over lookback period
        lookback_price = data[index - self._momentum_lookback].close
        momentum = 0.0
        if lookback_price > 0:
            momentum = (current_price - lookback_price) / lookback_price

        price_below_ma = current_price < ma
        prev_above_ma = data[index - 1].close >= sma(data, index - 1, self._ma_period)
        negative_momentum = momentum < -0.02

        # OPEN: price crosses below MA
        if price_below_ma and prev_above_ma:
            return OptionsSignal.OPEN
        # OPEN: negative momentum while below MA
        if negative_momentum and price_below_ma:
            return OptionsSignal.OPEN

        # CLOSE: price recovers above MA by a meaningful margin
        if current_price > ma and ma > 0:
            pct_above_ma = (current_price - ma) / ma
            if pct_above_ma > 0.02:
                return OptionsSignal.CLOSE

        return OptionsSignal.HOLD

    def get_strike_price(self, data: list[PriceData], index: int) -> float:
        """Strike = SMA * (1 - otm_pct)."""
        ma = sma(data, index, self._ma_period)
        return ma * (1.0 - self._otm_pct)

    def get_days_to_expiry(self) -> int:
        return self._days_to_expiry

    def get_name(self) -> str:
        return f"Protective Put (MA{self._ma_period}-{self._otm_pct * 100:.0f}%)"
