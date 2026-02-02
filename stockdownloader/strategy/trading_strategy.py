"""Interface for equity trading strategies."""

from abc import ABC, abstractmethod
from enum import Enum, auto

from stockdownloader.model.price_data import PriceData


class Signal(Enum):
    BUY = auto()
    SELL = auto()
    HOLD = auto()


class TradingStrategy(ABC):
    """Base class for all equity trading strategies.

    Each implementation evaluates price data and produces BUY/SELL/HOLD signals.
    """

    @abstractmethod
    def evaluate(self, data: list[PriceData], index: int) -> Signal:
        """Evaluate the strategy at the given index and return a signal."""
        ...

    @abstractmethod
    def get_name(self) -> str:
        """Return the strategy name."""
        ...
