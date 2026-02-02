from collections import defaultdict


class HistoricalData:
    def __init__(self, ticker: str) -> None:
        self.ticker = ticker
        self.highest_price_this_qtr: float = 0.0
        self.lowest_price_this_qtr: float = 0.0
        self.highest_price_last_qtr: float = 0.0
        self.lowest_price_last_qtr: float = 0.0
        self.historical_prices: list[str] = []
        self.patterns: dict[str, set[str]] = defaultdict(set)
        self.incomplete: bool = False
        self.error: bool = False
