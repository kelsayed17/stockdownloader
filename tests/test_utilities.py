"""Tests for utility classes."""

import pytest
import math
from stockdownloader.model.price_data import PriceData
from stockdownloader.util import moving_average, technical_indicators as ti, black_scholes
from stockdownloader.util.decimal_math import safe_divide, percent_change, average
from stockdownloader.util.retry_executor import retry_with_backoff
from stockdownloader.model.option_type import OptionType


class TestMovingAverage:
    def test_sma_uniform_prices(self, flat_data):
        result = moving_average.sma(flat_data, 49, 50)
        assert abs(result - 100.0) < 0.01

    def test_sma_period_1(self, synthetic_data):
        result = moving_average.sma(synthetic_data, 50, 1)
        assert abs(result - synthetic_data[50].close) < 0.01

    def test_ema_converges_on_flat(self, flat_data):
        result = moving_average.ema(flat_data, 49, 20)
        assert abs(result - 100.0) < 1.0

    def test_ema_more_responsive(self, synthetic_data):
        sma_val = moving_average.sma(synthetic_data, 299, 20)
        ema_val = moving_average.ema(synthetic_data, 299, 20)
        # Both should be close to the current price for trending data
        assert abs(sma_val - synthetic_data[299].close) < 10
        assert abs(ema_val - synthetic_data[299].close) < 10


class TestTechnicalIndicators:
    def test_rsi_range(self, synthetic_data):
        rsi = ti.rsi(synthetic_data, 200, 14)
        assert 0 <= rsi <= 100

    def test_rsi_default_on_insufficient_data(self, synthetic_data):
        rsi = ti.rsi(synthetic_data, 5, 14)
        assert rsi == 50  # default when insufficient data

    def test_bollinger_bands(self, synthetic_data):
        bb = ti.bollinger_bands(synthetic_data, 200)
        assert bb.upper > bb.middle > bb.lower
        assert bb.width > 0

    def test_bollinger_percent_b(self, synthetic_data):
        pctb = ti.bollinger_percent_b(synthetic_data, 200)
        assert -1 < pctb < 2  # reasonable range

    def test_stochastic(self, synthetic_data):
        stoch = ti.stochastic(synthetic_data, 200)
        assert 0 <= stoch.percent_k <= 100
        assert 0 <= stoch.percent_d <= 100

    def test_atr_positive(self, synthetic_data):
        atr_val = ti.atr(synthetic_data, 200, 14)
        assert atr_val >= 0

    def test_obv(self, synthetic_data):
        obv_val = ti.obv(synthetic_data, 200)
        # OBV can be positive or negative
        assert isinstance(obv_val, float)

    def test_adx(self, synthetic_data):
        result = ti.adx(synthetic_data, 200, 14)
        assert result.adx >= 0
        assert result.plus_di >= 0
        assert result.minus_di >= 0

    def test_williams_r_range(self, synthetic_data):
        wr = ti.williams_r(synthetic_data, 200)
        assert -100 <= wr <= 0

    def test_cci(self, synthetic_data):
        cci_val = ti.cci(synthetic_data, 200, 20)
        assert isinstance(cci_val, float)

    def test_vwap(self, synthetic_data):
        vwap_val = ti.vwap(synthetic_data, 200)
        assert vwap_val > 0

    def test_fibonacci(self, synthetic_data):
        fib = ti.fibonacci_retracement(synthetic_data, 200)
        assert fib.high >= fib.low
        assert fib.level_236 >= fib.level_382
        assert fib.level_382 >= fib.level_500
        assert fib.level_500 >= fib.level_618

    def test_roc(self, synthetic_data):
        roc_val = ti.roc(synthetic_data, 200, 12)
        assert isinstance(roc_val, float)

    def test_mfi_range(self, synthetic_data):
        mfi_val = ti.mfi(synthetic_data, 200, 14)
        assert 0 <= mfi_val <= 100

    def test_ichimoku(self, synthetic_data):
        ich = ti.ichimoku(synthetic_data, 200)
        assert ich.tenkan_sen > 0
        assert ich.kijun_sen > 0

    def test_macd(self, synthetic_data):
        ml = ti.macd_line(synthetic_data, 200)
        ms = ti.macd_signal(synthetic_data, 200)
        mh = ti.macd_histogram(synthetic_data, 200)
        assert isinstance(ml, float)
        assert isinstance(ms, float)
        assert abs(mh - (ml - ms)) < 0.01

    def test_parabolic_sar(self, synthetic_data):
        sar = ti.parabolic_sar(synthetic_data, 200)
        assert sar > 0

    def test_support_resistance(self, synthetic_data):
        sr = ti.support_resistance(synthetic_data, 200)
        price = synthetic_data[200].close
        for s in sr.support_levels:
            assert s < price
        for r in sr.resistance_levels:
            assert r > price

    def test_average_volume(self, synthetic_data):
        avg = ti.average_volume(synthetic_data, 200, 20)
        assert avg > 0

    def test_std_dev(self, flat_data):
        std = ti.standard_deviation(flat_data, 99, 50)
        assert std < 1.0  # near-flat data should have low std dev


class TestBlackScholes:
    def test_call_price_positive(self):
        p = black_scholes.price(OptionType.CALL, 100, 100, 30/365, 0.05, 0.20)
        assert p > 0

    def test_put_price_positive(self):
        p = black_scholes.price(OptionType.PUT, 100, 100, 30/365, 0.05, 0.20)
        assert p > 0

    def test_otm_call_cheaper_than_atm(self):
        atm = black_scholes.price(OptionType.CALL, 100, 100, 30/365, 0.05, 0.20)
        otm = black_scholes.price(OptionType.CALL, 100, 110, 30/365, 0.05, 0.20)
        assert otm < atm

    def test_longer_expiry_increases_price(self):
        short = black_scholes.price(OptionType.CALL, 100, 100, 30/365, 0.05, 0.20)
        long_ = black_scholes.price(OptionType.CALL, 100, 100, 365/365, 0.05, 0.20)
        assert long_ > short

    def test_expired_returns_intrinsic(self):
        itm = black_scholes.price(OptionType.CALL, 110, 100, 0, 0.05, 0.20)
        assert abs(itm - 10.0) < 0.01
        otm = black_scholes.price(OptionType.CALL, 90, 100, 0, 0.05, 0.20)
        assert otm == 0.0

    def test_call_delta_range(self):
        d = black_scholes.delta(OptionType.CALL, 100, 100, 30/365, 0.05, 0.20)
        assert 0 <= d <= 1

    def test_put_delta_range(self):
        d = black_scholes.delta(OptionType.PUT, 100, 100, 30/365, 0.05, 0.20)
        assert -1 <= d <= 0

    def test_atm_call_delta_around_half(self):
        d = black_scholes.delta(OptionType.CALL, 100, 100, 365/365, 0.05, 0.20)
        assert 0.4 < d < 0.7

    def test_theta_negative_for_long(self):
        t = black_scholes.theta(OptionType.CALL, 100, 100, 30/365, 0.05, 0.20)
        assert t < 0

    def test_estimate_volatility(self):
        prices = [100 + i * 0.1 for i in range(50)]
        vol = black_scholes.estimate_volatility(prices, 20)
        assert vol > 0

    def test_intrinsic_value_call(self):
        assert black_scholes.intrinsic_value(OptionType.CALL, 110, 100) == 10.0
        assert black_scholes.intrinsic_value(OptionType.CALL, 90, 100) == 0.0

    def test_intrinsic_value_put(self):
        assert black_scholes.intrinsic_value(OptionType.PUT, 90, 100) == 10.0
        assert black_scholes.intrinsic_value(OptionType.PUT, 110, 100) == 0.0

    def test_higher_vol_increases_price(self):
        low_vol = black_scholes.price(OptionType.CALL, 100, 100, 30/365, 0.05, 0.10)
        high_vol = black_scholes.price(OptionType.CALL, 100, 100, 30/365, 0.05, 0.40)
        assert high_vol > low_vol


class TestDecimalMath:
    def test_safe_divide(self):
        assert safe_divide(10, 3) == round(10/3, 10)

    def test_divide_by_zero(self):
        assert safe_divide(10, 0) == 0.0

    def test_percent_change(self):
        assert abs(percent_change(100, 110) - 10.0) < 0.01

    def test_percent_change_from_zero(self):
        assert percent_change(0, 100) == 0.0

    def test_average(self):
        assert abs(average(10, 20, 30) - 20.0) < 0.01

    def test_average_ignores_zeros(self):
        assert abs(average(10, 0, 20) - 15.0) < 0.01

    def test_average_all_zeros(self):
        assert average(0, 0, 0) == 0.0


class TestRetryExecutor:
    def test_success_on_first_attempt(self):
        result = retry_with_backoff(lambda: 42, max_retries=3, initial_delay=0.01)
        assert result == 42

    def test_retries_on_failure(self):
        attempts = [0]
        def flaky():
            attempts[0] += 1
            if attempts[0] < 3:
                raise ValueError("fail")
            return "success"
        result = retry_with_backoff(flaky, max_retries=3, initial_delay=0.01, backoff_factor=1.0)
        assert result == "success"

    def test_exhausts_retries(self):
        def always_fail():
            raise ValueError("always fail")
        with pytest.raises(ValueError, match="always fail"):
            retry_with_backoff(always_fail, max_retries=2, initial_delay=0.01, backoff_factor=1.0)
