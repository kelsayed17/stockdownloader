"""Tests for model classes."""

import pytest
from stockdownloader.model.price_data import PriceData
from stockdownloader.model.trade import Trade, TradeDirection, TradeStatus
from stockdownloader.model.option_type import OptionType
from stockdownloader.model.option_contract import OptionContract
from stockdownloader.model.options_trade import OptionsTrade, OptionsDirection, OptionsTradeStatus
from stockdownloader.model.options_chain import OptionsChain
from stockdownloader.model.pattern_result import PatternResult
from stockdownloader.model.financial_data import FinancialData
from stockdownloader.model.historical_data import HistoricalData
from stockdownloader.model.quote_data import QuoteData
from stockdownloader.model.unified_market_data import UnifiedMarketData


# --- PriceData tests ---
class TestPriceData:
    def test_create_valid(self):
        pd = PriceData("2023-01-03", 100.0, 105.0, 95.0, 102.0, 101.5, 1000000)
        assert pd.date == "2023-01-03"
        assert pd.open == 100.0
        assert pd.high == 105.0
        assert pd.low == 95.0
        assert pd.close == 102.0
        assert pd.adj_close == 101.5
        assert pd.volume == 1000000

    def test_frozen(self):
        pd = PriceData("2023-01-03", 100.0, 105.0, 95.0, 102.0, 101.5, 1000000)
        with pytest.raises(AttributeError):
            pd.close = 200.0

    def test_equality(self):
        pd1 = PriceData("2023-01-03", 100.0, 105.0, 95.0, 102.0, 101.5, 1000000)
        pd2 = PriceData("2023-01-03", 100.0, 105.0, 95.0, 102.0, 101.5, 1000000)
        assert pd1 == pd2


# --- Trade tests ---
class TestTrade:
    def test_create_long_trade(self):
        t = Trade(TradeDirection.LONG, "2023-01-03", 100.0, 10)
        assert t.direction == TradeDirection.LONG
        assert t.status == TradeStatus.OPEN
        assert t.profit_loss == 0.0

    def test_close_long_with_profit(self):
        t = Trade(TradeDirection.LONG, "2023-01-03", 100.0, 10)
        t.close("2023-01-10", 110.0)
        assert t.status == TradeStatus.CLOSED
        assert t.profit_loss == 100.0  # (110-100)*10
        assert t.is_win

    def test_close_long_with_loss(self):
        t = Trade(TradeDirection.LONG, "2023-01-03", 100.0, 10)
        t.close("2023-01-10", 90.0)
        assert t.profit_loss == -100.0
        assert not t.is_win

    def test_close_short_with_profit(self):
        t = Trade(TradeDirection.SHORT, "2023-01-03", 100.0, 10)
        t.close("2023-01-10", 90.0)
        assert t.profit_loss == 100.0
        assert t.is_win

    def test_double_close_raises(self):
        t = Trade(TradeDirection.LONG, "2023-01-03", 100.0, 10)
        t.close("2023-01-10", 110.0)
        with pytest.raises(RuntimeError):
            t.close("2023-01-11", 115.0)

    def test_zero_shares_raises(self):
        with pytest.raises(ValueError):
            Trade(TradeDirection.LONG, "2023-01-03", 100.0, 0)

    def test_negative_shares_raises(self):
        with pytest.raises(ValueError):
            Trade(TradeDirection.LONG, "2023-01-03", 100.0, -5)

    def test_return_pct(self):
        t = Trade(TradeDirection.LONG, "2023-01-03", 100.0, 10)
        t.close("2023-01-10", 110.0)
        assert abs(t.return_pct - 10.0) < 0.01


# --- OptionContract tests ---
class TestOptionContract:
    def test_create_valid(self):
        oc = OptionContract("SPY230120C00400000", OptionType.CALL, 400.0, "2023-01-20",
                           5.0, 4.8, 5.2, 1000, 5000, 0.25, 0.5, 0.03, -0.05, 0.1, True)
        assert oc.strike == 400.0
        assert oc.type == OptionType.CALL

    def test_mid_price(self):
        oc = OptionContract("SPY", OptionType.CALL, 400.0, "2023-01-20",
                           5.0, 4.0, 6.0, 100, 500, 0.25, 0.5, 0.03, -0.05, 0.1, True)
        assert oc.mid_price() == 5.0

    def test_spread(self):
        oc = OptionContract("SPY", OptionType.CALL, 400.0, "2023-01-20",
                           5.0, 4.0, 6.0, 100, 500, 0.25, 0.5, 0.03, -0.05, 0.1, True)
        assert oc.spread() == 2.0

    def test_notional_value(self):
        oc = OptionContract("SPY", OptionType.CALL, 400.0, "2023-01-20",
                           5.0, 4.0, 6.0, 100, 500, 0.25, 0.5, 0.03, -0.05, 0.1, True)
        assert oc.notional_value() == 500.0

    def test_negative_volume_raises(self):
        with pytest.raises(ValueError):
            OptionContract("SPY", OptionType.CALL, 400.0, "2023-01-20",
                          5.0, 4.0, 6.0, -1, 500, 0.25, 0.5, 0.03, -0.05, 0.1, True)


# --- OptionsTrade tests ---
class TestOptionsTrade:
    def test_new_trade_is_open(self):
        t = OptionsTrade(OptionType.CALL, OptionsDirection.BUY, 400.0, "2023-02-17",
                        "2023-01-03", 5.0, 2, 1000)
        assert t.status == OptionsTradeStatus.OPEN
        assert t.profit_loss == 0.0

    def test_close_long_call_with_profit(self):
        t = OptionsTrade(OptionType.CALL, OptionsDirection.BUY, 400.0, "2023-02-17",
                        "2023-01-03", 5.0, 2, 1000)
        t.close("2023-01-10", 8.0)
        assert t.status == OptionsTradeStatus.CLOSED
        assert t.profit_loss == 600.0  # (8-5) * 2 * 100
        assert t.is_win

    def test_close_short_put_with_profit(self):
        t = OptionsTrade(OptionType.PUT, OptionsDirection.SELL, 400.0, "2023-02-17",
                        "2023-01-03", 5.0, 1, 500)
        t.close("2023-01-10", 3.0)
        assert t.profit_loss == 200.0  # (5-3) * 1 * 100
        assert t.is_win

    def test_expire(self):
        t = OptionsTrade(OptionType.CALL, OptionsDirection.BUY, 400.0, "2023-02-17",
                        "2023-01-03", 5.0, 1, 1000)
        t.expire("2023-02-17", 0.0)
        assert t.status == OptionsTradeStatus.EXPIRED
        assert t.profit_loss == -500.0

    def test_cannot_close_already_closed(self):
        t = OptionsTrade(OptionType.CALL, OptionsDirection.BUY, 400.0, "2023-02-17",
                        "2023-01-03", 5.0, 1, 1000)
        t.close("2023-01-10", 8.0)
        with pytest.raises(RuntimeError):
            t.close("2023-01-11", 9.0)

    def test_total_entry_cost(self):
        t = OptionsTrade(OptionType.CALL, OptionsDirection.BUY, 400.0, "2023-02-17",
                        "2023-01-03", 5.0, 3, 1000)
        assert t.total_entry_cost() == 1500.0  # 5 * 3 * 100

    def test_zero_contracts_raises(self):
        with pytest.raises(ValueError):
            OptionsTrade(OptionType.CALL, OptionsDirection.BUY, 400.0, "2023-02-17",
                        "2023-01-03", 5.0, 0, 1000)


# --- OptionsChain tests ---
class TestOptionsChain:
    def _make_chain(self):
        chain = OptionsChain("SPY")
        chain.underlying_price = 450.0
        chain.add_expiration_date("2023-01-20")
        chain.add_expiration_date("2023-02-17")
        chain.add_call("2023-01-20", OptionContract("SPY230120C00450000", OptionType.CALL, 450.0,
                       "2023-01-20", 5.0, 4.8, 5.2, 1000, 5000, 0.25, 0.5, 0.03, -0.05, 0.1, True))
        chain.add_call("2023-01-20", OptionContract("SPY230120C00460000", OptionType.CALL, 460.0,
                       "2023-01-20", 2.0, 1.8, 2.2, 500, 3000, 0.20, 0.3, 0.02, -0.03, 0.08, False))
        chain.add_put("2023-01-20", OptionContract("SPY230120P00440000", OptionType.PUT, 440.0,
                      "2023-01-20", 4.0, 3.8, 4.2, 800, 4000, 0.22, -0.4, 0.03, -0.04, 0.09, False))
        return chain

    def test_basic_properties(self):
        chain = self._make_chain()
        assert chain.underlying_symbol == "SPY"
        assert chain.underlying_price == 450.0
        assert len(chain.expiration_dates) == 2

    def test_total_call_volume(self):
        chain = self._make_chain()
        assert chain.total_call_volume == 1500

    def test_total_put_volume(self):
        chain = self._make_chain()
        assert chain.total_put_volume == 800

    def test_total_volume(self):
        chain = self._make_chain()
        assert chain.total_volume == 2300

    def test_put_call_ratio(self):
        chain = self._make_chain()
        ratio = chain.put_call_ratio
        assert abs(ratio - 800 / 1500) < 0.01

    def test_find_nearest_strike(self):
        chain = self._make_chain()
        contract = chain.find_nearest_strike("2023-01-20", OptionType.CALL, 455.0)
        assert contract is not None
        assert contract.strike == 450.0


# --- PatternResult tests ---
class TestPatternResult:
    def test_sorting(self):
        pr1 = PatternResult("UUU", "DDD", "DDD", 0.5, 0.3, 0.2, 0.6, set(), set(), set())
        pr2 = PatternResult("UUD", "DDU", "DDU", 0.3, 0.2, 0.1, 0.5, set(), set(), set())
        assert pr1 < pr2  # higher freq sorts first


# --- FinancialData tests ---
class TestFinancialData:
    def test_compute_revenue_per_share(self):
        fd = FinancialData()
        for i in range(6):
            fd.revenue[i] = 1_000_000
            fd.diluted_shares[i] = 1_000
        fd.compute_revenue_per_share()
        assert fd.revenue_per_share[0] == 1000.0


# --- UnifiedMarketData tests ---
class TestUnifiedMarketData:
    def test_default_values(self):
        umd = UnifiedMarketData("SPY")
        assert umd.symbol == "SPY"
        assert umd.current_price == 0.0
        assert umd.equity_volume == 0.0
        assert umd.options_volume == 0
        assert not umd.is_complete

    def test_current_price_from_quote(self):
        umd = UnifiedMarketData("SPY")
        qd = QuoteData()
        qd.last_trade_price_only = 450.0
        umd.quote = qd
        assert umd.current_price == 450.0

    def test_completeness(self):
        umd = UnifiedMarketData("SPY")
        assert not umd.has_quote_data
        assert not umd.has_price_history
        umd.price_history = [PriceData("2023-01-03", 100, 105, 95, 102, 101.5, 1000000)]
        assert umd.has_price_history
