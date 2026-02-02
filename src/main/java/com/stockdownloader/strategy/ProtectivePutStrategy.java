package com.stockdownloader.strategy;

import com.stockdownloader.model.OptionType;
import com.stockdownloader.model.PriceData;
import com.stockdownloader.util.MovingAverageCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Protective put strategy: buy OTM puts to hedge a long stock position.
 * Provides downside protection at the cost of the put premium.
 *
 * Entry signal: price drops below moving average or RSI-like momentum weakens.
 * Exit signal: price recovers above MA or put approaches expiration with no value.
 *
 * Strike selection: buys puts at a configurable percentage below current price.
 * Expiration: targets a configurable number of days out (typically 30-60 DTE).
 */
public final class ProtectivePutStrategy implements OptionsStrategy {

    private final int maPeriod;
    private final BigDecimal otmPercent;
    private final int daysToExpiry;
    private final int momentumLookback;

    /**
     * @param maPeriod          moving average period for trend detection
     * @param otmPercent        percentage OTM for strike selection (e.g., 0.05 = 5% OTM)
     * @param daysToExpiry      target days to expiration
     * @param momentumLookback  bars to look back for momentum calculation
     */
    public ProtectivePutStrategy(int maPeriod, BigDecimal otmPercent, int daysToExpiry,
                                 int momentumLookback) {
        if (maPeriod <= 0) throw new IllegalArgumentException("maPeriod must be positive");
        if (daysToExpiry <= 0) throw new IllegalArgumentException("daysToExpiry must be positive");
        if (momentumLookback <= 0) throw new IllegalArgumentException("momentumLookback must be positive");
        this.maPeriod = maPeriod;
        this.otmPercent = otmPercent;
        this.daysToExpiry = daysToExpiry;
        this.momentumLookback = momentumLookback;
    }

    public ProtectivePutStrategy() {
        this(20, new BigDecimal("0.05"), 45, 5);
    }

    @Override
    public String getName() {
        return "Protective Put (MA%d, %s%% OTM, %dDTE)".formatted(
                maPeriod,
                otmPercent.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP),
                daysToExpiry);
    }

    @Override
    public Signal evaluate(List<PriceData> data, int currentIndex) {
        if (currentIndex < Math.max(maPeriod, momentumLookback)) {
            return Signal.HOLD;
        }

        BigDecimal currentPrice = data.get(currentIndex).close();
        BigDecimal ma = MovingAverageCalculator.sma(data, currentIndex, maPeriod);

        // Calculate momentum: percentage change over lookback period
        BigDecimal lookbackPrice = data.get(currentIndex - momentumLookback).close();
        BigDecimal momentum = BigDecimal.ZERO;
        if (lookbackPrice.compareTo(BigDecimal.ZERO) > 0) {
            momentum = currentPrice.subtract(lookbackPrice)
                    .divide(lookbackPrice, 6, RoundingMode.HALF_UP);
        }

        // Open: price crosses below MA or momentum turns negative
        boolean priceBelowMA = currentPrice.compareTo(ma) < 0;
        boolean prevAboveMA = data.get(currentIndex - 1).close().compareTo(
                MovingAverageCalculator.sma(data, currentIndex - 1, maPeriod)) >= 0;
        boolean negativeMomentum = momentum.compareTo(new BigDecimal("-0.02")) < 0;

        if (priceBelowMA && prevAboveMA) {
            return Signal.OPEN;
        }
        if (negativeMomentum && priceBelowMA) {
            return Signal.OPEN;
        }

        // Close: price recovers above MA (protection no longer needed)
        if (currentPrice.compareTo(ma) > 0) {
            BigDecimal pctAboveMA = currentPrice.subtract(ma)
                    .divide(ma, 6, RoundingMode.HALF_UP);
            if (pctAboveMA.compareTo(new BigDecimal("0.02")) > 0) {
                return Signal.CLOSE;
            }
        }

        return Signal.HOLD;
    }

    @Override
    public OptionType getOptionType() { return OptionType.PUT; }

    @Override
    public boolean isShort() { return false; }

    @Override
    public BigDecimal getTargetStrike(BigDecimal currentPrice) {
        return currentPrice.multiply(BigDecimal.ONE.subtract(otmPercent))
                .setScale(0, RoundingMode.FLOOR);
    }

    @Override
    public int getTargetDaysToExpiry() { return daysToExpiry; }

    @Override
    public int getWarmupPeriod() { return Math.max(maPeriod, momentumLookback); }
}
