from dataclasses import dataclass


@dataclass(frozen=True)
class PriceData:
    """Immutable OHLCV price bar."""
    date: str
    open: float
    high: float
    low: float
    close: float
    adj_close: float
    volume: int
