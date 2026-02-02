"""Tests for backtesting engines and results."""

import pytest
from stockdownloader.model.price_data import PriceData
from stockdownloader.model.trade import Trade, TradeDirection, TradeStatus
from stockdownloader.backtest.backtest_engine import BacktestEngine
from stockdownloader.backtest.backtest_result import BacktestResult
from stockdownloader.backtest import backtest_report
from stockdownloader.strategy.sma_crossover import SMACrossover
from stockdownloader.strategy.rsi_strategy import RSIStrategy
from stockdownloader.strategy.macd_strategy import MACDStrategy


class TestBacktestEngine:
    def test_empty_data_raises(self):
        engine = BacktestEngine()
        with pytest.raises(ValueError):
            engine.run(SMACrossover(10, 20), [])

    def test_no_trades_on_flat_prices(self, flat_data):
        engine = BacktestEngine()
        result = engine.run(SMACrossover(10, 20), flat_data)
        assert result.total_trades == 0
        assert abs(result.final_capital - 100_000) < 1.0

    def test_equity_curve_size(self, synthetic_data):
        engine = BacktestEngine()
        result = engine.run(SMACrossover(10, 20), synthetic_data)
        assert len(result.equity_curve) == len(synthetic_data)

    def test_dates_set(self, synthetic_data):
        engine = BacktestEngine()
        result = engine.run(SMACrossover(10, 20), synthetic_data)
        assert result.start_date == synthetic_data[0].date
        assert result.end_date == synthetic_data[-1].date

    def test_commission_reduces_profit(self, synthetic_data):
        no_comm = BacktestEngine(commission=0.0).run(SMACrossover(10, 20), synthetic_data)
        with_comm = BacktestEngine(commission=10.0).run(SMACrossover(10, 20), synthetic_data)
        if no_comm.total_trades > 0:
            assert with_comm.final_capital <= no_comm.final_capital

    def test_open_position_closed_at_end(self, synthetic_data):
        engine = BacktestEngine()
        result = engine.run(SMACrossover(10, 20), synthetic_data)
        for trade in result.trades:
            assert trade.status == TradeStatus.CLOSED


class TestBacktestResult:
    def test_total_return(self):
        r = BacktestResult("Test", 100_000)
        r.final_capital = 110_000
        assert abs(r.total_return - 10.0) < 0.01

    def test_zero_trades_win_rate(self):
        r = BacktestResult("Test", 100_000)
        assert r.win_rate == 0.0

    def test_max_drawdown_empty_curve(self):
        r = BacktestResult("Test", 100_000)
        assert r.max_drawdown == 0.0

    def test_max_drawdown_calculation(self):
        r = BacktestResult("Test", 100_000)
        r.equity_curve = [100000, 110000, 90000, 95000]
        assert r.max_drawdown > 0

    def test_sharpe_insufficient_data(self):
        r = BacktestResult("Test", 100_000)
        assert r.sharpe_ratio() == 0.0

    def test_buy_and_hold_return(self):
        data = [PriceData("2023-01-01", 100, 105, 95, 100, 100, 1000000),
                PriceData("2023-12-31", 110, 115, 105, 110, 110, 1000000)]
        r = BacktestResult("Test", 100_000)
        assert abs(r.buy_and_hold_return(data) - 10.0) < 0.01

    def test_profit_factor_no_losses(self):
        r = BacktestResult("Test", 100_000)
        t = Trade(TradeDirection.LONG, "2023-01-01", 100, 10)
        t.close("2023-01-10", 110)
        r.add_trade(t)
        assert r.profit_factor == 999.99


class TestBacktestIntegration:
    def test_sma_pipeline(self, spy_data):
        engine = BacktestEngine()
        result = engine.run(SMACrossover(20, 50), spy_data)
        assert result.strategy_name == "SMA Crossover (20/50)"
        assert result.final_capital > 0
        assert len(result.equity_curve) == len(spy_data)

    def test_rsi_pipeline(self, spy_data):
        engine = BacktestEngine()
        result = engine.run(RSIStrategy(14, 30, 70), spy_data)
        assert result.total_return is not None
        assert -100 <= result.total_return <= 1000

    def test_macd_pipeline(self, spy_data):
        engine = BacktestEngine()
        result = engine.run(MACDStrategy(), spy_data)
        assert result.sharpe_ratio() is not None

    def test_all_trades_closed(self, spy_data):
        engine = BacktestEngine()
        result = engine.run(SMACrossover(20, 50), spy_data)
        for trade in result.trades:
            assert trade.status == TradeStatus.CLOSED

    def test_pl_consistency(self, spy_data):
        engine = BacktestEngine()
        result = engine.run(SMACrossover(20, 50), spy_data)
        expected_pl = result.final_capital - result.initial_capital
        assert abs(result.total_profit_loss - expected_pl) < 0.01

    def test_determinism(self, spy_data):
        engine = BacktestEngine()
        r1 = engine.run(SMACrossover(20, 50), spy_data)
        r2 = engine.run(SMACrossover(20, 50), spy_data)
        assert r1.total_return == r2.total_return
        assert r1.total_trades == r2.total_trades

    def test_strategy_comparison(self, spy_data):
        engine = BacktestEngine()
        results = [
            engine.run(SMACrossover(20, 50), spy_data),
            engine.run(RSIStrategy(), spy_data),
            engine.run(MACDStrategy(), spy_data),
        ]
        for r in results:
            assert r.final_capital > 0
            assert -100 <= r.max_drawdown <= 100


class TestBacktestReport:
    def test_print_report(self, spy_data, capsys):
        engine = BacktestEngine()
        result = engine.run(SMACrossover(20, 50), spy_data)
        backtest_report.print_report(result, spy_data)
        captured = capsys.readouterr()
        assert "BACKTEST REPORT" in captured.out

    def test_print_comparison(self, spy_data, capsys):
        engine = BacktestEngine()
        results = [
            engine.run(SMACrossover(20, 50), spy_data),
            engine.run(RSIStrategy(), spy_data),
        ]
        backtest_report.print_comparison(results, spy_data)
        captured = capsys.readouterr()
        assert "STRATEGY COMPARISON" in captured.out
