package com.stockdownloader.backtest;

import com.stockdownloader.model.PriceData;
import com.stockdownloader.model.Trade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Stores backtest results and computes performance metrics including
 * total return, Sharpe ratio, max drawdown, win rate, and profit factor.
 */
public final class BacktestResult {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final String strategyName;
    private final BigDecimal initialCapital;
    private final List<Trade> trades = new ArrayList<>();
    private BigDecimal finalCapital;
    private List<BigDecimal> equityCurve = List.of();
    private String startDate;
    private String endDate;

    public BacktestResult(String strategyName, BigDecimal initialCapital) {
        this.strategyName = Objects.requireNonNull(strategyName, "strategyName must not be null");
        this.initialCapital = Objects.requireNonNull(initialCapital, "initialCapital must not be null");
        this.finalCapital = initialCapital;
    }

    public void addTrade(Trade trade) {
        trades.add(Objects.requireNonNull(trade, "trade must not be null"));
    }

    public void setFinalCapital(BigDecimal finalCapital) { this.finalCapital = Objects.requireNonNull(finalCapital); }
    public void setEquityCurve(List<BigDecimal> equityCurve) { this.equityCurve = List.copyOf(equityCurve); }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public BigDecimal getTotalReturn() {
        return finalCapital.subtract(initialCapital)
                .divide(initialCapital, 6, RoundingMode.HALF_UP)
                .multiply(HUNDRED);
    }

    public BigDecimal getTotalProfitLoss() {
        return finalCapital.subtract(initialCapital);
    }

    public long getTotalTrades() { return getClosedTrades().size(); }

    public long getWinningTrades() {
        return getClosedTrades().stream().filter(Trade::isWin).count();
    }

    public long getLosingTrades() { return getTotalTrades() - getWinningTrades(); }

    public BigDecimal getWinRate() {
        long total = getTotalTrades();
        if (total == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(getWinningTrades())
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED);
    }

    public BigDecimal getAverageWin() {
        return averageProfitLoss(true);
    }

    public BigDecimal getAverageLoss() {
        return averageProfitLoss(false);
    }

    private BigDecimal averageProfitLoss(boolean winners) {
        var filtered = getClosedTrades().stream()
                .filter(t -> t.isWin() == winners)
                .map(Trade::getProfitLoss)
                .toList();
        if (filtered.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = filtered.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(filtered.size()), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getProfitFactor() {
        BigDecimal grossProfit = BigDecimal.ZERO;
        BigDecimal grossLoss = BigDecimal.ZERO;
        for (Trade t : getClosedTrades()) {
            if (t.isWin()) {
                grossProfit = grossProfit.add(t.getProfitLoss());
            } else {
                grossLoss = grossLoss.add(t.getProfitLoss().abs());
            }
        }
        if (grossLoss.compareTo(BigDecimal.ZERO) == 0) {
            return grossProfit.compareTo(BigDecimal.ZERO) > 0
                    ? BigDecimal.valueOf(999.99) : BigDecimal.ZERO;
        }
        return grossProfit.divide(grossLoss, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getMaxDrawdown() {
        if (equityCurve.isEmpty()) return BigDecimal.ZERO;

        BigDecimal peak = equityCurve.getFirst();
        BigDecimal maxDrawdown = BigDecimal.ZERO;

        for (BigDecimal equity : equityCurve) {
            if (equity.compareTo(peak) > 0) {
                peak = equity;
            }
            BigDecimal drawdown = peak.subtract(equity)
                    .divide(peak, 6, RoundingMode.HALF_UP)
                    .multiply(HUNDRED);
            if (drawdown.compareTo(maxDrawdown) > 0) {
                maxDrawdown = drawdown;
            }
        }
        return maxDrawdown;
    }

    public BigDecimal getSharpeRatio(int tradingDaysPerYear) {
        if (equityCurve.size() < 2) return BigDecimal.ZERO;

        double[] dailyReturns = new double[equityCurve.size() - 1];
        for (int i = 1; i < equityCurve.size(); i++) {
            dailyReturns[i - 1] = equityCurve.get(i).subtract(equityCurve.get(i - 1))
                    .divide(equityCurve.get(i - 1), 10, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        double meanReturn = 0;
        for (double r : dailyReturns) meanReturn += r;
        meanReturn /= dailyReturns.length;

        double sumSqDiff = 0;
        for (double r : dailyReturns) {
            sumSqDiff += Math.pow(r - meanReturn, 2);
        }
        double stdDev = Math.sqrt(sumSqDiff / dailyReturns.length);

        if (stdDev == 0) return BigDecimal.ZERO;

        double sharpe = (meanReturn / stdDev) * Math.sqrt(tradingDaysPerYear);
        return BigDecimal.valueOf(sharpe).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getBuyAndHoldReturn(List<PriceData> data) {
        if (data.isEmpty()) return BigDecimal.ZERO;
        BigDecimal first = data.getFirst().close();
        BigDecimal last = data.getLast().close();
        return last.subtract(first)
                .divide(first, 6, RoundingMode.HALF_UP)
                .multiply(HUNDRED);
    }

    public List<Trade> getClosedTrades() {
        return trades.stream()
                .filter(t -> t.getStatus() == Trade.Status.CLOSED)
                .toList();
    }

    public String getStrategyName() { return strategyName; }
    public BigDecimal getInitialCapital() { return initialCapital; }
    public BigDecimal getFinalCapital() { return finalCapital; }
    public List<Trade> getTrades() { return Collections.unmodifiableList(trades); }
    public List<BigDecimal> getEquityCurve() { return equityCurve; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
}
