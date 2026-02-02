from .financial_data import FinancialData
from .historical_data import HistoricalData
from .options_chain import OptionsChain
from .price_data import PriceData
from .quote_data import QuoteData


class UnifiedMarketData:
    def __init__(self, symbol: str) -> None:
        self.symbol = symbol
        self.latest_price: PriceData | None = None
        self.price_history: list[PriceData] = []
        self.quote: QuoteData | None = None
        self.historical: HistoricalData | None = None
        self.financials: FinancialData | None = None
        self.options_chain: OptionsChain | None = None

    @property
    def equity_volume(self) -> float:
        return self.quote.volume if self.quote else 0.0

    @property
    def options_volume(self) -> int:
        return self.options_chain.total_volume if self.options_chain else 0

    @property
    def call_volume(self) -> int:
        return self.options_chain.total_call_volume if self.options_chain else 0

    @property
    def put_volume(self) -> int:
        return self.options_chain.total_put_volume if self.options_chain else 0

    @property
    def total_combined_volume(self) -> float:
        return self.equity_volume + self.options_volume

    def get_average_daily_volume(self, days: int) -> float:
        if not self.price_history:
            return 0.0
        limit = min(days, len(self.price_history))
        total_vol = sum(p.volume for p in self.price_history[-limit:])
        return round(total_vol / limit)

    @property
    def total_open_interest(self) -> int:
        if not self.options_chain:
            return 0
        return self.options_chain.total_call_open_interest + self.options_chain.total_put_open_interest

    @property
    def put_call_ratio(self) -> float:
        return self.options_chain.put_call_ratio if self.options_chain else 0.0

    @property
    def current_price(self) -> float:
        if self.quote and self.quote.last_trade_price_only > 0:
            return self.quote.last_trade_price_only
        if self.latest_price:
            return self.latest_price.close
        return 0.0

    @property
    def market_cap(self) -> float:
        return self.quote.market_capitalization if self.quote else 0.0

    @property
    def has_quote_data(self) -> bool:
        return self.quote is not None and not self.quote.error

    @property
    def has_price_history(self) -> bool:
        return bool(self.price_history)

    @property
    def has_financial_data(self) -> bool:
        return self.financials is not None and not self.financials.error

    @property
    def has_options_chain(self) -> bool:
        return self.options_chain is not None and bool(self.options_chain.expiration_dates)

    @property
    def has_historical_data(self) -> bool:
        return self.historical is not None and not self.historical.error

    @property
    def is_complete(self) -> bool:
        return all([
            self.has_quote_data,
            self.has_price_history,
            self.has_financial_data,
            self.has_options_chain,
            self.has_historical_data,
        ])

    def __repr__(self) -> str:
        return (f"UnifiedMarketData[{self.symbol}] price=${self.current_price} "
                f"eqVol={self.equity_volume} optVol={self.options_volume} "
                f"OI={self.total_open_interest} P/C={self.put_call_ratio:.4f}")
