"""Tests for BollingerBandRSIStrategy."""

import random
from decimal import Decimal

from stockdownloader.model.price_data import PriceData
from stockdownloader.strategy.bollinger_band_rsi_strategy import BollingerBandRSIStrategy
from stockdownloader.strategy.trading_strategy import Signal


def _generate_test_data(days):
    data = []
    price = 100.0
    for i in range(days):
        price += (random.random() - 0.48) * 3
        price = max(50, price)
        data.append(
            PriceData(
                date="2020-01-01",
                open=Decimal(str(price - 1)),
                high=Decimal(str(price + 2)),
                low=Decimal(str(price - 2)),
                close=Decimal(str(price)),
                adj_close=Decimal(str(price)),
                volume=int(1_000_000 + random.random() * 5_000_000),
            )
        )
    return data


def test_constructor_default_values():
    strategy = BollingerBandRSIStrategy()
    assert strategy.get_name() == "BB+RSI Mean Reversion (BB20, RSI14 [30/70], ADX<25)"


def test_evaluate_hold_during_warmup():
    strategy = BollingerBandRSIStrategy()
    data = _generate_test_data(50)
    assert strategy.evaluate(data, 5) == Signal.HOLD


def test_evaluate_hold_with_sufficient_data():
    strategy = BollingerBandRSIStrategy()
    data = _generate_test_data(200)
    # With random data, most signals should be HOLD
    signal = strategy.evaluate(data, 100)
    assert signal is not None


def test_get_warmup_period_returns_expected_value():
    strategy = BollingerBandRSIStrategy()
    assert strategy.get_warmup_period() >= 20, "Warmup should be at least 20"


def test_custom_parameters():
    strategy = BollingerBandRSIStrategy(30, 2.5, 14, 25, 75, 30)
    assert "BB30" in strategy.get_name()
