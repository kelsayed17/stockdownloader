"""Tests for analysis classes."""

import pytest
from stockdownloader.model.price_data import PriceData
from stockdownloader.model.alert_result import AlertDirection
from stockdownloader.analysis.signal_generator import generate_alert
from stockdownloader.analysis.pattern_analyzer import analyze_patterns
from stockdownloader.analysis.formula_calculator import (
    compound_annual_growth_rate, risk_adjusted_return, kelly_criterion, expected_value
)


class TestSignalGenerator:
    def test_generate_alert_sufficient_data(self, synthetic_data):
        alert = generate_alert(synthetic_data, 250, "TEST")
        assert alert.symbol == "TEST"
        assert alert.current_price > 0
        assert alert.direction in AlertDirection

    def test_generate_alert_insufficient_data(self, synthetic_data):
        alert = generate_alert(synthetic_data, 50, "TEST")
        assert alert.direction == AlertDirection.NEUTRAL

    def test_alert_has_indicators(self, synthetic_data):
        alert = generate_alert(synthetic_data, 250, "TEST")
        total = len(alert.bullish_indicators) + len(alert.bearish_indicators)
        assert total > 0

    def test_alert_string_no_error(self, synthetic_data):
        alert = generate_alert(synthetic_data, 250, "TEST")
        text = str(alert)
        assert "TRADING ALERT" in text

    def test_signal_strength(self, synthetic_data):
        alert = generate_alert(synthetic_data, 250, "TEST")
        assert "%" in alert.signal_strength


class TestPatternAnalyzer:
    def test_analyze_empty(self):
        results = analyze_patterns([])
        assert results == []

    def test_analyze_insufficient_data(self, flat_data):
        results = analyze_patterns(flat_data[:3])
        assert results == []

    def test_analyze_patterns(self, synthetic_data):
        results = analyze_patterns(synthetic_data, pattern_length=3)
        assert len(results) > 0
        # Sorted by frequency descending
        for i in range(len(results) - 1):
            assert results[i].pattern_freq >= results[i + 1].pattern_freq

    def test_accuracy_range(self, synthetic_data):
        results = analyze_patterns(synthetic_data, pattern_length=3)
        for r in results:
            assert 0 <= r.accuracy <= 1


class TestFormulaCalculator:
    def test_cagr(self):
        cagr = compound_annual_growth_rate(100, 200, 5)
        assert abs(cagr - 14.87) < 0.1

    def test_cagr_zero_begin(self):
        assert compound_annual_growth_rate(0, 200, 5) == 0.0

    def test_risk_adjusted_return(self):
        rar = risk_adjusted_return(20.0, 10.0)
        assert abs(rar - 2.0) < 0.01

    def test_kelly_criterion(self):
        k = kelly_criterion(0.6, 100, -50)
        assert k > 0

    def test_expected_value(self):
        ev = expected_value(0.6, 100, -50)
        assert ev == 40.0
