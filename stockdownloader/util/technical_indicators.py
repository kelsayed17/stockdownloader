"""Comprehensive technical indicator calculations for stock analysis.

Provides Bollinger Bands, Stochastic Oscillator, ATR, OBV, ADX, Parabolic SAR,
Williams %R, CCI, VWAP, Fibonacci Retracement, ROC, MFI, Ichimoku Cloud,
RSI, MACD, Support/Resistance, and more.

All functions operate on list[PriceData] with an end_index parameter to allow
incremental calculation during backtesting.
"""

from __future__ import annotations

import math
from typing import TYPE_CHECKING, NamedTuple

from .moving_average import sma as _sma, ema as _ema

if TYPE_CHECKING:
    from stockdownloader.model.price_data import PriceData


# ---------------------------------------------------------------------------
# Named tuple result types
# ---------------------------------------------------------------------------

class BollingerBands(NamedTuple):
    upper: float
    middle: float
    lower: float
    width: float


class Stochastic(NamedTuple):
    percent_k: float
    percent_d: float


class ADXResult(NamedTuple):
    adx: float
    plus_di: float
    minus_di: float


class FibonacciLevels(NamedTuple):
    high: float
    low: float
    level_236: float
    level_382: float
    level_500: float
    level_618: float
    level_786: float


class IchimokuCloud(NamedTuple):
    tenkan_sen: float      # Conversion Line (9-period)
    kijun_sen: float       # Base Line (26-period)
    senkou_span_a: float   # Leading Span A
    senkou_span_b: float   # Leading Span B (52-period)
    chikou_span: float     # Lagging Span
    price_above_cloud: bool


class SupportResistance(NamedTuple):
    support_levels: list[float]
    resistance_levels: list[float]


# ---------------------------------------------------------------------------
# Bollinger Bands
# ---------------------------------------------------------------------------

def bollinger_bands(data: list[PriceData], end_index: int,
                    period: int = 20, num_std_dev: float = 2.0) -> BollingerBands:
    """Calculate Bollinger Bands: Middle = SMA(period), Upper/Lower = Middle +/- numStdDev * StdDev."""
    if end_index < period - 1:
        return BollingerBands(0.0, 0.0, 0.0, 0.0)

    middle = _sma(data, end_index, period)
    std_dev = standard_deviation(data, end_index, period)
    deviation = std_dev * num_std_dev

    upper = middle + deviation
    lower = middle - deviation
    width = upper - lower

    return BollingerBands(upper, middle, lower, width)


def bollinger_percent_b(data: list[PriceData], end_index: int,
                        period: int = 20) -> float:
    """Calculate Bollinger Band %B: (Price - Lower) / (Upper - Lower).

    Values > 1 indicate above upper band, < 0 indicate below lower band.
    """
    bb = bollinger_bands(data, end_index, period, 2.0)
    band_range = bb.upper - bb.lower
    if band_range == 0:
        return 0.0
    return (data[end_index].close - bb.lower) / band_range


# ---------------------------------------------------------------------------
# Stochastic Oscillator
# ---------------------------------------------------------------------------

def stochastic(data: list[PriceData], end_index: int,
               k_period: int = 14, d_period: int = 3) -> Stochastic:
    """Calculate Stochastic Oscillator.

    %K = (Close - Lowest Low) / (Highest High - Lowest Low) * 100
    %D = SMA of recent %K values over d_period
    """
    if end_index < k_period - 1:
        return Stochastic(0.0, 0.0)

    percent_k = _calculate_percent_k(data, end_index, k_period)

    # Calculate %D as SMA of recent %K values
    sum_k = 0.0
    count = 0
    for i in range(max(k_period - 1, end_index - d_period + 1), end_index + 1):
        sum_k += _calculate_percent_k(data, i, k_period)
        count += 1

    percent_d = sum_k / count if count > 0 else 0.0

    return Stochastic(percent_k, percent_d)


def _calculate_percent_k(data: list[PriceData], end_index: int, period: int) -> float:
    highest_high = 0.0
    lowest_low = float("inf")

    for i in range(end_index - period + 1, end_index + 1):
        high = data[i].high
        low = data[i].low
        if high > highest_high:
            highest_high = high
        if low < lowest_low:
            lowest_low = low

    price_range = highest_high - lowest_low
    if price_range == 0:
        return 0.0

    return (data[end_index].close - lowest_low) / price_range * 100


# ---------------------------------------------------------------------------
# Average True Range (ATR)
# ---------------------------------------------------------------------------

def atr(data: list[PriceData], end_index: int, period: int = 14) -> float:
    """Calculate Average True Range with Wilder smoothing.

    TR = max(High - Low, |High - PrevClose|, |Low - PrevClose|)
    ATR = Wilder smooth of TR over period.
    """
    if end_index < period:
        return 0.0

    # Use Wilder smoothing: seed with average of first 'period' TRs
    start_index = max(1, end_index - period * 2)

    atr_val = 0.0
    count = 0
    for i in range(start_index, min(start_index + period, end_index + 1)):
        atr_val += true_range(data, i)
        count += 1
    if count == 0:
        return 0.0
    atr_val /= count

    multiplier = 1.0 / period
    one_minus_mult = 1.0 - multiplier

    for i in range(start_index + count, end_index + 1):
        atr_val = true_range(data, i) * multiplier + atr_val * one_minus_mult

    return atr_val


def true_range(data: list[PriceData], index: int) -> float:
    """Calculate True Range for a single bar."""
    if index <= 0:
        return data[index].high - data[index].low

    high = data[index].high
    low = data[index].low
    prev_close = data[index - 1].close

    tr1 = high - low
    tr2 = abs(high - prev_close)
    tr3 = abs(low - prev_close)

    return max(tr1, tr2, tr3)


# ---------------------------------------------------------------------------
# On-Balance Volume (OBV)
# ---------------------------------------------------------------------------

def obv(data: list[PriceData], end_index: int) -> float:
    """Calculate On-Balance Volume.

    If close > prevClose: OBV += volume
    If close < prevClose: OBV -= volume
    If equal: OBV unchanged
    """
    obv_val = 0.0
    for i in range(1, end_index + 1):
        if data[i].close > data[i - 1].close:
            obv_val += data[i].volume
        elif data[i].close < data[i - 1].close:
            obv_val -= data[i].volume
    return obv_val


def is_obv_rising(data: list[PriceData], end_index: int, lookback: int = 5) -> bool:
    """Check if OBV is rising (OBV now > OBV lookback bars ago)."""
    if end_index < lookback:
        return False
    current = obv(data, end_index)
    previous = obv(data, end_index - lookback)
    return current > previous


# ---------------------------------------------------------------------------
# Average Directional Index (ADX)
# ---------------------------------------------------------------------------

def adx(data: list[PriceData], end_index: int, period: int = 14) -> ADXResult:
    """Calculate ADX (Average Directional Index) with +DI and -DI using Wilder smoothing."""
    if end_index < period * 2:
        return ADXResult(0.0, 0.0, 0.0)

    start_idx = max(1, end_index - period * 3)

    # Seed smoothed +DM, -DM, and TR with sum of first 'period' values
    smooth_plus_dm = 0.0
    smooth_minus_dm = 0.0
    smooth_tr = 0.0

    seed_end = min(start_idx + period, end_index + 1)
    for i in range(start_idx, seed_end):
        if i <= 0:
            continue
        high = data[i].high
        low = data[i].low
        prev_high = data[i - 1].high
        prev_low = data[i - 1].low

        plus_dm = high - prev_high
        minus_dm = prev_low - low

        if plus_dm > 0 and plus_dm > minus_dm:
            smooth_plus_dm += plus_dm
        if minus_dm > 0 and minus_dm > plus_dm:
            smooth_minus_dm += minus_dm
        smooth_tr += true_range(data, i)

    # Wilder smoothing for remaining bars
    dx_values: list[float] = []

    for i in range(seed_end, end_index + 1):
        if i <= 0:
            continue
        high = data[i].high
        low = data[i].low
        prev_high = data[i - 1].high
        prev_low = data[i - 1].low

        plus_dm = high - prev_high
        minus_dm = prev_low - low

        cur_plus_dm = 0.0
        cur_minus_dm = 0.0

        if plus_dm > 0 and plus_dm > minus_dm:
            cur_plus_dm = plus_dm
        if minus_dm > 0 and minus_dm > plus_dm:
            cur_minus_dm = minus_dm

        smooth_plus_dm = smooth_plus_dm - smooth_plus_dm / period + cur_plus_dm
        smooth_minus_dm = smooth_minus_dm - smooth_minus_dm / period + cur_minus_dm
        smooth_tr = smooth_tr - smooth_tr / period + true_range(data, i)

        if smooth_tr != 0:
            p_di = (smooth_plus_dm / smooth_tr) * 100
            m_di = (smooth_minus_dm / smooth_tr) * 100
            di_sum = p_di + m_di
            if di_sum != 0:
                dx = abs(p_di - m_di) / di_sum * 100
                dx_values.append(dx)

    # ADX = smoothed average of DX values
    adx_value = 0.0
    if dx_values:
        adx_period = min(period, len(dx_values))
        total = sum(dx_values[len(dx_values) - adx_period:])
        adx_value = total / adx_period

    # Calculate current +DI and -DI
    plus_di = 0.0
    minus_di = 0.0
    if smooth_tr != 0:
        plus_di = (smooth_plus_dm / smooth_tr) * 100
        minus_di = (smooth_minus_dm / smooth_tr) * 100

    return ADXResult(adx_value, plus_di, minus_di)


# ---------------------------------------------------------------------------
# Parabolic SAR
# ---------------------------------------------------------------------------

def parabolic_sar(data: list[PriceData], end_index: int) -> float:
    """Calculate Parabolic SAR using the standard Wilder method.

    AF starts at 0.02, increments by 0.02 per new extreme point, max 0.20.
    """
    if end_index < 2:
        return data[0].low

    af = 0.02
    max_af = 0.20
    af_step = 0.02

    is_up_trend = data[1].close > data[0].close
    sar = data[0].low if is_up_trend else data[0].high
    ep = data[1].high if is_up_trend else data[1].low

    for i in range(2, end_index + 1):
        high = data[i].high
        low = data[i].low

        sar = sar + af * (ep - sar)

        if is_up_trend:
            sar = min(sar, data[i - 1].low, data[i - 2].low)
            if low < sar:
                # Flip to downtrend
                is_up_trend = False
                sar = ep
                ep = low
                af = af_step
            else:
                if high > ep:
                    ep = high
                    af = min(af + af_step, max_af)
        else:
            sar = max(sar, data[i - 1].high, data[i - 2].high)
            if high > sar:
                # Flip to uptrend
                is_up_trend = True
                sar = ep
                ep = high
                af = af_step
            else:
                if low < ep:
                    ep = low
                    af = min(af + af_step, max_af)

    return round(sar, 4)


def is_sar_bullish(data: list[PriceData], end_index: int) -> bool:
    """Return True if SAR indicates uptrend (SAR below price)."""
    sar = parabolic_sar(data, end_index)
    return data[end_index].close > sar


# ---------------------------------------------------------------------------
# Williams %R
# ---------------------------------------------------------------------------

def williams_r(data: list[PriceData], end_index: int, period: int = 14) -> float:
    """Calculate Williams %R.

    %R = (Highest High - Close) / (Highest High - Lowest Low) * -100
    """
    if end_index < period - 1:
        return 0.0

    highest_high = 0.0
    lowest_low = float("inf")

    for i in range(end_index - period + 1, end_index + 1):
        if data[i].high > highest_high:
            highest_high = data[i].high
        if data[i].low < lowest_low:
            lowest_low = data[i].low

    price_range = highest_high - lowest_low
    if price_range == 0:
        return 0.0

    return (highest_high - data[end_index].close) / price_range * -100


# ---------------------------------------------------------------------------
# Commodity Channel Index (CCI)
# ---------------------------------------------------------------------------

def cci(data: list[PriceData], end_index: int, period: int = 20) -> float:
    """Calculate CCI.

    CCI = (TP - SMA(TP)) / (0.015 * Mean Deviation)
    where TP = (High + Low + Close) / 3
    """
    if end_index < period - 1:
        return 0.0

    # Calculate typical prices
    tp = []
    sum_tp = 0.0

    for i in range(period):
        idx = end_index - period + 1 + i
        typical = (data[idx].high + data[idx].low + data[idx].close) / 3.0
        tp.append(typical)
        sum_tp += typical

    sma_tp = sum_tp / period

    # Mean deviation
    sum_dev = sum(abs(t - sma_tp) for t in tp)
    mean_dev = sum_dev / period

    divisor = 0.015 * mean_dev
    if divisor == 0:
        return 0.0

    current_tp = tp[-1]
    return (current_tp - sma_tp) / divisor


# ---------------------------------------------------------------------------
# VWAP (Volume-Weighted Average Price)
# ---------------------------------------------------------------------------

def vwap(data: list[PriceData], end_index: int, lookback: int = 20) -> float:
    """Calculate VWAP over a lookback period.

    VWAP = Sum(TP * Volume) / Sum(Volume)
    where TP = (High + Low + Close) / 3
    """
    start_idx = max(0, end_index - lookback + 1)

    sum_tpv = 0.0
    sum_vol = 0.0

    for i in range(start_idx, end_index + 1):
        tp = (data[i].high + data[i].low + data[i].close) / 3.0
        vol = data[i].volume
        sum_tpv += tp * vol
        sum_vol += vol

    if sum_vol == 0:
        return 0.0
    return sum_tpv / sum_vol


# ---------------------------------------------------------------------------
# Fibonacci Retracement
# ---------------------------------------------------------------------------

def fibonacci_retracement(data: list[PriceData], end_index: int,
                          lookback: int = 50) -> FibonacciLevels:
    """Calculate Fibonacci retracement levels based on swing high/low within lookback."""
    start_idx = max(0, end_index - lookback + 1)

    highest = 0.0
    lowest = float("inf")

    for i in range(start_idx, end_index + 1):
        if data[i].high > highest:
            highest = data[i].high
        if data[i].low < lowest:
            lowest = data[i].low

    price_range = highest - lowest

    return FibonacciLevels(
        high=highest,
        low=lowest,
        level_236=round(highest - price_range * 0.236, 4),
        level_382=round(highest - price_range * 0.382, 4),
        level_500=round(highest - price_range * 0.500, 4),
        level_618=round(highest - price_range * 0.618, 4),
        level_786=round(highest - price_range * 0.786, 4),
    )


# ---------------------------------------------------------------------------
# Rate of Change (ROC)
# ---------------------------------------------------------------------------

def roc(data: list[PriceData], end_index: int, period: int = 12) -> float:
    """Calculate Rate of Change.

    ROC = ((Close - Close_n) / Close_n) * 100
    """
    if end_index < period:
        return 0.0

    current_close = data[end_index].close
    past_close = data[end_index - period].close

    if past_close == 0:
        return 0.0

    return (current_close - past_close) / past_close * 100


# ---------------------------------------------------------------------------
# Money Flow Index (MFI)
# ---------------------------------------------------------------------------

def mfi(data: list[PriceData], end_index: int, period: int = 14) -> float:
    """Calculate Money Flow Index (volume-weighted RSI).

    MFI = 100 - (100 / (1 + Money Ratio))
    Money Ratio = Positive Money Flow / Negative Money Flow
    """
    if end_index < period:
        return 0.0

    positive_flow = 0.0
    negative_flow = 0.0

    for i in range(end_index - period + 1, end_index + 1):
        tp = (data[i].high + data[i].low + data[i].close) / 3.0
        prev_tp = (data[i - 1].high + data[i - 1].low + data[i - 1].close) / 3.0

        money_flow = tp * data[i].volume

        if tp > prev_tp:
            positive_flow += money_flow
        elif tp < prev_tp:
            negative_flow += money_flow

    if negative_flow == 0:
        return 100.0 if positive_flow > 0 else 0.0

    money_ratio = positive_flow / negative_flow
    return 100.0 - (100.0 / (1.0 + money_ratio))


# ---------------------------------------------------------------------------
# Ichimoku Cloud
# ---------------------------------------------------------------------------

def ichimoku(data: list[PriceData], end_index: int) -> IchimokuCloud:
    """Calculate Ichimoku Cloud components."""
    if end_index < 52:
        return IchimokuCloud(0.0, 0.0, 0.0, 0.0, 0.0, False)

    tenkan = _period_midpoint(data, end_index, 9)
    kijun = _period_midpoint(data, end_index, 26)
    senkou_a = (tenkan + kijun) / 2.0

    # Senkou Span B uses 52-period midpoint
    senkou_b = _period_midpoint(data, end_index, 52)

    # Chikou Span = current close (plotted 26 periods back)
    chikou = data[end_index].close

    price = data[end_index].close
    above_cloud = price > max(senkou_a, senkou_b)

    return IchimokuCloud(tenkan, kijun, senkou_a, senkou_b, chikou, above_cloud)


def _period_midpoint(data: list[PriceData], end_index: int, period: int) -> float:
    highest = 0.0
    lowest = float("inf")

    for i in range(end_index - period + 1, end_index + 1):
        if data[i].high > highest:
            highest = data[i].high
        if data[i].low < lowest:
            lowest = data[i].low

    return (highest + lowest) / 2.0


# ---------------------------------------------------------------------------
# RSI
# ---------------------------------------------------------------------------

def rsi(data: list[PriceData], end_index: int, period: int = 14) -> float:
    """Calculate Relative Strength Index."""
    if end_index < period + 1:
        return 50.0

    avg_gain = 0.0
    avg_loss = 0.0

    for i in range(end_index - period + 1, end_index + 1):
        change = data[i].close - data[i - 1].close
        if change > 0:
            avg_gain += change
        else:
            avg_loss += abs(change)

    avg_gain /= period
    avg_loss /= period

    if avg_loss == 0:
        return 100.0

    rs = avg_gain / avg_loss
    return 100.0 - (100.0 / (1.0 + rs))


# ---------------------------------------------------------------------------
# MACD
# ---------------------------------------------------------------------------

def macd_line(data: list[PriceData], end_index: int,
              fast: int = 12, slow: int = 26) -> float:
    """Calculate MACD line value (fast EMA - slow EMA)."""
    if end_index < slow:
        return 0.0
    return _ema(data, end_index, fast) - _ema(data, end_index, slow)


def macd_signal(data: list[PriceData], end_index: int,
                fast: int = 12, slow: int = 26, signal: int = 9) -> float:
    """Calculate MACD signal line (EMA of MACD line)."""
    if end_index < slow + signal:
        return 0.0

    multiplier = 2.0 / (signal + 1)
    one_minus_mult = 1.0 - multiplier

    start_idx = max(slow, end_index - signal + 1)

    total = 0.0
    count = 0
    for i in range(start_idx, min(start_idx + signal, end_index + 1)):
        total += macd_line(data, i, fast, slow)
        count += 1
    if count == 0:
        return 0.0

    signal_ema = total / count

    for i in range(start_idx + count, end_index + 1):
        macd_val = macd_line(data, i, fast, slow)
        signal_ema = macd_val * multiplier + signal_ema * one_minus_mult

    return signal_ema


def macd_histogram(data: list[PriceData], end_index: int,
                   fast: int = 12, slow: int = 26, signal: int = 9) -> float:
    """Calculate MACD histogram (MACD line - signal line)."""
    return macd_line(data, end_index, fast, slow) - macd_signal(data, end_index, fast, slow, signal)


# ---------------------------------------------------------------------------
# Support & Resistance
# ---------------------------------------------------------------------------

def support_resistance(data: list[PriceData], end_index: int,
                       lookback: int = 50, window: int = 3) -> SupportResistance:
    """Detect support and resistance levels using swing points.

    A swing high requires 'window' bars on each side to be lower.
    A swing low requires 'window' bars on each side to be higher.
    """
    start_idx = max(window, end_index - lookback)
    end_limit = min(end_index - window, end_index)

    supports: list[float] = []
    resistances: list[float] = []

    current_price = data[end_index].close

    for i in range(start_idx, end_limit + 1):
        is_swing_high = True
        is_swing_low = True

        for j in range(1, window + 1):
            if i - j < 0 or i + j > end_index:
                is_swing_high = False
                is_swing_low = False
                break
            if data[i].high <= data[i - j].high or data[i].high <= data[i + j].high:
                is_swing_high = False
            if data[i].low >= data[i - j].low or data[i].low >= data[i + j].low:
                is_swing_low = False

        if is_swing_high:
            level = data[i].high
            if level > current_price:
                resistances.append(level)
        if is_swing_low:
            level = data[i].low
            if level < current_price:
                supports.append(level)

    # Sort: supports descending (nearest first), resistances ascending (nearest first)
    supports.sort(reverse=True)
    resistances.sort()

    # Deduplicate levels that are within 1% of each other
    supports = _deduplicate_levels(supports, current_price)
    resistances = _deduplicate_levels(resistances, current_price)

    return SupportResistance(supports, resistances)


def _deduplicate_levels(levels: list[float], reference: float) -> list[float]:
    if not levels:
        return levels
    tolerance = reference * 0.01
    deduped = [levels[0]]

    for i in range(1, len(levels)):
        too_close = False
        for existing in deduped:
            if abs(levels[i] - existing) < tolerance:
                too_close = True
                break
        if not too_close:
            deduped.append(levels[i])
    return deduped


# ---------------------------------------------------------------------------
# Average Volume
# ---------------------------------------------------------------------------

def average_volume(data: list[PriceData], end_index: int, period: int = 20) -> float:
    """Calculate average volume over a period."""
    if end_index < period - 1:
        return 0.0
    total = sum(data[i].volume for i in range(end_index - period + 1, end_index + 1))
    return total / period


# ---------------------------------------------------------------------------
# Standard Deviation
# ---------------------------------------------------------------------------

def standard_deviation(data: list[PriceData], end_index: int, period: int) -> float:
    """Calculate standard deviation of close prices over a period."""
    if end_index < period - 1:
        return 0.0

    total = sum(data[i].close for i in range(end_index - period + 1, end_index + 1))
    mean = total / period

    sum_sq_diff = sum(
        (data[i].close - mean) ** 2
        for i in range(end_index - period + 1, end_index + 1)
    )

    variance = sum_sq_diff / period
    return math.sqrt(variance)


# ---------------------------------------------------------------------------
# Class wrapper for IndicatorValues compatibility
# ---------------------------------------------------------------------------

class TechnicalIndicators:
    """Static-method facade exposing all indicator functions as class methods."""

    sma = staticmethod(_sma)
    ema = staticmethod(_ema)
    rsi = staticmethod(rsi)
    macd_line = staticmethod(macd_line)
    macd_signal = staticmethod(macd_signal)
    macd_histogram = staticmethod(macd_histogram)
    bollinger_bands = staticmethod(bollinger_bands)
    bollinger_percent_b = staticmethod(bollinger_percent_b)
    stochastic = staticmethod(stochastic)
    atr = staticmethod(atr)
    obv = staticmethod(obv)
    is_obv_rising = staticmethod(is_obv_rising)
    adx = staticmethod(adx)
    parabolic_sar = staticmethod(parabolic_sar)
    is_sar_bullish = staticmethod(is_sar_bullish)
    williams_r = staticmethod(williams_r)
    cci = staticmethod(cci)
    vwap = staticmethod(vwap)
    fibonacci_retracement = staticmethod(fibonacci_retracement)
    roc = staticmethod(roc)
    mfi = staticmethod(mfi)
    ichimoku = staticmethod(ichimoku)
    average_volume = staticmethod(average_volume)
    standard_deviation = staticmethod(standard_deviation)
    support_resistance = staticmethod(support_resistance)
    true_range = staticmethod(true_range)
