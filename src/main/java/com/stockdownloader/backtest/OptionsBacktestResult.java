package com.stockdownloader.backtest;

import com.stockdownloader.model.OptionsTrade;
import com.stockdownloader.model.PriceData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Stores options backtest results and computes performance metrics including
 * total P/L from premiums, win rate, average premium captured, max drawdown,
 * and volume statistics.
 */
public final class OptionsBacktestResult {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final String strategyName;
    private final BigDecimal initialCapital;
    private final List<OptionsTrade> trades = new ArrayList<>();
    private BigDecimal finalCapital;
    private List<BigDecimal> equityCurve = List.of();
    private String startDate;
    private String endDate;
    private long totalVolumeTraded;

    public OptionsBacktestResult(String strategyName, BigDecimal initialCapital) {
        this.strategyName = Objects.requireNonNull(strategyName);
        this.initialCapital = Objects.requireNonNull(initialCapital);
        this.finalCapital = initialCapital;
    }

    public void addTrade(OptionsTrade trade) {
        trades.add(Objects.requireNonNull(trade));
        totalVolumeTraded += trade.getEntryVolume();
    }

    public void setFinalCapital(BigDecimal v) { this.finalCapital = v; }
    public void setEquityCurve(List<BigDecimal> curve) { this.equityCurve = List.copyOf(curve); }
    public void setStartDate(String d) { this.startDate = d; }
    public void setEndDate(String d) { this.endDate = d; }

    public BigDecimal getTotalReturn() {
        return finalCapital.subtract(initialCapital)
                .divide(initialCapital, 6, RoundingMode.HALF_UP)
                .multiply(HUNDRED);
    }

    public BigDecimal getTotalProfitLoss() {
        return finalCapital.subtract(initialCapital);
    }

    public List<OptionsTrade> getClosedTrades() {
        return trades.stream()
                .filter(t -> t.getStatus() != OptionsTrade.Status.OPEN)
                .toList();
    }

    public long getTotalTrades() { return getClosedTrades().size(); }

    public long getWinningTrades() {
        return getClosedTrades().stream().filter(OptionsTrade::isWin).count();
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
        return averagePL(true);
    }

    public BigDecimal getAverageLoss() {
        return averagePL(false);
    }

    private BigDecimal averagePL(boolean winners) {
        var filtered = getClosedTrades().stream()
                .filter(t -> t.isWin() == winners)
                .map(OptionsTrade::getProfitLoss)
                .toList();
        if (filtered.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = filtered.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(filtered.size()), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getProfitFactor() {
        BigDecimal grossProfit = BigDecimal.ZERO;
        BigDecimal grossLoss = BigDecimal.ZERO;
        for (OptionsTrade t : getClosedTrades()) {
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
        BigDecimal maxDD = BigDecimal.ZERO;
        for (BigDecimal equity : equityCurve) {
            if (equity.compareTo(peak) > 0) peak = equity;
            BigDecimal dd = peak.subtract(equity)
                    .divide(peak, 6, RoundingMode.HALF_UP)
                    .multiply(HUNDRED);
            if (dd.compareTo(maxDD) > 0) maxDD = dd;
        }
        return maxDD;
    }

    public BigDecimal getSharpeRatio(int tradingDaysPerYear) {
        if (equityCurve.size() < 2) return BigDecimal.ZERO;
        double[] dailyReturns = new double[equityCurve.size() - 1];
        for (int i = 1; i < equityCurve.size(); i++) {
            dailyReturns[i - 1] = equityCurve.get(i).subtract(equityCurve.get(i - 1))
                    .divide(equityCurve.get(i - 1), 10, RoundingMode.HALF_UP)
                    .doubleValue();
        }
        double mean = 0;
        for (double r : dailyReturns) mean += r;
        mean /= dailyReturns.length;
        double sumSqDiff = 0;
        for (double r : dailyReturns) sumSqDiff += Math.pow(r - mean, 2);
        double stdDev = Math.sqrt(sumSqDiff / dailyReturns.length);
        if (stdDev == 0) return BigDecimal.ZERO;
        double sharpe = (mean / stdDev) * Math.sqrt(tradingDaysPerYear);
        return BigDecimal.valueOf(sharpe).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Average premium collected per trade (for short strategies).
     */
    public BigDecimal getAveragePremiumCollected() {
        var closed = getClosedTrades();
        if (closed.isEmpty()) return BigDecimal.ZERO;
        BigDecimal totalPremium = closed.stream()
                .map(OptionsTrade::totalEntryCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalPremium.divide(BigDecimal.valueOf(closed.size()), 2, RoundingMode.HALF_UP);
    }

    public String getStrategyName() { return strategyName; }
    public BigDecimal getInitialCapital() { return initialCapital; }
    public BigDecimal getFinalCapital() { return finalCapital; }
    public List<OptionsTrade> getTrades() { return Collections.unmodifiableList(trades); }
    public List<BigDecimal> getEquityCurve() { return equityCurve; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public long getTotalVolumeTraded() { return totalVolumeTraded; }
}
