"""Interface for options trading strategies."""

from abc import ABC, abstractmethod
from enum import Enum, auto

from stockdownloader.model.price_data import PriceData


class OptionsSignal(Enum):
    OPEN = auto()
    CLOSE = auto()
    HOLD = auto()


class OptionsStrategy(ABC):
    """Base class for options trading strategies.

    Operates on underlying price data and generates signals for
    opening/closing options positions at specific strikes and expirations.
    """

    @abstractmethod
    def evaluate(self, data: list[PriceData], index: int) -> OptionsSignal:
        """Evaluate the strategy and return a signal."""
        ...

    @abstractmethod
    def get_name(self) -> str:
        """Return the strategy name."""
        ...

    @abstractmethod
    def get_strike_price(self, data: list[PriceData], index: int) -> float:
        """Calculate the target strike price based on current market conditions."""
        ...

    @abstractmethod
    def get_days_to_expiry(self) -> int:
        """Get the target days to expiration for new positions."""
        ...
