"""Signal generation utilities."""

from stockdownloader.model.price_data import PriceData
from stockdownloader.model.alert_result import AlertResult, AlertDirection, OptionsRecommendation, OptionsAction
from stockdownloader.model.option_type import OptionType
from stockdownloader.model.indicator_values import IndicatorValues
from stockdownloader.util import technical_indicators as ti


def generate_alert(data: list[PriceData], index: int, symbol: str = "UNKNOWN") -> AlertResult:
    """Generate a comprehensive trading alert from all technical indicators."""
    if index < 200:
        indicators = IndicatorValues.compute(data, index)
        return AlertResult(
            symbol=symbol, date=data[index].date, current_price=data[index].close,
            direction=AlertDirection.NEUTRAL, confluence_score=0.0, total_indicators=0,
            bullish_indicators=[], bearish_indicators=[],
            call_recommendation=_hold_recommendation(OptionType.CALL),
            put_recommendation=_hold_recommendation(OptionType.PUT),
            support_levels=[], resistance_levels=[], indicators=indicators,
        )

    indicators = IndicatorValues.compute(data, index)
    bullish = []
    bearish = []

    # RSI
    if indicators.rsi14 < 30:
        bullish.append(f"RSI oversold ({indicators.rsi14:.1f})")
    elif indicators.rsi14 > 70:
        bearish.append(f"RSI overbought ({indicators.rsi14:.1f})")

    # MACD
    if indicators.macd_line > indicators.macd_signal:
        bullish.append("MACD bullish crossover")
    else:
        bearish.append("MACD bearish crossover")

    # Bollinger Bands
    if data[index].close < indicators.bb_lower:
        bullish.append("Price below lower Bollinger Band")
    elif data[index].close > indicators.bb_upper:
        bearish.append("Price above upper Bollinger Band")

    # SMA crossover
    if indicators.sma50 > 0 and indicators.sma200 > 0:
        if indicators.sma50 > indicators.sma200:
            bullish.append("Golden Cross (SMA50 > SMA200)")
        else:
            bearish.append("Death Cross (SMA50 < SMA200)")

    # OBV
    if indicators.obv_rising:
        bullish.append("OBV rising (volume confirms trend)")
    else:
        bearish.append("OBV falling (volume divergence)")

    # Stochastic
    if indicators.stoch_k < 20:
        bullish.append(f"Stochastic oversold (%K={indicators.stoch_k:.1f})")
    elif indicators.stoch_k > 80:
        bearish.append(f"Stochastic overbought (%K={indicators.stoch_k:.1f})")

    # ADX
    if indicators.adx14 > 25:
        if indicators.plus_di > indicators.minus_di:
            bullish.append(f"Strong uptrend (ADX={indicators.adx14:.1f})")
        else:
            bearish.append(f"Strong downtrend (ADX={indicators.adx14:.1f})")

    # SAR
    if indicators.sar_bullish:
        bullish.append("Parabolic SAR bullish")
    else:
        bearish.append("Parabolic SAR bearish")

    # Ichimoku
    if indicators.price_above_cloud:
        bullish.append("Price above Ichimoku cloud")
    else:
        bearish.append("Price below Ichimoku cloud")

    total = len(bullish) + len(bearish)
    score = len(bullish) / total if total > 0 else 0.5

    if score >= 0.8:
        direction = AlertDirection.STRONG_BUY
    elif score >= 0.6:
        direction = AlertDirection.BUY
    elif score <= 0.2:
        direction = AlertDirection.STRONG_SELL
    elif score <= 0.4:
        direction = AlertDirection.SELL
    else:
        direction = AlertDirection.NEUTRAL

    sr = ti.support_resistance(data, index, lookback=50, window=3)

    price = data[index].close
    call_rec = OptionsRecommendation(
        type=OptionType.CALL,
        action=OptionsAction.BUY if direction in (AlertDirection.STRONG_BUY, AlertDirection.BUY) else OptionsAction.HOLD,
        suggested_strike=round(price * 1.02, 2),
        suggested_dte=30,
        estimated_premium=round(price * 0.02, 2),
        target_delta=0.40,
        rationale="Bullish signal confluence" if score >= 0.6 else "No clear signal",
    )
    put_rec = OptionsRecommendation(
        type=OptionType.PUT,
        action=OptionsAction.BUY if direction in (AlertDirection.STRONG_SELL, AlertDirection.SELL) else OptionsAction.HOLD,
        suggested_strike=round(price * 0.98, 2),
        suggested_dte=30,
        estimated_premium=round(price * 0.02, 2),
        target_delta=-0.40,
        rationale="Bearish signal confluence" if score <= 0.4 else "No clear signal",
    )

    return AlertResult(
        symbol=symbol, date=data[index].date, current_price=price,
        direction=direction, confluence_score=score, total_indicators=total,
        bullish_indicators=bullish, bearish_indicators=bearish,
        call_recommendation=call_rec, put_recommendation=put_rec,
        support_levels=sr.support_levels, resistance_levels=sr.resistance_levels,
        indicators=indicators,
    )


def _hold_recommendation(option_type: OptionType) -> OptionsRecommendation:
    return OptionsRecommendation(
        type=option_type, action=OptionsAction.HOLD,
        suggested_strike=0.0, suggested_dte=0,
        estimated_premium=0.0, target_delta=0.0,
        rationale="Insufficient data",
    )
