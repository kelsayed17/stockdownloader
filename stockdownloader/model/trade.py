from enum import Enum, auto


class TradeDirection(Enum):
    LONG = auto()
    SHORT = auto()


class TradeStatus(Enum):
    OPEN = auto()
    CLOSED = auto()


class Trade:
    """Tracks an individual trade position from entry to exit."""

    def __init__(self, direction: TradeDirection, entry_date: str, entry_price: float, shares: int):
        if shares <= 0:
            raise ValueError("shares must be positive")
        self.direction = direction
        self.entry_date = entry_date
        self.entry_price = entry_price
        self.shares = shares
        self.status = TradeStatus.OPEN
        self.exit_date: str | None = None
        self.exit_price: float | None = None
        self.profit_loss: float = 0.0
        self.return_pct: float = 0.0

    def close(self, exit_date: str, exit_price: float) -> None:
        if self.status == TradeStatus.CLOSED:
            raise RuntimeError("Trade is already closed")
        self.exit_date = exit_date
        self.exit_price = exit_price
        self.status = TradeStatus.CLOSED

        if self.direction == TradeDirection.LONG:
            price_diff = exit_price - self.entry_price
        else:
            price_diff = self.entry_price - exit_price

        self.profit_loss = price_diff * self.shares
        self.return_pct = (price_diff / self.entry_price) * 100 if self.entry_price != 0 else 0.0

    @property
    def is_win(self) -> bool:
        return self.profit_loss > 0

    def __repr__(self) -> str:
        exit_d = self.exit_date or "N/A"
        exit_p = f"${self.exit_price:.2f}" if self.exit_price is not None else "N/A"
        return (f"{self.direction.name} {self.status.name}: Entry {self.entry_date} @ ${self.entry_price:.2f} "
                f"-> Exit {exit_d} @ {exit_p} | P/L: ${self.profit_loss:.2f} ({self.return_pct:.2f}%)")
