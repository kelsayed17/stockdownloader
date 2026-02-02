package com.stockdownloader.strategy;

import com.stockdownloader.model.PriceData;
import com.stockdownloader.util.TechnicalIndicators;
import com.stockdownloader.util.TechnicalIndicators.BollingerBands;
import com.stockdownloader.util.TechnicalIndicators.Stochastic;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mean reversion strategy combining Bollinger Bands, RSI, and Stochastic Oscillator.
 *
 * BUY when: Price touches lower Bollinger Band AND RSI < oversold AND Stochastic %K < 20
 * SELL when: Price touches upper Bollinger Band AND RSI > overbought AND Stochastic %K > 80
 *
 * Uses ADX as a trend filter: only trades when ADX < 25 (range-bound market).
 * This prevents mean reversion trades during strong trends where price can continue
 * moving in one direction for extended periods.
 */
public final class BollingerBandRSIStrategy implements TradingStrategy {

    private final int bbPeriod;
    private final double bbStdDev;
    private final int rsiPeriod;
    private final double rsiOversold;
    private final double rsiOverbought;
    private final double adxThreshold;

    public BollingerBandRSIStrategy(int bbPeriod, double bbStdDev, int rsiPeriod,
                                     double rsiOversold, double rsiOverbought, double adxThreshold) {
        this.bbPeriod = bbPeriod;
        this.bbStdDev = bbStdDev;
        this.rsiPeriod = rsiPeriod;
        this.rsiOversold = rsiOversold;
        this.rsiOverbought = rsiOverbought;
        this.adxThreshold = adxThreshold;
    }

    public BollingerBandRSIStrategy() {
        this(20, 2.0, 14, 30, 70, 25);
    }

    @Override
    public String getName() {
        return "BB+RSI Mean Reversion (BB%d, RSI%d [%.0f/%.0f], ADX<%.0f)".formatted(
                bbPeriod, rsiPeriod, rsiOversold, rsiOverbought, adxThreshold);
    }

    @Override
    public Signal evaluate(List<PriceData> data, int currentIndex) {
        if (currentIndex < getWarmupPeriod()) return Signal.HOLD;

        BigDecimal close = data.get(currentIndex).close();
        BollingerBands bb = TechnicalIndicators.bollingerBands(data, currentIndex, bbPeriod, bbStdDev);
        BigDecimal rsi = TechnicalIndicators.rsi(data, currentIndex, rsiPeriod);
        Stochastic stoch = TechnicalIndicators.stochastic(data, currentIndex);
        var adxResult = TechnicalIndicators.adx(data, currentIndex);

        // Only trade in range-bound markets (ADX < threshold)
        boolean isRangeBound = adxResult.adx().doubleValue() < adxThreshold;

        // Previous values for crossover detection
        BigDecimal prevClose = data.get(currentIndex - 1).close();
        BigDecimal prevRSI = TechnicalIndicators.rsi(data, currentIndex - 1, rsiPeriod);

        // BUY: Price at/below lower BB, RSI crosses above oversold, Stochastic < 20
        boolean priceAtLowerBB = close.compareTo(bb.lower()) <= 0
                || (prevClose.compareTo(bb.lower()) < 0 && close.compareTo(bb.lower()) >= 0);
        boolean rsiRecovering = rsi.doubleValue() > rsiOversold && prevRSI.doubleValue() <= rsiOversold;
        boolean stochOversold = stoch.percentK().doubleValue() < 20;

        if (isRangeBound && (priceAtLowerBB || rsiRecovering) && stochOversold) {
            return Signal.BUY;
        }

        // SELL: Price at/above upper BB, RSI crosses below overbought, Stochastic > 80
        boolean priceAtUpperBB = close.compareTo(bb.upper()) >= 0
                || (prevClose.compareTo(bb.upper()) > 0 && close.compareTo(bb.upper()) <= 0);
        boolean rsiTopping = rsi.doubleValue() < rsiOverbought && prevRSI.doubleValue() >= rsiOverbought;
        boolean stochOverbought = stoch.percentK().doubleValue() > 80;

        if (isRangeBound && (priceAtUpperBB || rsiTopping) && stochOverbought) {
            return Signal.SELL;
        }

        return Signal.HOLD;
    }

    @Override
    public int getWarmupPeriod() {
        return Math.max(bbPeriod, Math.max(rsiPeriod + 1, 28));
    }
}
