from __future__ import annotations

from collections import OrderedDict

from .option_contract import OptionContract
from .option_type import OptionType


class OptionsChain:
    """Full options chain for an underlying symbol, organized by expiration date.
    Tracks calls and puts separately with aggregate volume metrics.
    """

    def __init__(self, underlying_symbol: str):
        self.underlying_symbol = underlying_symbol
        self.underlying_price: float = 0.0
        self.expiration_dates: list[str] = []
        self.calls_by_expiration: dict[str, list[OptionContract]] = OrderedDict()
        self.puts_by_expiration: dict[str, list[OptionContract]] = OrderedDict()

    def add_expiration_date(self, date: str) -> None:
        if date not in self.expiration_dates:
            self.expiration_dates.append(date)

    def add_call(self, expiration: str, contract: OptionContract) -> None:
        self.calls_by_expiration.setdefault(expiration, []).append(contract)

    def add_put(self, expiration: str, contract: OptionContract) -> None:
        self.puts_by_expiration.setdefault(expiration, []).append(contract)

    def get_calls(self, expiration: str) -> list[OptionContract]:
        return list(self.calls_by_expiration.get(expiration, []))

    def get_puts(self, expiration: str) -> list[OptionContract]:
        return list(self.puts_by_expiration.get(expiration, []))

    def get_all_calls(self) -> list[OptionContract]:
        return [c for contracts in self.calls_by_expiration.values() for c in contracts]

    def get_all_puts(self) -> list[OptionContract]:
        return [p for contracts in self.puts_by_expiration.values() for p in contracts]

    @property
    def total_call_volume(self) -> int:
        return sum(c.volume for c in self.get_all_calls())

    @property
    def total_put_volume(self) -> int:
        return sum(p.volume for p in self.get_all_puts())

    @property
    def total_volume(self) -> int:
        return self.total_call_volume + self.total_put_volume

    @property
    def total_call_open_interest(self) -> int:
        return sum(c.open_interest for c in self.get_all_calls())

    @property
    def total_put_open_interest(self) -> int:
        return sum(p.open_interest for p in self.get_all_puts())

    @property
    def put_call_ratio(self) -> float:
        """Put/Call ratio based on volume. Values > 1 indicate bearish sentiment."""
        call_vol = self.total_call_volume
        if call_vol == 0:
            return 0.0
        return self.total_put_volume / call_vol

    def find_nearest_strike(
        self, expiration: str, option_type: OptionType, target_price: float
    ) -> OptionContract | None:
        """Finds the nearest strike to a target price for a given expiration and type."""
        contracts = self.get_calls(expiration) if option_type == OptionType.CALL else self.get_puts(expiration)
        if not contracts:
            return None
        return min(contracts, key=lambda c: abs(c.strike - target_price))

    def get_contracts_at_strike(self, expiration: str, strike: float) -> list[OptionContract]:
        """Gets contracts for a specific strike and expiration."""
        result: list[OptionContract] = []
        for c in self.get_calls(expiration):
            if c.strike == strike:
                result.append(c)
        for p in self.get_puts(expiration):
            if p.strike == strike:
                result.append(p)
        return result
