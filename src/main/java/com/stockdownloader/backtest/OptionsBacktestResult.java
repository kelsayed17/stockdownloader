package com.stockdownloader.backtest;

import com.stockdownloader.model.OptionTrade;
import com.stockdownloader.model.OptionType;
import com.stockdownloader.model.PriceData;
import com.stockdownloader.model.Trade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Stores options backtest results with metrics specific to options trading:
 * premium collected/paid, assignment rate, average DTE, theta decay impact,
 * and volume-weighted performance in addition to standard metrics.
 */
public final class OptionsBacktestResult {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final String strategyName;
    private final BigDecimal initialCapital;
    private final List<OptionTrade> trades = new ArrayList<>();
    private BigDecimal finalCapital;
    private List<BigDecimal> equityCurve = List.of();
    private String startDate;
    private String endDate;

    public OptionsBacktestResult(String strategyName, BigDecimal initialCapital) {
        this.strategyName = Objects.requireNonNull(strategyName);
        this.initialCapital = Objects.requireNonNull(initialCapital);
        this.finalCapital = initialCapital;
    }

    public void addTrades(List<OptionTrade> trades) {
        this.trades.addAll(trades);
    }

    public void addTrade(OptionTrade trade) {
        this.trades.add(Objects.requireNonNull(trade));
    }

    public BigDecimal getTotalReturn() {
        return finalCapital.subtract(initialCapital)
                .divide(initialCapital, 6, RoundingMode.HALF_UP)
                .multiply(HUNDRED);
    }

    public BigDecimal getTotalProfitLoss() {
        return finalCapital.subtract(initialCapital);
    }

    public long getTotalTrades() {
        return getClosedTrades().size();
    }

    public long getWinningTrades() {
        return getClosedTrades().stream().filter(OptionTrade::isWin).count();
    }

    public long getLosingTrades() {
        return getTotalTrades() - getWinningTrades();
    }

    public BigDecimal getWinRate() {
        long total = getTotalTrades();
        if (total == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(getWinningTrades())
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED);
    }

    public BigDecimal getTotalPremiumCollected() {
        return getClosedTrades().stream()
                .filter(t -> t.getDirection() == Trade.Direction.SHORT)
                .map(t -> t.getEntryPremium().multiply(BigDecimal.valueOf(t.getContracts() * 100L)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalPremiumPaid() {
        return getClosedTrades().stream()
                .filter(t -> t.getDirection() == Trade.Direction.LONG)
                .map(t -> t.getEntryPremium().multiply(BigDecimal.valueOf(t.getContracts() * 100L)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long getAssignedCount() {
        return trades.stream()
                .filter(t -> t.getStatus() == OptionTrade.Status.CLOSED_ASSIGNED)
                .count();
    }

    public long getExpiredWorthlessCount() {
        return trades.stream()
                .filter(t -> t.getStatus() == OptionTrade.Status.CLOSED_EXPIRED)
                .count();
    }

    public BigDecimal getAssignmentRate() {
        long total = getTotalTrades();
        if (total == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(getAssignedCount())
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED);
    }

    public BigDecimal getAverageWin() {
        var wins = getClosedTrades().stream()
                .filter(OptionTrade::isWin)
                .map(OptionTrade::getProfitLoss)
                .toList();
        if (wins.isEmpty()) return BigDecimal.ZERO;
        return wins.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(wins.size()), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getAverageLoss() {
        var losses = getClosedTrades().stream()
                .filter(t -> !t.isWin())
                .map(OptionTrade::getProfitLoss)
                .toList();
        if (losses.isEmpty()) return BigDecimal.ZERO;
        return losses.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(losses.size()), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getProfitFactor() {
        BigDecimal grossProfit = BigDecimal.ZERO;
        BigDecimal grossLoss = BigDecimal.ZERO;
        for (OptionTrade t : getClosedTrades()) {
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
            if (equity.compareTo(peak) > 0) peak = equity;
            BigDecimal drawdown = peak.subtract(equity)
                    .divide(peak, 6, RoundingMode.HALF_UP)
                    .multiply(HUNDRED);
            if (drawdown.compareTo(maxDrawdown) > 0) maxDrawdown = drawdown;
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
        for (double r : dailyReturns) sumSqDiff += Math.pow(r - meanReturn, 2);
        double stdDev = Math.sqrt(sumSqDiff / dailyReturns.length);

        if (stdDev == 0) return BigDecimal.ZERO;
        double sharpe = (meanReturn / stdDev) * Math.sqrt(tradingDaysPerYear);
        return BigDecimal.valueOf(sharpe).setScale(2, RoundingMode.HALF_UP);
    }

    public long getAverageEntryVolume() {
        var closed = getClosedTrades();
        if (closed.isEmpty()) return 0;
        return closed.stream().mapToLong(OptionTrade::getEntryVolume).sum() / closed.size();
    }

    public long getTotalCallTrades() {
        return trades.stream().filter(t -> t.getOptionType() == OptionType.CALL).count();
    }

    public long getTotalPutTrades() {
        return trades.stream().filter(t -> t.getOptionType() == OptionType.PUT).count();
    }

    public List<OptionTrade> getClosedTrades() {
        return trades.stream()
                .filter(t -> t.getStatus() != OptionTrade.Status.OPEN)
                .toList();
    }

    public void setFinalCapital(BigDecimal finalCapital) { this.finalCapital = Objects.requireNonNull(finalCapital); }
    public void setEquityCurve(List<BigDecimal> equityCurve) { this.equityCurve = List.copyOf(equityCurve); }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getStrategyName() { return strategyName; }
    public BigDecimal getInitialCapital() { return initialCapital; }
    public BigDecimal getFinalCapital() { return finalCapital; }
    public List<OptionTrade> getTrades() { return Collections.unmodifiableList(trades); }
    public List<BigDecimal> getEquityCurve() { return equityCurve; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
}
