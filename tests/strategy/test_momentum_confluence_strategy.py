"""Tests for MomentumConfluenceStrategy."""

import random
from decimal import Decimal

from stockdownloader.model.price_data import PriceData
from stockdownloader.strategy.momentum_confluence_strategy import MomentumConfluenceStrategy
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
    strategy = MomentumConfluenceStrategy()
    assert "Momentum" in strategy.get_name()
    assert "MACD" in strategy.get_name()


def test_evaluate_hold_during_warmup():
    strategy = MomentumConfluenceStrategy()
    data = _generate_test_data(300)
    assert strategy.evaluate(data, 10) == Signal.HOLD


def test_evaluate_handles_enough_data():
    strategy = MomentumConfluenceStrategy()
    data = _generate_test_data(300)
    signal = strategy.evaluate(data, 250)
    assert signal is not None


def test_get_warmup_period_covers_all_indicators():
    strategy = MomentumConfluenceStrategy()
    assert strategy.get_warmup_period() >= 200, "Warmup should cover EMA(200) requirement"
