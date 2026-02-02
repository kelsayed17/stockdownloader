"""Covered call options strategy.

Sells OTM calls against a long stock position to collect premium income.

OPEN when price crosses above the SMA (mild bullish / neutral trend).
CLOSE when price drops significantly below the SMA (trend reversal).
Strike is selected at a configurable percentage above the current SMA.
"""

from stockdownloader.model.price_data import PriceData
from stockdownloader.strategy.options_strategy import OptionsSignal, OptionsStrategy
from stockdownloader.util.moving_average import sma


class CoveredCallStrategy(OptionsStrategy):
    """Covered call strategy with SMA trend filter and configurable OTM strike."""

    def __init__(
        self,
        ma_period: int = 20,
        otm_pct: float = 0.03,
        days_to_expiry: int = 30,
        exit_threshold: float = 0.03,
    ) -> None:
        if ma_period <= 0:
            raise ValueError("ma_period must be positive")
        if days_to_expiry <= 0:
            raise ValueError("days_to_expiry must be positive")
        self._ma_period = ma_period
        self._otm_pct = otm_pct
        self._days_to_expiry = days_to_expiry
        self._exit_threshold = exit_threshold

    def evaluate(self, data: list[PriceData], index: int) -> OptionsSignal:
        if index < self._ma_period:
            return OptionsSignal.HOLD

        current_price = data[index].close
        ma = sma(data, index, self._ma_period)

        # OPEN: price just crossed above MA (mild bullish / neutral)
        if current_price > ma:
            prev_price = data[index - 1].close
            prev_ma = sma(data, index - 1, self._ma_period)
            if prev_price <= prev_ma:
                return OptionsSignal.OPEN

        # CLOSE: price drops significantly below MA (trend reversal)
        if ma > 0:
            pct_below_ma = (ma - current_price) / ma
            if pct_below_ma > self._exit_threshold:
                return OptionsSignal.CLOSE

        return OptionsSignal.HOLD

    def get_strike_price(self, data: list[PriceData], index: int) -> float:
        """Strike = SMA * (1 + otm_pct)."""
        ma = sma(data, index, self._ma_period)
        return ma * (1.0 + self._otm_pct)

    def get_days_to_expiry(self) -> int:
        return self._days_to_expiry

    def get_name(self) -> str:
        return f"Covered Call (MA{self._ma_period}-{self._otm_pct * 100:.0f}%)"
