package com.stockdownloader.strategy;

import com.stockdownloader.model.*;
import com.stockdownloader.util.MovingAverageCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Protective put strategy: buys put options as insurance against downside
 * moves in the underlying stock. Acts as a portfolio hedge.
 *
 * Entry: When the underlying price drops below the 20-day SMA (bearish signal)
 *        or volatility increases (large daily move), buy puts for protection.
 * Strike selection: ATM or slightly OTM puts (configurable percent below current price).
 * Exit: Sells the put when price recovers above SMA, or lets it expire.
 * Volume filter: Requires minimum underlying volume for liquidity.
 */
public final class ProtectivePutStrategy implements OptionsStrategy {

    private final int smaPeriod;
    private final BigDecimal percentOTM;
    private final int targetDTE;
    private final long minVolume;
    private final BigDecimal volatilityThreshold;

    public ProtectivePutStrategy(int smaPeriod, BigDecimal percentOTM, int targetDTE,
                                 long minVolume, BigDecimal volatilityThreshold) {
        if (smaPeriod <= 0) throw new IllegalArgumentException("smaPeriod must be positive");
        this.smaPeriod = smaPeriod;
        this.percentOTM = percentOTM;
        this.targetDTE = targetDTE;
        this.minVolume = minVolume;
        this.volatilityThreshold = volatilityThreshold;
    }

    public ProtectivePutStrategy() {
        this(20, new BigDecimal("3"), 30, 0, new BigDecimal("2.0"));
    }

    @Override
    public String getName() {
        return "Protective Put (%d SMA, %s%% OTM, %dDTE)".formatted(smaPeriod, percentOTM, targetDTE);
    }

    @Override
    public Action evaluate(List<PriceData> data, int currentIndex, List<OptionTrade> openTrades) {
        if (currentIndex < smaPeriod) return Action.HOLD;

        PriceData current = data.get(currentIndex);
        BigDecimal sma = MovingAverageCalculator.sma(data, currentIndex, smaPeriod);

        if (!openTrades.isEmpty()) {
            // Close if price recovered above SMA or option nearing expiry
            if (current.close().compareTo(sma) > 0) {
                return Action.CLOSE;
            }
            for (OptionTrade trade : openTrades) {
                if (current.date().compareTo(trade.getExpirationDate()) >= 0) {
                    return Action.CLOSE;
                }
            }
            return Action.HOLD;
        }

        // Entry: price below SMA or large daily decline
        boolean priceBelowSMA = current.close().compareTo(sma) < 0;
        boolean largeMove = false;

        if (currentIndex > 0) {
            BigDecimal prevClose = data.get(currentIndex - 1).close();
            if (prevClose.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal dailyChange = current.close().subtract(prevClose)
                        .divide(prevClose, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).abs();
                largeMove = dailyChange.compareTo(volatilityThreshold) > 0
                        && current.close().compareTo(prevClose) < 0;
            }
        }

        if (priceBelowSMA || largeMove) {
            return Action.OPEN;
        }

        return Action.HOLD;
    }

    @Override
    public List<OptionTrade> createTrades(List<PriceData> data, int currentIndex, BigDecimal availableCapital) {
        PriceData current = data.get(currentIndex);
        BigDecimal strikePrice = current.close().multiply(
                BigDecimal.ONE.subtract(percentOTM.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)));
        strikePrice = strikePrice.setScale(0, RoundingMode.HALF_UP);

        // Estimate premium for protective put (~1.5% of underlying for slightly OTM)
        BigDecimal estimatedPremium = current.close().multiply(new BigDecimal("0.015"))
                .setScale(2, RoundingMode.HALF_UP);

        String expirationDate = java.time.LocalDate.parse(current.date()).plusDays(targetDTE).toString();

        // Calculate contracts based on capital allocation (max 5% of capital on put premium)
        BigDecimal maxPremiumSpend = availableCapital.multiply(new BigDecimal("0.05"));
        BigDecimal costPerContract = estimatedPremium.multiply(BigDecimal.valueOf(100));
        int contracts = costPerContract.compareTo(BigDecimal.ZERO) > 0
                ? Math.max(1, maxPremiumSpend.divide(costPerContract, 0, RoundingMode.DOWN).intValue())
                : 1;

        var trade = new OptionTrade(
                OptionType.PUT, Trade.Direction.LONG,
                strikePrice, expirationDate, current.date(),
                estimatedPremium, contracts, current.volume());

        return List.of(trade);
    }

    @Override public int getTargetDTE() { return targetDTE; }
    @Override public long getMinVolume() { return minVolume; }
    @Override public int getWarmupPeriod() { return smaPeriod; }
}
