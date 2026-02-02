"""Tests for trading strategies."""

import pytest
from stockdownloader.strategy.trading_strategy import Signal
from stockdownloader.strategy.options_strategy import OptionsSignal
from stockdownloader.strategy.sma_crossover import SMACrossover
from stockdownloader.strategy.rsi_strategy import RSIStrategy
from stockdownloader.strategy.macd_strategy import MACDStrategy
from stockdownloader.strategy.bollinger_band_rsi import BollingerBandRSI
from stockdownloader.strategy.breakout import BreakoutStrategy
from stockdownloader.strategy.momentum_confluence import MomentumConfluence
from stockdownloader.strategy.multi_indicator import MultiIndicatorStrategy
from stockdownloader.strategy.covered_call import CoveredCallStrategy
from stockdownloader.strategy.protective_put import ProtectivePutStrategy


class TestSMACrossover:
    def test_construction(self):
        s = SMACrossover(10, 20)
        assert "SMA Crossover" in s.get_name()
        assert "10" in s.get_name()
        assert "20" in s.get_name()

    def test_hold_during_warmup(self, synthetic_data):
        s = SMACrossover(10, 20)
        for i in range(19):
            assert s.evaluate(synthetic_data, i) == Signal.HOLD

    def test_generates_signals_after_warmup(self, synthetic_data):
        s = SMACrossover(10, 20)
        signals = [s.evaluate(synthetic_data, i) for i in range(20, len(synthetic_data))]
        non_hold = [sig for sig in signals if sig != Signal.HOLD]
        assert len(non_hold) > 0

    def test_hold_on_flat_prices(self, flat_data):
        s = SMACrossover(10, 20)
        signals = [s.evaluate(flat_data, i) for i in range(20, len(flat_data))]
        non_hold = [sig for sig in signals if sig != Signal.HOLD]
        assert len(non_hold) == 0


class TestRSIStrategy:
    def test_construction(self):
        s = RSIStrategy()
        assert "RSI" in s.get_name()

    def test_hold_during_warmup(self, synthetic_data):
        s = RSIStrategy()
        for i in range(15):
            assert s.evaluate(synthetic_data, i) == Signal.HOLD

    def test_generates_signals(self, synthetic_data):
        s = RSIStrategy(14, 30.0, 70.0)
        signals = [s.evaluate(synthetic_data, i) for i in range(16, len(synthetic_data))]
        assert all(sig in (Signal.BUY, Signal.SELL, Signal.HOLD) for sig in signals)


class TestMACDStrategy:
    def test_construction(self):
        s = MACDStrategy()
        assert "MACD" in s.get_name()

    def test_hold_during_warmup(self, synthetic_data):
        s = MACDStrategy()
        for i in range(35):
            assert s.evaluate(synthetic_data, i) == Signal.HOLD

    def test_generates_signals(self, synthetic_data):
        s = MACDStrategy()
        signals = [s.evaluate(synthetic_data, i) for i in range(36, len(synthetic_data))]
        non_hold = [sig for sig in signals if sig != Signal.HOLD]
        assert len(non_hold) > 0


class TestBollingerBandRSI:
    def test_construction(self):
        s = BollingerBandRSI()
        assert "Bollinger" in s.get_name() or "BB" in s.get_name()

    def test_hold_during_warmup(self, synthetic_data):
        s = BollingerBandRSI()
        assert s.evaluate(synthetic_data, 5) == Signal.HOLD

    def test_processes_data(self, synthetic_data):
        s = BollingerBandRSI()
        signal = s.evaluate(synthetic_data, 200)
        assert signal in (Signal.BUY, Signal.SELL, Signal.HOLD)


class TestBreakoutStrategy:
    def test_construction(self):
        s = BreakoutStrategy()
        assert "Breakout" in s.get_name()

    def test_processes_data(self, synthetic_data):
        s = BreakoutStrategy()
        signal = s.evaluate(synthetic_data, 200)
        assert signal in (Signal.BUY, Signal.SELL, Signal.HOLD)


class TestMomentumConfluence:
    def test_construction(self):
        s = MomentumConfluence()
        assert "Momentum" in s.get_name()

    def test_processes_data(self, synthetic_data):
        s = MomentumConfluence()
        signal = s.evaluate(synthetic_data, 200)
        assert signal in (Signal.BUY, Signal.SELL, Signal.HOLD)


class TestMultiIndicator:
    def test_construction(self):
        s = MultiIndicatorStrategy()
        assert "Multi" in s.get_name()

    def test_processes_data(self, synthetic_data):
        s = MultiIndicatorStrategy()
        signal = s.evaluate(synthetic_data, 200)
        assert signal in (Signal.BUY, Signal.SELL, Signal.HOLD)


class TestCoveredCall:
    def test_construction(self):
        s = CoveredCallStrategy()
        assert "Covered Call" in s.get_name()

    def test_strike_above_price(self, synthetic_data):
        s = CoveredCallStrategy(20, 0.03)
        strike = s.get_strike_price(synthetic_data, 200)
        assert strike > 0

    def test_days_to_expiry(self):
        s = CoveredCallStrategy()
        assert s.get_days_to_expiry() == 30

    def test_generates_signals(self, synthetic_data):
        s = CoveredCallStrategy()
        signal = s.evaluate(synthetic_data, 200)
        assert signal in (OptionsSignal.OPEN, OptionsSignal.CLOSE, OptionsSignal.HOLD)


class TestProtectivePut:
    def test_construction(self):
        s = ProtectivePutStrategy()
        assert "Protective Put" in s.get_name()

    def test_strike_below_price(self, synthetic_data):
        s = ProtectivePutStrategy(20, 0.05)
        strike = s.get_strike_price(synthetic_data, 200)
        assert strike > 0

    def test_days_to_expiry(self):
        s = ProtectivePutStrategy()
        assert s.get_days_to_expiry() == 30
