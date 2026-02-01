package com.stockdownloader.backtest;

import com.stockdownloader.model.PriceData;
import com.stockdownloader.model.Trade;
import com.stockdownloader.strategy.TradingStrategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Core backtesting simulation engine that runs a strategy against historical price data
 * and produces a detailed result with trade log and equity curve.
 */
public final class BacktestEngine {

    private final BigDecimal initialCapital;
    private final BigDecimal commission;

    public BacktestEngine(BigDecimal initialCapital, BigDecimal commission) {
        this.initialCapital = Objects.requireNonNull(initialCapital, "initialCapital must not be null");
        this.commission = Objects.requireNonNull(commission, "commission must not be null");
    }

    public BacktestResult run(TradingStrategy strategy, List<PriceData> data) {
        Objects.requireNonNull(strategy, "strategy must not be null");
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("data must not be null or empty");
        }

        var result = new BacktestResult(strategy.getName(), initialCapital);
        BigDecimal cash = initialCapital;
        Trade currentTrade = null;
        List<BigDecimal> equityCurve = new ArrayList<>(data.size());

        result.setStartDate(data.getFirst().date());
        result.setEndDate(data.getLast().date());

        for (int i = 0; i < data.size(); i++) {
            PriceData bar = data.get(i);
            TradingStrategy.Signal signal = strategy.evaluate(data, i);

            BigDecimal equity = cash;
            if (currentTrade != null && currentTrade.getStatus() == Trade.Status.OPEN) {
                BigDecimal positionValue = bar.close().multiply(BigDecimal.valueOf(currentTrade.getShares()));
                equity = cash.add(positionValue);
            }
            equityCurve.add(equity);

            if (signal == TradingStrategy.Signal.BUY && currentTrade == null) {
                int shares = cash.subtract(commission)
                        .divide(bar.close(), 0, RoundingMode.DOWN)
                        .intValue();

                if (shares > 0) {
                    BigDecimal cost = bar.close().multiply(BigDecimal.valueOf(shares)).add(commission);
                    cash = cash.subtract(cost);
                    currentTrade = new Trade(Trade.Direction.LONG, bar.date(), bar.close(), shares);
                }
            } else if (signal == TradingStrategy.Signal.SELL
                    && currentTrade != null
                    && currentTrade.getStatus() == Trade.Status.OPEN) {
                cash = closePosition(currentTrade, bar, cash);
                result.addTrade(currentTrade);
                currentTrade = null;
            }
        }

        if (currentTrade != null && currentTrade.getStatus() == Trade.Status.OPEN) {
            PriceData lastBar = data.getLast();
            cash = closePosition(currentTrade, lastBar, cash);
            result.addTrade(currentTrade);
        }

        result.setFinalCapital(cash);
        result.setEquityCurve(equityCurve);

        return result;
    }

    private BigDecimal closePosition(Trade trade, PriceData bar, BigDecimal cash) {
        BigDecimal proceeds = bar.close()
                .multiply(BigDecimal.valueOf(trade.getShares()))
                .subtract(commission);
        trade.close(bar.date(), bar.close());
        return cash.add(proceeds);
    }
}
