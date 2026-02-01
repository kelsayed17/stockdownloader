package com.stockdownloader.strategy;

import com.stockdownloader.model.PriceData;
import com.stockdownloader.util.MovingAverageCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Moving Average Convergence Divergence strategy.
 * Generates BUY on bullish crossover (MACD crosses above signal line)
 * and SELL on bearish crossover (MACD crosses below signal line).
 */
public final class MACDStrategy implements TradingStrategy {

    private static final int SCALE = 10;

    private final int fastPeriod;
    private final int slowPeriod;
    private final int signalPeriod;

    public MACDStrategy(int fastPeriod, int slowPeriod, int signalPeriod) {
        if (fastPeriod <= 0 || slowPeriod <= 0 || signalPeriod <= 0) {
            throw new IllegalArgumentException("All periods must be positive");
        }
        if (fastPeriod >= slowPeriod) {
            throw new IllegalArgumentException("Fast period must be less than slow period");
        }
        this.fastPeriod = fastPeriod;
        this.slowPeriod = slowPeriod;
        this.signalPeriod = signalPeriod;
    }

    @Override
    public String getName() {
        return "MACD (%d/%d/%d)".formatted(fastPeriod, slowPeriod, signalPeriod);
    }

    @Override
    public Signal evaluate(List<PriceData> data, int currentIndex) {
        int minRequired = slowPeriod + signalPeriod;
        if (currentIndex < minRequired) {
            return Signal.HOLD;
        }

        BigDecimal currentMACD = calculateMACD(data, currentIndex);
        BigDecimal currentSignal = calculateSignalLine(data, currentIndex);
        BigDecimal prevMACD = calculateMACD(data, currentIndex - 1);
        BigDecimal prevSignal = calculateSignalLine(data, currentIndex - 1);

        boolean macdAboveSignalNow = currentMACD.compareTo(currentSignal) > 0;
        boolean macdAboveSignalPrev = prevMACD.compareTo(prevSignal) > 0;

        if (macdAboveSignalNow && !macdAboveSignalPrev) {
            return Signal.BUY;
        }
        if (!macdAboveSignalNow && macdAboveSignalPrev) {
            return Signal.SELL;
        }
        return Signal.HOLD;
    }

    @Override
    public int getWarmupPeriod() {
        return slowPeriod + signalPeriod;
    }

    private BigDecimal calculateMACD(List<PriceData> data, int endIndex) {
        BigDecimal fastEMA = MovingAverageCalculator.ema(data, endIndex, fastPeriod);
        BigDecimal slowEMA = MovingAverageCalculator.ema(data, endIndex, slowPeriod);
        return fastEMA.subtract(slowEMA);
    }

    private BigDecimal calculateSignalLine(List<PriceData> data, int endIndex) {
        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (signalPeriod + 1));
        BigDecimal oneMinusMultiplier = BigDecimal.ONE.subtract(multiplier);

        int startIndex = Math.max(slowPeriod, endIndex - signalPeriod + 1);

        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (int i = startIndex; i < startIndex + signalPeriod && i <= endIndex; i++) {
            sum = sum.add(calculateMACD(data, i));
            count++;
        }
        if (count == 0) return BigDecimal.ZERO;

        BigDecimal signalEMA = sum.divide(BigDecimal.valueOf(count), SCALE, RoundingMode.HALF_UP);

        for (int i = startIndex + count; i <= endIndex; i++) {
            BigDecimal macdVal = calculateMACD(data, i);
            signalEMA = macdVal.multiply(multiplier)
                    .add(signalEMA.multiply(oneMinusMultiplier))
                    .setScale(SCALE, RoundingMode.HALF_UP);
        }

        return signalEMA;
    }
}
