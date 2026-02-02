package com.stockdownloader.backtest;

import com.stockdownloader.model.OptionType;
import com.stockdownloader.model.OptionsTrade;
import com.stockdownloader.model.PriceData;
import com.stockdownloader.strategy.OptionsStrategy;
import com.stockdownloader.util.BlackScholesCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Backtesting engine for options strategies. Simulates options trading against
 * historical underlying price data using Black-Scholes pricing for synthetic
 * option premiums. Supports dated expirations and strike price selection.
 *
 * The engine:
 * - Evaluates the strategy signal at each bar
 * - Uses Black-Scholes to price synthetic options at the target strike/expiry
 * - Tracks time decay (theta) and premium changes as the underlying moves
 * - Handles expiration: closes positions when DTE reaches zero
 * - Captures volume from the underlying for each trade entry
 * - Maintains an equity curve accounting for premium flow
 */
public final class OptionsBacktestEngine {

    private static final int CONTRACT_MULTIPLIER = 100;
    private static final BigDecimal DEFAULT_RISK_FREE_RATE = new BigDecimal("0.05");
    private static final int VOLATILITY_LOOKBACK = 20;

    private final BigDecimal initialCapital;
    private final BigDecimal commission;
    private final BigDecimal riskFreeRate;

    public OptionsBacktestEngine(BigDecimal initialCapital, BigDecimal commission) {
        this(initialCapital, commission, DEFAULT_RISK_FREE_RATE);
    }

    public OptionsBacktestEngine(BigDecimal initialCapital, BigDecimal commission,
                                 BigDecimal riskFreeRate) {
        this.initialCapital = Objects.requireNonNull(initialCapital);
        this.commission = Objects.requireNonNull(commission);
        this.riskFreeRate = Objects.requireNonNull(riskFreeRate);
    }

    public OptionsBacktestResult run(OptionsStrategy strategy, List<PriceData> data) {
        Objects.requireNonNull(strategy, "strategy must not be null");
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("data must not be null or empty");
        }

        var result = new OptionsBacktestResult(strategy.getName(), initialCapital);
        BigDecimal cash = initialCapital;
        OptionsTrade currentTrade = null;
        int tradeEntryBar = -1;
        BigDecimal tradeStrike = BigDecimal.ZERO;
        int tradeDTE = 0;
        List<BigDecimal> equityCurve = new ArrayList<>(data.size());

        result.setStartDate(data.getFirst().date());
        result.setEndDate(data.getLast().date());

        // Pre-compute close prices for volatility estimation
        BigDecimal[] closePrices = data.stream().map(PriceData::close).toArray(BigDecimal[]::new);

        for (int i = 0; i < data.size(); i++) {
            PriceData bar = data.get(i);
            BigDecimal spotPrice = bar.close();

            // Calculate current position value
            BigDecimal equity = cash;
            if (currentTrade != null && currentTrade.getStatus() == OptionsTrade.Status.OPEN) {
                int barsHeld = i - tradeEntryBar;
                int remainingDTE = Math.max(tradeDTE - barsHeld, 0);
                BigDecimal timeToExpiry = BigDecimal.valueOf(remainingDTE).divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);
                BigDecimal vol = BlackScholesCalculator.estimateVolatility(closePrices, Math.min(i + 1, VOLATILITY_LOOKBACK));

                BigDecimal currentPremium = BlackScholesCalculator.price(
                        strategy.getOptionType(), spotPrice, tradeStrike,
                        timeToExpiry, riskFreeRate, vol);

                BigDecimal positionValue = currentPremium
                        .multiply(BigDecimal.valueOf((long) currentTrade.getContracts() * CONTRACT_MULTIPLIER));

                if (strategy.isShort()) {
                    // Short position: we received premium, owe the current value
                    equity = cash.subtract(positionValue).add(currentTrade.totalEntryCost());
                } else {
                    // Long position: value is the current premium
                    equity = cash.add(positionValue);
                }
            }
            equityCurve.add(equity);

            // Check expiration
            if (currentTrade != null && currentTrade.getStatus() == OptionsTrade.Status.OPEN) {
                int barsHeld = i - tradeEntryBar;
                if (barsHeld >= tradeDTE) {
                    // Option expired
                    BigDecimal intrinsic = BlackScholesCalculator.intrinsicValue(
                            strategy.getOptionType(), spotPrice, tradeStrike);

                    if (strategy.isShort()) {
                        currentTrade.expire(bar.date(), intrinsic);
                        cash = cash.subtract(intrinsic.multiply(
                                BigDecimal.valueOf((long) currentTrade.getContracts() * CONTRACT_MULTIPLIER)));
                        cash = cash.subtract(commission);
                    } else {
                        currentTrade.expire(bar.date(), intrinsic);
                        cash = cash.add(intrinsic.multiply(
                                BigDecimal.valueOf((long) currentTrade.getContracts() * CONTRACT_MULTIPLIER)));
                        cash = cash.subtract(commission);
                    }
                    result.addTrade(currentTrade);
                    currentTrade = null;
                    continue;
                }
            }

            // Evaluate strategy signal
            OptionsStrategy.Signal signal = strategy.evaluate(data, i);

            if (signal == OptionsStrategy.Signal.OPEN && currentTrade == null) {
                BigDecimal vol = BlackScholesCalculator.estimateVolatility(
                        closePrices, Math.min(i + 1, VOLATILITY_LOOKBACK));
                tradeStrike = strategy.getTargetStrike(spotPrice);
                tradeDTE = strategy.getTargetDaysToExpiry();
                BigDecimal timeToExpiry = BigDecimal.valueOf(tradeDTE)
                        .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);

                BigDecimal premium = BlackScholesCalculator.price(
                        strategy.getOptionType(), spotPrice, tradeStrike,
                        timeToExpiry, riskFreeRate, vol);

                if (premium.compareTo(BigDecimal.ZERO) <= 0) continue;

                // Determine number of contracts based on available capital
                int contracts;
                if (strategy.isShort()) {
                    // For short options, require margin (use underlying price as collateral)
                    BigDecimal marginPerContract = spotPrice.multiply(BigDecimal.valueOf(CONTRACT_MULTIPLIER));
                    contracts = cash.subtract(commission)
                            .divide(marginPerContract, 0, RoundingMode.DOWN)
                            .intValue();
                } else {
                    // For long options, cost is the premium
                    BigDecimal costPerContract = premium.multiply(BigDecimal.valueOf(CONTRACT_MULTIPLIER));
                    contracts = cash.subtract(commission)
                            .divide(costPerContract, 0, RoundingMode.DOWN)
                            .intValue();
                }

                contracts = Math.min(contracts, 10); // Cap at 10 contracts for risk management

                if (contracts > 0) {
                    OptionsTrade.Direction dir = strategy.isShort()
                            ? OptionsTrade.Direction.SELL : OptionsTrade.Direction.BUY;

                    currentTrade = new OptionsTrade(
                            strategy.getOptionType(), dir, tradeStrike,
                            bar.date(), bar.date(), premium, contracts, bar.volume());
                    tradeEntryBar = i;

                    if (strategy.isShort()) {
                        cash = cash.add(currentTrade.totalEntryCost());
                    } else {
                        cash = cash.subtract(currentTrade.totalEntryCost());
                    }
                    cash = cash.subtract(commission);
                }
            } else if (signal == OptionsStrategy.Signal.CLOSE
                    && currentTrade != null
                    && currentTrade.getStatus() == OptionsTrade.Status.OPEN) {
                int barsHeld = i - tradeEntryBar;
                int remainingDTE = Math.max(tradeDTE - barsHeld, 0);
                BigDecimal timeToExpiry = BigDecimal.valueOf(remainingDTE)
                        .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);
                BigDecimal vol = BlackScholesCalculator.estimateVolatility(
                        closePrices, Math.min(i + 1, VOLATILITY_LOOKBACK));

                BigDecimal exitPremium = BlackScholesCalculator.price(
                        strategy.getOptionType(), spotPrice, tradeStrike,
                        timeToExpiry, riskFreeRate, vol);

                if (strategy.isShort()) {
                    cash = cash.subtract(exitPremium.multiply(
                            BigDecimal.valueOf((long) currentTrade.getContracts() * CONTRACT_MULTIPLIER)));
                } else {
                    cash = cash.add(exitPremium.multiply(
                            BigDecimal.valueOf((long) currentTrade.getContracts() * CONTRACT_MULTIPLIER)));
                }
                cash = cash.subtract(commission);

                currentTrade.close(bar.date(), exitPremium);
                result.addTrade(currentTrade);
                currentTrade = null;
            }
        }

        // Force close any remaining open position at the last bar
        if (currentTrade != null && currentTrade.getStatus() == OptionsTrade.Status.OPEN) {
            PriceData lastBar = data.getLast();
            BigDecimal intrinsic = BlackScholesCalculator.intrinsicValue(
                    strategy.getOptionType(), lastBar.close(), tradeStrike);

            if (strategy.isShort()) {
                cash = cash.subtract(intrinsic.multiply(
                        BigDecimal.valueOf((long) currentTrade.getContracts() * CONTRACT_MULTIPLIER)));
            } else {
                cash = cash.add(intrinsic.multiply(
                        BigDecimal.valueOf((long) currentTrade.getContracts() * CONTRACT_MULTIPLIER)));
            }
            cash = cash.subtract(commission);
            currentTrade.close(lastBar.date(), intrinsic);
            result.addTrade(currentTrade);
        }

        result.setFinalCapital(cash);
        result.setEquityCurve(equityCurve);
        return result;
    }
}
