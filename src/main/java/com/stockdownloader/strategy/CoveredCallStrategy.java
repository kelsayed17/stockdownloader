package com.stockdownloader.strategy;

import com.stockdownloader.model.*;
import com.stockdownloader.util.MovingAverageCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Covered call strategy: holds the underlying stock and sells OTM call options
 * against the position to collect premium income. Best in neutral-to-mildly-bullish markets.
 *
 * Entry: When price is above the 50-day SMA (bullish bias) and no open option position exists.
 * Strike selection: Sells calls at a configurable percent above current price (default 5% OTM).
 * Exit: Option expires worthless (keep premium) or gets assigned (sell shares at strike).
 * Volume filter: Only enters when underlying volume exceeds the 20-day average.
 */
public final class CoveredCallStrategy implements OptionsStrategy {

    private final int smaPeriod;
    private final BigDecimal percentOTM;
    private final int targetDTE;
    private final long minVolume;

    public CoveredCallStrategy(int smaPeriod, BigDecimal percentOTM, int targetDTE, long minVolume) {
        if (smaPeriod <= 0) throw new IllegalArgumentException("smaPeriod must be positive");
        this.smaPeriod = smaPeriod;
        this.percentOTM = percentOTM;
        this.targetDTE = targetDTE;
        this.minVolume = minVolume;
    }

    public CoveredCallStrategy() {
        this(50, new BigDecimal("5"), 30, 0);
    }

    @Override
    public String getName() {
        return "Covered Call (%d SMA, %s%% OTM, %dDTE)".formatted(smaPeriod, percentOTM, targetDTE);
    }

    @Override
    public Action evaluate(List<PriceData> data, int currentIndex, List<OptionTrade> openTrades) {
        if (currentIndex < smaPeriod) return Action.HOLD;

        boolean hasOpenTrade = !openTrades.isEmpty();

        if (hasOpenTrade) {
            // Check if any open trade has reached expiration
            PriceData current = data.get(currentIndex);
            for (OptionTrade trade : openTrades) {
                if (current.date().compareTo(trade.getExpirationDate()) >= 0) {
                    return Action.CLOSE;
                }
            }
            return Action.HOLD;
        }

        // Entry: price above SMA, volume above average
        BigDecimal sma = MovingAverageCalculator.sma(data, currentIndex, smaPeriod);
        PriceData current = data.get(currentIndex);

        if (current.close().compareTo(sma) > 0) {
            BigDecimal avgVol = VolumeProfile.computeAvgVolume(data, currentIndex, 20);
            if (avgVol.compareTo(BigDecimal.ZERO) == 0
                    || BigDecimal.valueOf(current.volume()).compareTo(avgVol) >= 0) {
                return Action.OPEN;
            }
        }

        return Action.HOLD;
    }

    @Override
    public List<OptionTrade> createTrades(List<PriceData> data, int currentIndex, BigDecimal availableCapital) {
        PriceData current = data.get(currentIndex);
        BigDecimal strikePrice = current.close().multiply(
                BigDecimal.ONE.add(percentOTM.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)));
        strikePrice = roundToStrike(strikePrice);

        // Estimate premium as ~2% of underlying price for OTM call
        BigDecimal estimatedPremium = current.close().multiply(new BigDecimal("0.02"))
                .setScale(2, RoundingMode.HALF_UP);

        String expirationDate = computeExpirationDate(current.date(), targetDTE);

        // Calculate contracts: 1 contract per 100 shares we can afford of underlying
        int sharesAffordable = availableCapital.divide(current.close(), 0, RoundingMode.DOWN).intValue();
        int contracts = Math.max(1, sharesAffordable / 100);

        var trade = new OptionTrade(
                OptionType.CALL, Trade.Direction.SHORT,
                strikePrice, expirationDate, current.date(),
                estimatedPremium, contracts, current.volume());

        return List.of(trade);
    }

    @Override public int getTargetDTE() { return targetDTE; }
    @Override public long getMinVolume() { return minVolume; }
    @Override public int getWarmupPeriod() { return smaPeriod; }

    private static BigDecimal roundToStrike(BigDecimal price) {
        // Round to nearest dollar for strike prices
        return price.setScale(0, RoundingMode.HALF_UP);
    }

    private static String computeExpirationDate(String currentDate, int dte) {
        try {
            return java.time.LocalDate.parse(currentDate).plusDays(dte).toString();
        } catch (Exception e) {
            return currentDate;
        }
    }
}
