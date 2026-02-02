"""Class wrapper around moving_average module functions for IndicatorValues compatibility."""

from __future__ import annotations

from typing import TYPE_CHECKING

from . import moving_average as _ma

if TYPE_CHECKING:
    from stockdownloader.model.price_data import PriceData


class MovingAverageCalculator:
    """Static-method facade over the moving_average module."""

    @staticmethod
    def sma(data: list[PriceData], end_index: int, period: int) -> float:
        return _ma.sma(data, end_index, period)

    @staticmethod
    def ema(data: list[PriceData], end_index: int, period: int) -> float:
        return _ma.ema(data, end_index, period)
