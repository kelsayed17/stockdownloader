"""Tests for MultiIndicatorStrategy."""

import random
from decimal import Decimal

import pytest

from stockdownloader.model.price_data import PriceData
from stockdownloader.strategy.multi_indicator_strategy import MultiIndicatorStrategy
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
    strategy = MultiIndicatorStrategy()
    assert "Multi-Indicator" in strategy.get_name()


def test_constructor_invalid_threshold():
    with pytest.raises(ValueError):
        MultiIndicatorStrategy(0, 3)


def test_evaluate_hold_during_warmup():
    strategy = MultiIndicatorStrategy()
    data = _generate_test_data(300)
    assert strategy.evaluate(data, 50) == Signal.HOLD


def test_evaluate_handles_enough_data():
    strategy = MultiIndicatorStrategy()
    data = _generate_test_data(300)
    signal = strategy.evaluate(data, 250)
    assert signal is not None


def test_get_warmup_period_is_sufficient():
    strategy = MultiIndicatorStrategy()
    assert strategy.get_warmup_period() == 201


def test_custom_thresholds():
    strategy = MultiIndicatorStrategy(3, 3)
    assert "Buy>=3" in strategy.get_name()
    assert "Sell>=3" in strategy.get_name()
