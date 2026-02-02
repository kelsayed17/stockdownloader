package com.stockdownloader.strategy;

import com.stockdownloader.model.*;
import com.stockdownloader.util.MovingAverageCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Long straddle strategy: simultaneously buys an ATM call and ATM put with
 * the same strike and expiration. Profits from large moves in either direction.
 *
 * Entry: When historical volatility is low (price consolidating near SMA)
 *        anticipating a breakout. Uses Bollinger Band width as a proxy.
 * Strike selection: At-the-money (nearest strike to current price).
 * Exit: Closes both legs when combined premium exceeds entry cost by the profit target,
 *        or at expiration.
 * Volume filter: Requires minimum volume on both the call and put legs.
 */
public final class StraddleStrategy implements OptionsStrategy {

    private final int smaPeriod;
    private final int targetDTE;
    private final long minVolume;
    private final BigDecimal profitTargetPct;
    private final BigDecimal bbWidthThreshold;

    public StraddleStrategy(int smaPeriod, int targetDTE, long minVolume,
                            BigDecimal profitTargetPct, BigDecimal bbWidthThreshold) {
        if (smaPeriod <= 0) throw new IllegalArgumentException("smaPeriod must be positive");
        this.smaPeriod = smaPeriod;
        this.targetDTE = targetDTE;
        this.minVolume = minVolume;
        this.profitTargetPct = profitTargetPct;
        this.bbWidthThreshold = bbWidthThreshold;
    }

    public StraddleStrategy() {
        this(20, 30, 0, new BigDecimal("50"), new BigDecimal("0.04"));
    }

    @Override
    public String getName() {
        return "Long Straddle (%d SMA, %dDTE, %s%% target)".formatted(smaPeriod, targetDTE, profitTargetPct);
    }

    @Override
    public Action evaluate(List<PriceData> data, int currentIndex, List<OptionTrade> openTrades) {
        if (currentIndex < smaPeriod) return Action.HOLD;

        PriceData current = data.get(currentIndex);

        if (!openTrades.isEmpty()) {
            // Check expiration
            for (OptionTrade trade : openTrades) {
                if (current.date().compareTo(trade.getExpirationDate()) >= 0) {
                    return Action.CLOSE;
                }
            }

            // Check profit target: if underlying moved enough from entry
            BigDecimal entryPrice = openTrades.getFirst().getStrike();
            BigDecimal movePercent = current.close().subtract(entryPrice).abs()
                    .divide(entryPrice, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            if (movePercent.compareTo(profitTargetPct.divide(BigDecimal.valueOf(10), 2, RoundingMode.HALF_UP)) > 0) {
                return Action.CLOSE;
            }
            return Action.HOLD;
        }

        // Entry: when Bollinger Band width is narrow (low volatility, expecting breakout)
        BigDecimal bbWidth = computeBollingerBandWidth(data, currentIndex, smaPeriod);
        if (bbWidth.compareTo(bbWidthThreshold) < 0) {
            return Action.OPEN;
        }

        return Action.HOLD;
    }

    @Override
    public List<OptionTrade> createTrades(List<PriceData> data, int currentIndex, BigDecimal availableCapital) {
        PriceData current = data.get(currentIndex);
        BigDecimal strikePrice = current.close().setScale(0, RoundingMode.HALF_UP);

        String expirationDate = java.time.LocalDate.parse(current.date()).plusDays(targetDTE).toString();

        // ATM call premium ~3% of underlying
        BigDecimal callPremium = current.close().multiply(new BigDecimal("0.03"))
                .setScale(2, RoundingMode.HALF_UP);
        // ATM put premium ~3% of underlying
        BigDecimal putPremium = current.close().multiply(new BigDecimal("0.03"))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalCostPerContract = callPremium.add(putPremium).multiply(BigDecimal.valueOf(100));
        BigDecimal maxSpend = availableCapital.multiply(new BigDecimal("0.10")); // 10% of capital
        int contracts = totalCostPerContract.compareTo(BigDecimal.ZERO) > 0
                ? Math.max(1, maxSpend.divide(totalCostPerContract, 0, RoundingMode.DOWN).intValue())
                : 1;

        List<OptionTrade> trades = new ArrayList<>(2);

        trades.add(new OptionTrade(
                OptionType.CALL, Trade.Direction.LONG,
                strikePrice, expirationDate, current.date(),
                callPremium, contracts, current.volume()));

        trades.add(new OptionTrade(
                OptionType.PUT, Trade.Direction.LONG,
                strikePrice, expirationDate, current.date(),
                putPremium, contracts, current.volume()));

        return trades;
    }

    @Override public int getTargetDTE() { return targetDTE; }
    @Override public long getMinVolume() { return minVolume; }
    @Override public int getWarmupPeriod() { return smaPeriod; }

    private BigDecimal computeBollingerBandWidth(List<PriceData> data, int currentIndex, int period) {
        if (currentIndex < period - 1) return BigDecimal.ONE;

        BigDecimal sma = MovingAverageCalculator.sma(data, currentIndex, period);
        if (sma.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ONE;

        double sumSqDiff = 0;
        for (int i = currentIndex - period + 1; i <= currentIndex; i++) {
            double diff = data.get(i).close().subtract(sma).doubleValue();
            sumSqDiff += diff * diff;
        }
        double stdDev = Math.sqrt(sumSqDiff / period);
        BigDecimal bandwidth = BigDecimal.valueOf(4 * stdDev)
                .divide(sma, 6, RoundingMode.HALF_UP);
        return bandwidth;
    }
}
