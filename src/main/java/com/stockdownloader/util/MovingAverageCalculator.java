package com.stockdownloader.util;

import com.stockdownloader.model.PriceData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Shared calculations for Simple Moving Average (SMA) and Exponential Moving Average (EMA).
 * Used by multiple strategy implementations to avoid duplication.
 */
public final class MovingAverageCalculator {

    private static final int SCALE = 10;

    private MovingAverageCalculator() {}

    public static BigDecimal sma(List<PriceData> data, int endIndex, int period) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = endIndex - period + 1; i <= endIndex; i++) {
            sum = sum.add(data.get(i).close());
        }
        return sum.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal ema(List<PriceData> data, int endIndex, int period) {
        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (period + 1));
        BigDecimal oneMinusMultiplier = BigDecimal.ONE.subtract(multiplier);

        int startIndex = Math.max(0, endIndex - period - period);
        int seedEnd = Math.min(startIndex + period, endIndex + 1);

        BigDecimal sum = BigDecimal.ZERO;
        for (int i = startIndex; i < seedEnd; i++) {
            sum = sum.add(data.get(i).close());
        }
        BigDecimal ema = sum.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);

        for (int i = startIndex + period; i <= endIndex; i++) {
            ema = data.get(i).close().multiply(multiplier)
                    .add(ema.multiply(oneMinusMultiplier))
                    .setScale(SCALE, RoundingMode.HALF_UP);
        }

        return ema;
    }
}
