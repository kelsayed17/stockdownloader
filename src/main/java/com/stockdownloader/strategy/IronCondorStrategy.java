package com.stockdownloader.strategy;

import com.stockdownloader.model.*;
import com.stockdownloader.util.MovingAverageCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Iron condor strategy: a four-leg options strategy that profits from low volatility.
 * Sells an OTM call spread and an OTM put spread simultaneously.
 *
 * Structure:
 *   - Sell OTM put  (lower-middle strike)
 *   - Buy further OTM put (lowest strike, wing protection)
 *   - Sell OTM call (upper-middle strike)
 *   - Buy further OTM call (highest strike, wing protection)
 *
 * Entry: When volatility is elevated (expecting it to decrease) and price is
 *        range-bound near the SMA.
 * Exit: At expiration or if underlying breaches the short strikes.
 * Volume filter: Requires minimum volume for all four legs.
 */
public final class IronCondorStrategy implements OptionsStrategy {

    private final int smaPeriod;
    private final BigDecimal shortStrikeDistancePct;
    private final BigDecimal wingWidthPct;
    private final int targetDTE;
    private final long minVolume;

    public IronCondorStrategy(int smaPeriod, BigDecimal shortStrikeDistancePct,
                              BigDecimal wingWidthPct, int targetDTE, long minVolume) {
        if (smaPeriod <= 0) throw new IllegalArgumentException("smaPeriod must be positive");
        this.smaPeriod = smaPeriod;
        this.shortStrikeDistancePct = shortStrikeDistancePct;
        this.wingWidthPct = wingWidthPct;
        this.targetDTE = targetDTE;
        this.minVolume = minVolume;
    }

    public IronCondorStrategy() {
        this(20, new BigDecimal("5"), new BigDecimal("3"), 30, 0);
    }

    @Override
    public String getName() {
        return "Iron Condor (%d SMA, %s%%/%s%% width, %dDTE)".formatted(
                smaPeriod, shortStrikeDistancePct, wingWidthPct, targetDTE);
    }

    @Override
    public Action evaluate(List<PriceData> data, int currentIndex, List<OptionTrade> openTrades) {
        if (currentIndex < smaPeriod) return Action.HOLD;

        PriceData current = data.get(currentIndex);

        if (!openTrades.isEmpty()) {
            // Close at expiration
            for (OptionTrade trade : openTrades) {
                if (current.date().compareTo(trade.getExpirationDate()) >= 0) {
                    return Action.CLOSE;
                }
            }

            // Close if price breaches short strikes (risk management)
            BigDecimal shortPutStrike = null;
            BigDecimal shortCallStrike = null;
            for (OptionTrade trade : openTrades) {
                if (trade.getDirection() == Trade.Direction.SHORT) {
                    if (trade.getOptionType() == OptionType.PUT) shortPutStrike = trade.getStrike();
                    if (trade.getOptionType() == OptionType.CALL) shortCallStrike = trade.getStrike();
                }
            }
            if (shortPutStrike != null && current.close().compareTo(shortPutStrike) < 0) {
                return Action.CLOSE;
            }
            if (shortCallStrike != null && current.close().compareTo(shortCallStrike) > 0) {
                return Action.CLOSE;
            }

            return Action.HOLD;
        }

        // Entry: price near SMA (range-bound), moderate volatility
        BigDecimal sma = MovingAverageCalculator.sma(data, currentIndex, smaPeriod);
        BigDecimal deviation = current.close().subtract(sma).abs()
                .divide(sma, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        // Enter when price is within 2% of SMA (range-bound)
        if (deviation.compareTo(new BigDecimal("2")) < 0) {
            return Action.OPEN;
        }

        return Action.HOLD;
    }

    @Override
    public List<OptionTrade> createTrades(List<PriceData> data, int currentIndex, BigDecimal availableCapital) {
        PriceData current = data.get(currentIndex);
        BigDecimal price = current.close();

        BigDecimal shortPutStrike = price.multiply(
                BigDecimal.ONE.subtract(shortStrikeDistancePct.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)))
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal longPutStrike = price.multiply(
                BigDecimal.ONE.subtract(shortStrikeDistancePct.add(wingWidthPct).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)))
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal shortCallStrike = price.multiply(
                BigDecimal.ONE.add(shortStrikeDistancePct.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)))
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal longCallStrike = price.multiply(
                BigDecimal.ONE.add(shortStrikeDistancePct.add(wingWidthPct).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)))
                .setScale(0, RoundingMode.HALF_UP);

        String expirationDate = java.time.LocalDate.parse(current.date()).plusDays(targetDTE).toString();

        // Premium estimates: short options collect more than long options cost
        BigDecimal shortPutPremium = price.multiply(new BigDecimal("0.012")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal longPutPremium = price.multiply(new BigDecimal("0.006")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal shortCallPremium = price.multiply(new BigDecimal("0.012")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal longCallPremium = price.multiply(new BigDecimal("0.006")).setScale(2, RoundingMode.HALF_UP);

        // Max risk per contract = wing width * 100 - net credit
        BigDecimal wingWidth = shortCallStrike.subtract(longCallStrike).abs().max(
                shortPutStrike.subtract(longPutStrike).abs());
        BigDecimal netCredit = shortPutPremium.add(shortCallPremium).subtract(longPutPremium).subtract(longCallPremium);
        BigDecimal maxRiskPerContract = wingWidth.multiply(BigDecimal.valueOf(100)).subtract(netCredit.multiply(BigDecimal.valueOf(100)));

        BigDecimal maxSpend = availableCapital.multiply(new BigDecimal("0.10"));
        int contracts = maxRiskPerContract.compareTo(BigDecimal.ZERO) > 0
                ? Math.max(1, maxSpend.divide(maxRiskPerContract, 0, RoundingMode.DOWN).intValue())
                : 1;

        List<OptionTrade> trades = new ArrayList<>(4);

        // Short put (collect premium)
        trades.add(new OptionTrade(OptionType.PUT, Trade.Direction.SHORT,
                shortPutStrike, expirationDate, current.date(),
                shortPutPremium, contracts, current.volume()));

        // Long put (wing protection)
        trades.add(new OptionTrade(OptionType.PUT, Trade.Direction.LONG,
                longPutStrike, expirationDate, current.date(),
                longPutPremium, contracts, current.volume()));

        // Short call (collect premium)
        trades.add(new OptionTrade(OptionType.CALL, Trade.Direction.SHORT,
                shortCallStrike, expirationDate, current.date(),
                shortCallPremium, contracts, current.volume()));

        // Long call (wing protection)
        trades.add(new OptionTrade(OptionType.CALL, Trade.Direction.LONG,
                longCallStrike, expirationDate, current.date(),
                longCallPremium, contracts, current.volume()));

        return trades;
    }

    @Override public int getTargetDTE() { return targetDTE; }
    @Override public long getMinVolume() { return minVolume; }
    @Override public int getWarmupPeriod() { return smaPeriod; }
}
