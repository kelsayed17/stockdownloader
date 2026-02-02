package com.stockdownloader.strategy;

import com.stockdownloader.model.OptionType;
import com.stockdownloader.model.PriceData;
import com.stockdownloader.util.MovingAverageCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Covered call strategy: sell OTM calls against a long stock position.
 * Generates income from premium collection while capping upside.
 *
 * Entry signal: price is above the moving average (bullish bias, but not strongly trending)
 * Exit signal: approaching expiration or price moves significantly against the position
 *
 * Strike selection: sells calls at a configurable percentage above current price.
 * Expiration: targets a configurable number of days out (typically 30-45 DTE).
 */
public final class CoveredCallStrategy implements OptionsStrategy {

    private final int maPeriod;
    private final BigDecimal otmPercent;
    private final int daysToExpiry;
    private final BigDecimal exitThreshold;

    /**
     * @param maPeriod       moving average period for trend filter
     * @param otmPercent     percentage OTM for strike selection (e.g., 0.05 = 5% OTM)
     * @param daysToExpiry   target days to expiration
     * @param exitThreshold  percentage move that triggers early exit (e.g., 0.03 = 3%)
     */
    public CoveredCallStrategy(int maPeriod, BigDecimal otmPercent, int daysToExpiry,
                               BigDecimal exitThreshold) {
        if (maPeriod <= 0) throw new IllegalArgumentException("maPeriod must be positive");
        if (daysToExpiry <= 0) throw new IllegalArgumentException("daysToExpiry must be positive");
        this.maPeriod = maPeriod;
        this.otmPercent = otmPercent;
        this.daysToExpiry = daysToExpiry;
        this.exitThreshold = exitThreshold;
    }

    public CoveredCallStrategy() {
        this(20, new BigDecimal("0.05"), 30, new BigDecimal("0.03"));
    }

    @Override
    public String getName() {
        return "Covered Call (MA%d, %s%% OTM, %dDTE)".formatted(
                maPeriod,
                otmPercent.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP),
                daysToExpiry);
    }

    @Override
    public Signal evaluate(List<PriceData> data, int currentIndex) {
        if (currentIndex < maPeriod) {
            return Signal.HOLD;
        }

        BigDecimal currentPrice = data.get(currentIndex).close();
        BigDecimal ma = MovingAverageCalculator.sma(data, currentIndex, maPeriod);

        // Open: price above MA (mild bullish / neutral trend)
        if (currentPrice.compareTo(ma) > 0) {
            BigDecimal prevPrice = data.get(currentIndex - 1).close();
            BigDecimal prevMA = MovingAverageCalculator.sma(data, currentIndex - 1, maPeriod);

            // Only open when price just crossed above MA or is consolidating
            if (prevPrice.compareTo(prevMA) <= 0) {
                return Signal.OPEN;
            }
        }

        // Close: price drops significantly below MA (trend reversal)
        BigDecimal pctBelowMA = ma.subtract(currentPrice)
                .divide(ma, 6, RoundingMode.HALF_UP);
        if (pctBelowMA.compareTo(exitThreshold) > 0) {
            return Signal.CLOSE;
        }

        return Signal.HOLD;
    }

    @Override
    public OptionType getOptionType() { return OptionType.CALL; }

    @Override
    public boolean isShort() { return true; }

    @Override
    public BigDecimal getTargetStrike(BigDecimal currentPrice) {
        return currentPrice.multiply(BigDecimal.ONE.add(otmPercent))
                .setScale(0, RoundingMode.CEILING);
    }

    @Override
    public int getTargetDaysToExpiry() { return daysToExpiry; }

    @Override
    public int getWarmupPeriod() { return maPeriod; }
}
