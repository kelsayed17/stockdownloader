from dataclasses import dataclass

from .option_type import OptionType


@dataclass(frozen=True)
class OptionContract:
    """Immutable representation of a single options contract with full greeks and volume data."""
    contract_symbol: str
    type: OptionType
    strike: float
    expiration_date: str
    last_price: float
    bid: float
    ask: float
    volume: int
    open_interest: int
    implied_volatility: float
    delta: float
    gamma: float
    theta: float
    vega: float
    in_the_money: bool

    def __post_init__(self) -> None:
        if self.volume < 0:
            raise ValueError("volume must not be negative")
        if self.open_interest < 0:
            raise ValueError("open_interest must not be negative")

    def mid_price(self) -> float:
        """Returns the mid-price between bid and ask."""
        return (self.bid + self.ask) / 2.0

    def spread(self) -> float:
        """Returns the bid-ask spread."""
        return self.ask - self.bid

    def notional_value(self) -> float:
        """Returns the notional value per contract (premium * 100 shares)."""
        return self.last_price * 100

    def __str__(self) -> str:
        return (f"{self.contract_symbol} {self.type.name} ${self.strike:.2f} "
                f"exp:{self.expiration_date} last:${self.last_price:.2f} "
                f"bid:${self.bid:.2f} ask:${self.ask:.2f} vol:{self.volume} "
                f"OI:{self.open_interest} IV:{self.implied_volatility * 100:.2f}%")
