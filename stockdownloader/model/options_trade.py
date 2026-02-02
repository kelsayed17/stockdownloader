from enum import Enum, auto

from .option_type import OptionType


class OptionsDirection(Enum):
    BUY = auto()
    SELL = auto()


class OptionsTradeStatus(Enum):
    OPEN = auto()
    CLOSED = auto()
    EXPIRED = auto()


class OptionsTrade:
    """Tracks an individual options trade from entry to exit.
    Each contract represents 100 shares of the underlying.
    """

    CONTRACT_MULTIPLIER: int = 100

    def __init__(
        self,
        option_type: OptionType,
        direction: OptionsDirection,
        strike: float,
        expiration_date: str,
        entry_date: str,
        entry_premium: float,
        contracts: int,
        entry_volume: int,
    ):
        if contracts <= 0:
            raise ValueError("contracts must be positive")
        self.option_type = option_type
        self.direction = direction
        self.strike = strike
        self.expiration_date = expiration_date
        self.entry_date = entry_date
        self.entry_premium = entry_premium
        self.contracts = contracts
        self.entry_volume = entry_volume
        self.status = OptionsTradeStatus.OPEN
        self.exit_date: str | None = None
        self.exit_premium: float | None = None
        self.profit_loss: float = 0.0
        self.return_pct: float = 0.0

    def close(self, exit_date: str, exit_premium: float) -> None:
        """Close the trade at the given premium."""
        if self.status != OptionsTradeStatus.OPEN:
            raise RuntimeError(f"Trade is not open, current status: {self.status.name}")
        self.exit_date = exit_date
        self.exit_premium = exit_premium
        self.status = OptionsTradeStatus.CLOSED
        self._calculate_profit_loss()

    def expire(self, expiry_date: str, settlement_premium: float) -> None:
        """Mark the trade as expired (option expired worthless or exercised)."""
        if self.status != OptionsTradeStatus.OPEN:
            raise RuntimeError(f"Trade is not open, current status: {self.status.name}")
        self.exit_date = expiry_date
        self.exit_premium = settlement_premium
        self.status = OptionsTradeStatus.EXPIRED
        self._calculate_profit_loss()

    def _calculate_profit_loss(self) -> None:
        assert self.exit_premium is not None
        if self.direction == OptionsDirection.BUY:
            premium_diff = self.exit_premium - self.entry_premium
        else:
            premium_diff = self.entry_premium - self.exit_premium

        self.profit_loss = premium_diff * self.contracts * self.CONTRACT_MULTIPLIER

        total_cost = self.entry_premium * self.contracts * self.CONTRACT_MULTIPLIER
        if total_cost != 0:
            self.return_pct = (self.profit_loss / total_cost) * 100
        else:
            self.return_pct = 0.0

    @property
    def is_win(self) -> bool:
        return self.profit_loss > 0

    def total_entry_cost(self) -> float:
        """Total premium paid/received at entry (contracts * 100 * premium)."""
        return self.entry_premium * self.contracts * self.CONTRACT_MULTIPLIER

    def __repr__(self) -> str:
        exit_d = self.exit_date or "N/A"
        exit_p = f"${self.exit_premium:.2f}" if self.exit_premium is not None else "N/A"
        return (
            f"{self.direction.name} {self.option_type.name} {self.status.name} "
            f"${self.strike:.2f} exp:{self.expiration_date} | "
            f"Entry:{self.entry_date} @${self.entry_premium:.2f} -> "
            f"Exit:{exit_d} @{exit_p} | P/L:${self.profit_loss:.2f} "
            f"({self.return_pct:.2f}%) vol:{self.entry_volume}"
        )
