package com.stockdownloader.backtest;

import com.stockdownloader.model.OptionTrade;
import com.stockdownloader.model.OptionType;
import com.stockdownloader.model.PriceData;
import com.stockdownloader.model.Trade;
import com.stockdownloader.strategy.OptionsStrategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Backtesting engine for options strategies. Simulates options trading
 * with support for multi-leg strategies, expiration handling, assignment,
 * premium collection/payment, and volume tracking.
 *
 * Handles the unique aspects of options vs equities:
 * - Time decay (theta) through expiration-based exit
 * - Strike price selection and moneyness at expiration
 * - Multi-leg positions (straddles, iron condors)
 * - Premium-based P/L calculations (100 shares per contract)
 * - Volume-aware entry filtering
 */
public final class OptionsBacktestEngine {

    private final BigDecimal initialCapital;
    private final BigDecimal commissionPerContract;

    public OptionsBacktestEngine(BigDecimal initialCapital, BigDecimal commissionPerContract) {
        this.initialCapital = Objects.requireNonNull(initialCapital);
        this.commissionPerContract = Objects.requireNonNull(commissionPerContract);
    }

    public OptionsBacktestResult run(OptionsStrategy strategy, List<PriceData> data) {
        Objects.requireNonNull(strategy);
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("data must not be null or empty");
        }

        var result = new OptionsBacktestResult(strategy.getName(), initialCapital);
        BigDecimal cash = initialCapital;
        List<OptionTrade> openTrades = new ArrayList<>();
        List<BigDecimal> equityCurve = new ArrayList<>(data.size());

        result.setStartDate(data.getFirst().date());
        result.setEndDate(data.getLast().date());

        for (int i = 0; i < data.size(); i++) {
            PriceData bar = data.get(i);
            OptionsStrategy.Action action = strategy.evaluate(data, i, openTrades);

            // Close trades if action says so or if options have expired
            if (action == OptionsStrategy.Action.CLOSE && !openTrades.isEmpty()) {
                cash = closeAllTrades(openTrades, bar, result, cash);
                openTrades.clear();
            } else {
                // Auto-close expired options
                cash = closeExpiredTrades(openTrades, bar, result, cash);
            }

            // Calculate current equity including open positions
            BigDecimal equity = cash;
            for (OptionTrade trade : openTrades) {
                equity = equity.add(estimateOpenPositionValue(trade, bar));
            }
            equityCurve.add(equity);

            // Open new trades if action says so and no open positions
            if (action == OptionsStrategy.Action.OPEN && openTrades.isEmpty()) {
                List<OptionTrade> newTrades = strategy.createTrades(data, i, cash);
                for (OptionTrade trade : newTrades) {
                    BigDecimal tradeCost = computeTradeCost(trade);
                    if (cash.compareTo(tradeCost) >= 0) {
                        cash = cash.subtract(tradeCost);
                        openTrades.add(trade);
                    }
                }
            }
        }

        // Close any remaining open positions at last bar
        if (!openTrades.isEmpty()) {
            PriceData lastBar = data.getLast();
            cash = closeAllTrades(openTrades, lastBar, result, cash);
        }

        result.setFinalCapital(cash);
        result.setEquityCurve(equityCurve);

        return result;
    }

    private BigDecimal closeAllTrades(List<OptionTrade> openTrades, PriceData bar,
                                      OptionsBacktestResult result, BigDecimal cash) {
        for (OptionTrade trade : new ArrayList<>(openTrades)) {
            if (trade.getStatus() == OptionTrade.Status.OPEN) {
                cash = closeTrade(trade, bar, cash);
                result.addTrade(trade);
            }
        }
        openTrades.clear();
        return cash;
    }

    private BigDecimal closeExpiredTrades(List<OptionTrade> openTrades, PriceData bar,
                                          OptionsBacktestResult result, BigDecimal cash) {
        var expired = openTrades.stream()
                .filter(t -> t.getStatus() == OptionTrade.Status.OPEN
                        && bar.date().compareTo(t.getExpirationDate()) >= 0)
                .toList();

        for (OptionTrade trade : expired) {
            cash = closeTrade(trade, bar, cash);
            result.addTrade(trade);
            openTrades.remove(trade);
        }
        return cash;
    }

    private BigDecimal closeTrade(OptionTrade trade, PriceData bar, BigDecimal cash) {
        trade.closeAtExpiration(bar.close());

        BigDecimal commission = commissionPerContract.multiply(BigDecimal.valueOf(trade.getContracts()));

        if (trade.getDirection() == Trade.Direction.SHORT) {
            // For short options: we already collected the premium at entry
            // At close: if assigned, we owe the intrinsic value
            cash = cash.add(trade.getProfitLoss()).subtract(commission);
        } else {
            // For long options: we paid premium at entry
            // At close: we receive the intrinsic value
            cash = cash.add(trade.getProfitLoss()).subtract(commission);
        }

        return cash;
    }

    private BigDecimal computeTradeCost(OptionTrade trade) {
        BigDecimal multiplier = BigDecimal.valueOf(trade.getContracts() * 100L);
        BigDecimal commission = commissionPerContract.multiply(BigDecimal.valueOf(trade.getContracts()));

        if (trade.getDirection() == Trade.Direction.LONG) {
            // Buying: pay premium + commission
            return trade.getEntryPremium().multiply(multiplier).add(commission);
        } else {
            // Selling: receive premium, but need margin (use premium as proxy for cost)
            // Commission still applies
            return commission;
        }
    }

    private BigDecimal estimateOpenPositionValue(OptionTrade trade, PriceData bar) {
        BigDecimal multiplier = BigDecimal.valueOf(trade.getContracts() * 100L);
        BigDecimal intrinsicValue;

        if (trade.getOptionType() == OptionType.CALL) {
            BigDecimal diff = bar.close().subtract(trade.getStrike());
            intrinsicValue = diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO;
        } else {
            BigDecimal diff = trade.getStrike().subtract(bar.close());
            intrinsicValue = diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO;
        }

        // Add time value estimate: simple linear decay
        BigDecimal timeValue = trade.getEntryPremium().multiply(new BigDecimal("0.5"));
        BigDecimal estimatedPremium = intrinsicValue.add(timeValue);

        if (trade.getDirection() == Trade.Direction.LONG) {
            return estimatedPremium.multiply(multiplier);
        } else {
            // Short position: negative value if option is ITM
            return trade.getEntryPremium().subtract(estimatedPremium).multiply(multiplier);
        }
    }
}
