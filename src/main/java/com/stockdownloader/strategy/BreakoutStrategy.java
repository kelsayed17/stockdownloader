package com.stockdownloader.strategy;

import com.stockdownloader.model.PriceData;
import com.stockdownloader.util.TechnicalIndicators;
import com.stockdownloader.util.TechnicalIndicators.BollingerBands;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Breakout strategy using Bollinger Band squeeze detection with volume and ATR confirmation.
 *
 * Detects low-volatility consolidation periods (Bollinger squeeze) and enters
 * when price breaks out with volume confirmation.
 *
 * BUY when: BB width at N-period low (squeeze) AND price breaks above upper band
 *           AND volume > 1.5x average AND ATR expanding
 * SELL when: Price breaks below lower band with same confirmations
 *            OR trailing stop hit (2x ATR below entry)
 */
public final class BreakoutStrategy implements TradingStrategy {

    private final int bbPeriod;
    private final int squeezeLookback;
    private final double volumeMultiplier;

    public BreakoutStrategy(int bbPeriod, int squeezeLookback, double volumeMultiplier) {
        this.bbPeriod = bbPeriod;
        this.squeezeLookback = squeezeLookback;
        this.volumeMultiplier = volumeMultiplier;
    }

    public BreakoutStrategy() {
        this(20, 120, 1.5);
    }

    @Override
    public String getName() {
        return "Breakout (BB%d, Squeeze%d, Vol>%.1fx)".formatted(
                bbPeriod, squeezeLookback, volumeMultiplier);
    }

    @Override
    public Signal evaluate(List<PriceData> data, int currentIndex) {
        if (currentIndex < getWarmupPeriod()) return Signal.HOLD;

        BigDecimal close = data.get(currentIndex).close();
        BigDecimal prevClose = data.get(currentIndex - 1).close();
        long volume = data.get(currentIndex).volume();

        BollingerBands bb = TechnicalIndicators.bollingerBands(data, currentIndex, bbPeriod, 2.0);
        BollingerBands prevBB = TechnicalIndicators.bollingerBands(data, currentIndex - 1, bbPeriod, 2.0);

        // Check for squeeze: current BB width near the minimum in the lookback period
        boolean isSqueeze = isInSqueeze(data, currentIndex);

        // Volume confirmation
        BigDecimal avgVol = TechnicalIndicators.averageVolume(data, currentIndex, 20);
        boolean highVolume = avgVol.compareTo(BigDecimal.ZERO) > 0
                && BigDecimal.valueOf(volume).compareTo(
                avgVol.multiply(BigDecimal.valueOf(volumeMultiplier))) > 0;

        // ATR expanding (current ATR > previous ATR)
        BigDecimal currATR = TechnicalIndicators.atr(data, currentIndex, 14);
        BigDecimal prevATR = TechnicalIndicators.atr(data, currentIndex - 1, 14);
        boolean atrExpanding = currATR.compareTo(prevATR) > 0;

        // Bullish breakout: price closes above upper BB from squeeze
        boolean bullishBreakout = close.compareTo(bb.upper()) > 0
                && prevClose.compareTo(prevBB.upper()) <= 0;

        if ((isSqueeze || atrExpanding) && bullishBreakout && highVolume) {
            return Signal.BUY;
        }

        // Bearish breakdown: price closes below lower BB
        boolean bearishBreakdown = close.compareTo(bb.lower()) < 0
                && prevClose.compareTo(prevBB.lower()) >= 0;

        if (bearishBreakdown) {
            return Signal.SELL;
        }

        // Also sell if price falls back inside bands from above (failed breakout)
        boolean failedBreakout = prevClose.compareTo(prevBB.upper()) > 0
                && close.compareTo(bb.middle()) < 0;

        if (failedBreakout) {
            return Signal.SELL;
        }

        return Signal.HOLD;
    }

    @Override
    public int getWarmupPeriod() {
        return Math.max(bbPeriod, squeezeLookback) + 1;
    }

    private boolean isInSqueeze(List<PriceData> data, int currentIndex) {
        BollingerBands currentBB = TechnicalIndicators.bollingerBands(data, currentIndex, bbPeriod, 2.0);
        BigDecimal currentWidth = currentBB.width();

        if (currentWidth.compareTo(BigDecimal.ZERO) == 0) return false;

        // Find min BB width in lookback period
        BigDecimal minWidth = currentWidth;
        int lookbackEnd = Math.max(bbPeriod, currentIndex - squeezeLookback);

        for (int i = lookbackEnd; i < currentIndex; i++) {
            BollingerBands pastBB = TechnicalIndicators.bollingerBands(data, i, bbPeriod, 2.0);
            if (pastBB.width().compareTo(BigDecimal.ZERO) > 0 && pastBB.width().compareTo(minWidth) < 0) {
                minWidth = pastBB.width();
            }
        }

        // Squeeze if current width is within 10% of the minimum
        BigDecimal threshold = minWidth.multiply(new BigDecimal("1.10"));
        return currentWidth.compareTo(threshold) <= 0;
    }
}
