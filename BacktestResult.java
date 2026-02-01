import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class BacktestResult {
    private String strategyName;
    private BigDecimal initialCapital;
    private BigDecimal finalCapital;
    private List<Trade> trades;
    private List<BigDecimal> equityCurve;
    private String startDate;
    private String endDate;

    public BacktestResult(String strategyName, BigDecimal initialCapital) {
        this.strategyName = strategyName;
        this.initialCapital = initialCapital;
        this.finalCapital = initialCapital;
        this.trades = new ArrayList<>();
        this.equityCurve = new ArrayList<>();
    }

    public void addTrade(Trade trade) {
        trades.add(trade);
    }

    public void setFinalCapital(BigDecimal finalCapital) {
        this.finalCapital = finalCapital;
    }

    public void setEquityCurve(List<BigDecimal> equityCurve) {
        this.equityCurve = equityCurve;
    }

    public void setStartDate(String startDate) { this.startDate = startDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public BigDecimal getTotalReturn() {
        return finalCapital.subtract(initialCapital)
                .divide(initialCapital, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    public BigDecimal getTotalProfitLoss() {
        return finalCapital.subtract(initialCapital);
    }

    public int getTotalTrades() {
        return (int) trades.stream().filter(t -> t.getStatus() == Trade.Status.CLOSED).count();
    }

    public int getWinningTrades() {
        return (int) trades.stream()
                .filter(t -> t.getStatus() == Trade.Status.CLOSED)
                .filter(Trade::isWin).count();
    }

    public int getLosingTrades() {
        return getTotalTrades() - getWinningTrades();
    }

    public BigDecimal getWinRate() {
        if (getTotalTrades() == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(getWinningTrades())
                .divide(BigDecimal.valueOf(getTotalTrades()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    public BigDecimal getAverageWin() {
        List<BigDecimal> wins = new ArrayList<>();
        for (Trade t : trades) {
            if (t.getStatus() == Trade.Status.CLOSED && t.isWin()) {
                wins.add(t.getProfitLoss());
            }
        }
        if (wins.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal w : wins) sum = sum.add(w);
        return sum.divide(BigDecimal.valueOf(wins.size()), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getAverageLoss() {
        List<BigDecimal> losses = new ArrayList<>();
        for (Trade t : trades) {
            if (t.getStatus() == Trade.Status.CLOSED && !t.isWin()) {
                losses.add(t.getProfitLoss());
            }
        }
        if (losses.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal l : losses) sum = sum.add(l);
        return sum.divide(BigDecimal.valueOf(losses.size()), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getProfitFactor() {
        BigDecimal grossProfit = BigDecimal.ZERO;
        BigDecimal grossLoss = BigDecimal.ZERO;
        for (Trade t : trades) {
            if (t.getStatus() == Trade.Status.CLOSED) {
                if (t.isWin()) {
                    grossProfit = grossProfit.add(t.getProfitLoss());
                } else {
                    grossLoss = grossLoss.add(t.getProfitLoss().abs());
                }
            }
        }
        if (grossLoss.compareTo(BigDecimal.ZERO) == 0) {
            return grossProfit.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(999.99) : BigDecimal.ZERO;
        }
        return grossProfit.divide(grossLoss, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getMaxDrawdown() {
        if (equityCurve.isEmpty()) return BigDecimal.ZERO;

        BigDecimal peak = equityCurve.get(0);
        BigDecimal maxDrawdown = BigDecimal.ZERO;

        for (BigDecimal equity : equityCurve) {
            if (equity.compareTo(peak) > 0) {
                peak = equity;
            }
            BigDecimal drawdown = peak.subtract(equity)
                    .divide(peak, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            if (drawdown.compareTo(maxDrawdown) > 0) {
                maxDrawdown = drawdown;
            }
        }
        return maxDrawdown;
    }

    public BigDecimal getSharpeRatio(int tradingDaysPerYear) {
        if (equityCurve.size() < 2) return BigDecimal.ZERO;

        // Calculate daily returns
        List<Double> dailyReturns = new ArrayList<>();
        for (int i = 1; i < equityCurve.size(); i++) {
            double ret = equityCurve.get(i).subtract(equityCurve.get(i - 1))
                    .divide(equityCurve.get(i - 1), 10, RoundingMode.HALF_UP)
                    .doubleValue();
            dailyReturns.add(ret);
        }

        // Mean return
        double sumReturns = 0;
        for (double r : dailyReturns) sumReturns += r;
        double meanReturn = sumReturns / dailyReturns.size();

        // Standard deviation
        double sumSqDiff = 0;
        for (double r : dailyReturns) {
            sumSqDiff += Math.pow(r - meanReturn, 2);
        }
        double stdDev = Math.sqrt(sumSqDiff / dailyReturns.size());

        if (stdDev == 0) return BigDecimal.ZERO;

        // Annualized Sharpe (assuming risk-free rate ~ 0 for simplicity)
        double sharpe = (meanReturn / stdDev) * Math.sqrt(tradingDaysPerYear);
        return BigDecimal.valueOf(sharpe).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getBuyAndHoldReturn(List<PriceData> data) {
        if (data.isEmpty()) return BigDecimal.ZERO;
        BigDecimal first = data.get(0).getClose();
        BigDecimal last = data.get(data.size() - 1).getClose();
        return last.subtract(first).divide(first, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    public void printReport(List<PriceData> data) {
        String separator = "=".repeat(70);
        String thinSep = "-".repeat(70);

        System.out.println();
        System.out.println(separator);
        System.out.println("  BACKTEST REPORT: " + strategyName);
        System.out.println(separator);
        System.out.println();

        System.out.println("  Period:              " + startDate + " to " + endDate);
        System.out.println("  Initial Capital:     $" + initialCapital.setScale(2, RoundingMode.HALF_UP));
        System.out.println("  Final Capital:       $" + finalCapital.setScale(2, RoundingMode.HALF_UP));
        System.out.println();

        System.out.println(thinSep);
        System.out.println("  PERFORMANCE METRICS");
        System.out.println(thinSep);
        System.out.printf("  Total Return:        %s%%%n", getTotalReturn().setScale(2, RoundingMode.HALF_UP));
        System.out.printf("  Buy & Hold Return:   %s%%%n", getBuyAndHoldReturn(data).setScale(2, RoundingMode.HALF_UP));
        System.out.printf("  Total P/L:           $%s%n", getTotalProfitLoss().setScale(2, RoundingMode.HALF_UP));
        System.out.printf("  Sharpe Ratio:        %s%n", getSharpeRatio(252));
        System.out.printf("  Max Drawdown:        %s%%%n", getMaxDrawdown().setScale(2, RoundingMode.HALF_UP));
        System.out.printf("  Profit Factor:       %s%n", getProfitFactor());
        System.out.println();

        System.out.println(thinSep);
        System.out.println("  TRADE STATISTICS");
        System.out.println(thinSep);
        System.out.printf("  Total Trades:        %d%n", getTotalTrades());
        System.out.printf("  Winning Trades:      %d%n", getWinningTrades());
        System.out.printf("  Losing Trades:       %d%n", getLosingTrades());
        System.out.printf("  Win Rate:            %s%%%n", getWinRate().setScale(2, RoundingMode.HALF_UP));
        System.out.printf("  Average Win:         $%s%n", getAverageWin());
        System.out.printf("  Average Loss:        $%s%n", getAverageLoss());
        System.out.println();

        if (!trades.isEmpty()) {
            System.out.println(thinSep);
            System.out.println("  TRADE LOG");
            System.out.println(thinSep);
            int count = 1;
            for (Trade t : trades) {
                if (t.getStatus() == Trade.Status.CLOSED) {
                    System.out.printf("  #%-4d %s%n", count++, t);
                }
            }
        }

        System.out.println();
        System.out.println(separator);
    }

    public String getStrategyName() { return strategyName; }
    public BigDecimal getInitialCapital() { return initialCapital; }
    public BigDecimal getFinalCapital() { return finalCapital; }
    public List<Trade> getTrades() { return trades; }
    public List<BigDecimal> getEquityCurve() { return equityCurve; }
}
