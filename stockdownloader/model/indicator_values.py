from __future__ import annotations

from dataclasses import dataclass
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from .price_data import PriceData


def _fmt(v: float | None) -> str:
    if v is None:
        return "N/A"
    return f"{v:.2f}"


@dataclass
class IndicatorValues:
    """Snapshot of all computed technical indicator values at a given point in time.
    Used by multi-indicator strategies and the signal generator to make decisions
    based on the full suite of available indicators rather than just price alone.
    """

    # Price data
    date: str
    close: float
    high: float
    low: float
    open: float
    volume: int

    # Moving Averages
    sma20: float
    sma50: float
    sma200: float
    ema12: float
    ema26: float

    # Momentum
    rsi14: float
    macd_line: float
    macd_signal: float
    macd_histogram: float
    roc12: float
    williams_r: float

    # Bollinger Bands
    bb_upper: float
    bb_middle: float
    bb_lower: float
    bb_width: float
    bb_percent_b: float

    # Stochastic
    stoch_k: float
    stoch_d: float

    # Volatility
    atr14: float

    # Volume
    obv: float
    obv_rising: bool
    mfi14: float
    avg_volume20: float

    # Trend
    adx14: float
    plus_di: float
    minus_di: float
    parabolic_sar: float
    sar_bullish: bool

    # Other
    cci20: float
    vwap: float

    # Ichimoku
    ichimoku_tenkan: float
    ichimoku_kijun: float
    ichimoku_span_a: float
    ichimoku_span_b: float
    price_above_cloud: bool

    # Fibonacci
    fib_high: float
    fib_low: float
    fib_236: float
    fib_382: float
    fib_500: float
    fib_618: float
    fib_786: float

    @classmethod
    def compute(cls, data: list[PriceData], index: int) -> IndicatorValues:
        """Compute all indicator values at the given index in the price data.
        Requires at least 200 bars for SMA(200); partial values are returned
        for indicators that have enough data.
        """
        from ..util.moving_average_calculator import MovingAverageCalculator
        from ..util.technical_indicators import TechnicalIndicators

        bar = data[index]

        # Moving averages
        sma20 = MovingAverageCalculator.sma(data, index, 20) if index >= 19 else 0.0
        sma50 = MovingAverageCalculator.sma(data, index, 50) if index >= 49 else 0.0
        sma200 = MovingAverageCalculator.sma(data, index, 200) if index >= 199 else 0.0
        ema12 = MovingAverageCalculator.ema(data, index, 12) if index >= 12 else 0.0
        ema26 = MovingAverageCalculator.ema(data, index, 26) if index >= 26 else 0.0

        # RSI
        rsi14 = TechnicalIndicators.rsi(data, index, 14)

        # MACD
        macd_l = TechnicalIndicators.macd_line(data, index, 12, 26)
        macd_s = TechnicalIndicators.macd_signal(data, index, 12, 26, 9)
        macd_h = TechnicalIndicators.macd_histogram(data, index, 12, 26, 9)

        # ROC
        roc = TechnicalIndicators.roc(data, index, 12)

        # Williams %R
        will_r = TechnicalIndicators.williams_r(data, index, 14)

        # Bollinger Bands
        bb = TechnicalIndicators.bollinger_bands(data, index)
        bb_pct_b = TechnicalIndicators.bollinger_percent_b(data, index, 20) if index >= 19 else 0.0

        # Stochastic
        stoch = TechnicalIndicators.stochastic(data, index)

        # ATR
        atr = TechnicalIndicators.atr(data, index, 14)

        # OBV
        obv_val = TechnicalIndicators.obv(data, index)
        obv_ris = TechnicalIndicators.is_obv_rising(data, index, 5)

        # MFI
        mfi_val = TechnicalIndicators.mfi(data, index, 14)

        # Average Volume
        avg_vol = TechnicalIndicators.average_volume(data, index, 20)

        # ADX
        adx_result = TechnicalIndicators.adx(data, index, 14)

        # Parabolic SAR
        psar = TechnicalIndicators.parabolic_sar(data, index)
        sar_bull = TechnicalIndicators.is_sar_bullish(data, index)

        # CCI
        cci_val = TechnicalIndicators.cci(data, index, 20)

        # VWAP
        vwap_val = TechnicalIndicators.vwap(data, index, 20)

        # Ichimoku
        ichimoku = TechnicalIndicators.ichimoku(data, index)

        # Fibonacci
        fib = TechnicalIndicators.fibonacci_retracement(data, index, 50)

        return cls(
            date=bar.date, close=bar.close, high=bar.high, low=bar.low,
            open=bar.open, volume=bar.volume,
            sma20=sma20, sma50=sma50, sma200=sma200, ema12=ema12, ema26=ema26,
            rsi14=rsi14, macd_line=macd_l, macd_signal=macd_s, macd_histogram=macd_h,
            roc12=roc, williams_r=will_r,
            bb_upper=bb.upper, bb_middle=bb.middle, bb_lower=bb.lower,
            bb_width=bb.width, bb_percent_b=bb_pct_b,
            stoch_k=stoch.percent_k, stoch_d=stoch.percent_d,
            atr14=atr,
            obv=obv_val, obv_rising=obv_ris, mfi14=mfi_val, avg_volume20=avg_vol,
            adx14=adx_result.adx, plus_di=adx_result.plus_di, minus_di=adx_result.minus_di,
            parabolic_sar=psar, sar_bullish=sar_bull,
            cci20=cci_val, vwap=vwap_val,
            ichimoku_tenkan=ichimoku.tenkan_sen, ichimoku_kijun=ichimoku.kijun_sen,
            ichimoku_span_a=ichimoku.senkou_span_a, ichimoku_span_b=ichimoku.senkou_span_b,
            price_above_cloud=ichimoku.price_above_cloud,
            fib_high=fib.high, fib_low=fib.low,
            fib_236=fib.level_236, fib_382=fib.level_382, fib_500=fib.level_500,
            fib_618=fib.level_618, fib_786=fib.level_786,
        )

    def summary(self) -> str:
        """Summary string for display in reports."""
        return (
            f"    {self.date}  Close: ${_fmt(self.close)}  Vol: {self.volume:,}\n"
            f"      SMA(20/50/200): {_fmt(self.sma20)} / {_fmt(self.sma50)} / {_fmt(self.sma200)}\n"
            f"      RSI(14): {_fmt(self.rsi14)}  MACD: {_fmt(self.macd_line)}  ADX: {_fmt(self.adx14)}\n"
            f"      BB: [{_fmt(self.bb_lower)} - {_fmt(self.bb_middle)} - {_fmt(self.bb_upper)}]  %B: {_fmt(self.bb_percent_b)}\n"
            f"      Stoch: %K={_fmt(self.stoch_k)} %D={_fmt(self.stoch_d)}  ATR: {_fmt(self.atr14)}\n"
            f"      OBV Rising: {'Yes' if self.obv_rising else 'No'}  MFI: {_fmt(self.mfi14)}  CCI: {_fmt(self.cci20)}\n"
            f"      VWAP: {_fmt(self.vwap)}  SAR: {_fmt(self.parabolic_sar)} ({'Bullish' if self.sar_bullish else 'Bearish'})\n"
            f"      Ichimoku: {'Above' if self.price_above_cloud else 'Below'} cloud  Williams %R: {_fmt(self.williams_r)}\n"
            f"      Fib Levels: 23.6%={_fmt(self.fib_236)}  38.2%={_fmt(self.fib_382)}  50%={_fmt(self.fib_500)}  61.8%={_fmt(self.fib_618)}"
        )
