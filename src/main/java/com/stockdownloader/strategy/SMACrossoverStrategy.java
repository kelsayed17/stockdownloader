package com.stockdownloader.strategy;

import com.stockdownloader.model.PriceData;
import com.stockdownloader.util.MovingAverageCalculator;

import java.math.BigDecimal;
import java.util.List;

/**
 * Simple Moving Average crossover strategy.
 * Generates BUY on golden cross (short SMA crosses above long SMA)
 * and SELL on death cross (short SMA crosses below long SMA).
 */
public final class SMACrossoverStrategy implements TradingStrategy {

    private final int shortPeriod;
    private final int longPeriod;

    public SMACrossoverStrategy(int shortPeriod, int longPeriod) {
        if (shortPeriod <= 0 || longPeriod <= 0) {
            throw new IllegalArgumentException("Periods must be positive");
        }
        if (shortPeriod >= longPeriod) {
            throw new IllegalArgumentException("Short period must be less than long period");
        }
        this.shortPeriod = shortPeriod;
        this.longPeriod = longPeriod;
    }

    @Override
    public String getName() {
        return "SMA Crossover (%d/%d)".formatted(shortPeriod, longPeriod);
    }

    @Override
    public Signal evaluate(List<PriceData> data, int currentIndex) {
        if (currentIndex < longPeriod) {
            return Signal.HOLD;
        }

        BigDecimal currentShortSMA = MovingAverageCalculator.sma(data, currentIndex, shortPeriod);
        BigDecimal currentLongSMA = MovingAverageCalculator.sma(data, currentIndex, longPeriod);
        BigDecimal prevShortSMA = MovingAverageCalculator.sma(data, currentIndex - 1, shortPeriod);
        BigDecimal prevLongSMA = MovingAverageCalculator.sma(data, currentIndex - 1, longPeriod);

        boolean shortAboveLongNow = currentShortSMA.compareTo(currentLongSMA) > 0;
        boolean shortAboveLongPrev = prevShortSMA.compareTo(prevLongSMA) > 0;

        if (shortAboveLongNow && !shortAboveLongPrev) {
            return Signal.BUY;
        }
        if (!shortAboveLongNow && shortAboveLongPrev) {
            return Signal.SELL;
        }
        return Signal.HOLD;
    }

    @Override
    public int getWarmupPeriod() {
        return longPeriod;
    }
}
