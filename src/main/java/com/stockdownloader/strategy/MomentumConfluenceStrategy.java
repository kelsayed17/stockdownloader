package com.stockdownloader.strategy;

import com.stockdownloader.model.PriceData;
import com.stockdownloader.util.MovingAverageCalculator;
import com.stockdownloader.util.TechnicalIndicators;

import java.math.BigDecimal;
import java.util.List;

/**
 * Momentum/trend-following strategy combining MACD, ADX, and EMA.
 *
 * BUY when: MACD bullish crossover AND ADX > 25 (strong trend) AND Price > EMA(200)
 * SELL when: MACD bearish crossover OR ADX falls below 20 (trend weakening)
 *
 * Also uses OBV as volume confirmation: only enter if OBV is rising (accumulation).
 * Position sizing should be scaled by ATR externally (smaller in volatile markets).
 */
public final class MomentumConfluenceStrategy implements TradingStrategy {

    private final int fastEMA;
    private final int slowEMA;
    private final int signalPeriod;
    private final int emaTrendFilter;
    private final double adxStrengthThreshold;
    private final double adxWeakThreshold;

    public MomentumConfluenceStrategy(int fastEMA, int slowEMA, int signalPeriod,
                                       int emaTrendFilter, double adxStrengthThreshold,
                                       double adxWeakThreshold) {
        this.fastEMA = fastEMA;
        this.slowEMA = slowEMA;
        this.signalPeriod = signalPeriod;
        this.emaTrendFilter = emaTrendFilter;
        this.adxStrengthThreshold = adxStrengthThreshold;
        this.adxWeakThreshold = adxWeakThreshold;
    }

    public MomentumConfluenceStrategy() {
        this(12, 26, 9, 200, 25, 20);
    }

    @Override
    public String getName() {
        return "Momentum (MACD %d/%d/%d + ADX>%.0f + EMA%d)".formatted(
                fastEMA, slowEMA, signalPeriod, adxStrengthThreshold, emaTrendFilter);
    }

    @Override
    public Signal evaluate(List<PriceData> data, int currentIndex) {
        if (currentIndex < getWarmupPeriod()) return Signal.HOLD;

        BigDecimal close = data.get(currentIndex).close();

        // MACD crossover detection
        BigDecimal currMACDLine = TechnicalIndicators.macdLine(data, currentIndex, fastEMA, slowEMA);
        BigDecimal currMACDSignal = TechnicalIndicators.macdSignal(data, currentIndex, fastEMA, slowEMA, signalPeriod);
        BigDecimal prevMACDLine = TechnicalIndicators.macdLine(data, currentIndex - 1, fastEMA, slowEMA);
        BigDecimal prevMACDSignal = TechnicalIndicators.macdSignal(data, currentIndex - 1, fastEMA, slowEMA, signalPeriod);

        boolean macdBullishCross = currMACDLine.compareTo(currMACDSignal) > 0
                && prevMACDLine.compareTo(prevMACDSignal) <= 0;
        boolean macdBearishCross = currMACDLine.compareTo(currMACDSignal) < 0
                && prevMACDLine.compareTo(prevMACDSignal) >= 0;

        // ADX for trend strength
        var adxResult = TechnicalIndicators.adx(data, currentIndex);
        boolean strongTrend = adxResult.adx().doubleValue() > adxStrengthThreshold;
        boolean weakTrend = adxResult.adx().doubleValue() < adxWeakThreshold;
        boolean bullishDI = adxResult.plusDI().compareTo(adxResult.minusDI()) > 0;

        // EMA trend filter
        BigDecimal ema = MovingAverageCalculator.ema(data, currentIndex, emaTrendFilter);
        boolean aboveTrendEMA = close.compareTo(ema) > 0;

        // OBV confirmation
        boolean obvConfirm = TechnicalIndicators.isOBVRising(data, currentIndex, 5);

        // BUY: MACD bullish crossover + strong uptrend + above EMA + volume confirmation
        if (macdBullishCross && strongTrend && bullishDI && aboveTrendEMA && obvConfirm) {
            return Signal.BUY;
        }

        // SELL: MACD bearish crossover OR trend weakening
        if (macdBearishCross || (weakTrend && !aboveTrendEMA)) {
            return Signal.SELL;
        }

        return Signal.HOLD;
    }

    @Override
    public int getWarmupPeriod() {
        return Math.max(emaTrendFilter, slowEMA + signalPeriod) + 1;
    }
}
