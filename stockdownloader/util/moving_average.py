"""Shared calculations for Simple Moving Average (SMA) and Exponential Moving Average (EMA)."""

from __future__ import annotations
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from stockdownloader.model.price_data import PriceData


def sma(data: list[PriceData], end_index: int, period: int) -> float:
    """Calculate Simple Moving Average."""
    total = sum(data[i].close for i in range(end_index - period + 1, end_index + 1))
    return total / period


def ema(data: list[PriceData], end_index: int, period: int) -> float:
    """Calculate Exponential Moving Average."""
    multiplier = 2.0 / (period + 1)
    one_minus = 1.0 - multiplier

    start_index = max(0, end_index - period - period)
    seed_end = min(start_index + period, end_index + 1)

    total = sum(data[i].close for i in range(start_index, seed_end))
    ema_val = total / period

    for i in range(start_index + period, end_index + 1):
        ema_val = data[i].close * multiplier + ema_val * one_minus

    return ema_val
