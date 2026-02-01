import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Immutable snapshot of a backtest run, built via {@link Builder}.
 *
 * <p>Encapsulates all result data and lazily computes derived performance
 * metrics such as Sharpe ratio, max drawdown, and profit factor. Returned
 * collections are unmodifiable.</p>
 */
public final class BacktestResult {

    private static final int DISPLAY_SCALE = 2;
    private static final int CALC_SCALE = 6;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final String strategyName;
    private final BigDecimal initialCapital;
    private final BigDecimal finalCapital;
    private final List<Trade> trades;
    private final List<BigDecimal> equityCurve;
    private final LocalDate startDate;
    private final LocalDate endDate;

    private BacktestResult(Builder builder) {
        this.strategyName = Objects.requireNonNull(builder.strategyName);
        this.initialCapital = Objects.requireNonNull(builder.initialCapital);
        this.finalCapital = Objects.requireNonNull(builder.finalCapital);
        this.trades = Collections.unmodifiableList(new ArrayList<>(builder.trades));
        this.equityCurve = Collections.unmodifiableList(new ArrayList<>(builder.equityCurve));
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
    }

    // ---- Core Metrics ----

    public BigDecimal getTotalReturn() {
        return finalCapital.subtract(initialCapital)
                .divide(initialCapital, CALC_SCALE, RoundingMode.HALF_UP)
                .multiply(HUNDRED);
    }

    public BigDecimal getTotalProfitLoss() {
        return finalCapital.subtract(initialCapital);
    }

    // ---- Trade Statistics ----

    public long getTotalTrades() {
        return trades.stream().filter(Trade::isClosed).count();
    }

    public long getWinningTrades() {
        return trades.stream().filter(Trade::isClosed).filter(Trade::isWin).count();
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

    public BigDecimal getAverageWin() {
        return trades.stream()
                .filter(Trade::isClosed)
                .filter(Trade::isWin)
                .map(Trade::getProfitLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(1, getWinningTrades())), DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal getAverageLoss() {
        long losers = getLosingTrades();
        if (losers == 0) return BigDecimal.ZERO;
        return trades.stream()
                .filter(Trade::isClosed)
                .filter(t -> !t.isWin())
                .map(Trade::getProfitLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(losers), DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal getProfitFactor() {
        BigDecimal grossProfit = trades.stream()
                .filter(Trade::isClosed).filter(Trade::isWin)
                .map(Trade::getProfitLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grossLoss = trades.stream()
                .filter(Trade::isClosed).filter(t -> !t.isWin())
                .map(t -> t.getProfitLoss().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (grossLoss.compareTo(BigDecimal.ZERO) == 0) {
            return grossProfit.signum() > 0 ? BigDecimal.valueOf(999.99) : BigDecimal.ZERO;
        }
        return grossProfit.divide(grossLoss, DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

    // ---- Risk Metrics ----

    public BigDecimal getMaxDrawdown() {
        if (equityCurve.isEmpty()) return BigDecimal.ZERO;

        BigDecimal peak = equityCurve.getFirst();
        BigDecimal maxDrawdown = BigDecimal.ZERO;

        for (BigDecimal equity : equityCurve) {
            if (equity.compareTo(peak) > 0) {
                peak = equity;
            }
            BigDecimal drawdown = peak.subtract(equity)
                    .divide(peak, CALC_SCALE, RoundingMode.HALF_UP)
                    .multiply(HUNDRED);
            if (drawdown.compareTo(maxDrawdown) > 0) {
                maxDrawdown = drawdown;
            }
        }
        return maxDrawdown;
    }

    public BigDecimal getSharpeRatio(int tradingDaysPerYear) {
        if (equityCurve.size() < 2) return BigDecimal.ZERO;

        double[] dailyReturns = IntStream.range(1, equityCurve.size())
                .mapToDouble(i -> equityCurve.get(i).subtract(equityCurve.get(i - 1))
                        .divide(equityCurve.get(i - 1), 10, RoundingMode.HALF_UP)
                        .doubleValue())
                .toArray();

        DoubleSummaryStatistics stats = java.util.Arrays.stream(dailyReturns).summaryStatistics();
        double meanReturn = stats.getAverage();

        double variance = java.util.Arrays.stream(dailyReturns)
                .map(r -> Math.pow(r - meanReturn, 2))
                .average()
                .orElse(0.0);
        double stdDev = Math.sqrt(variance);

        if (stdDev == 0) return BigDecimal.ZERO;

        double sharpe = (meanReturn / stdDev) * Math.sqrt(tradingDaysPerYear);
        return BigDecimal.valueOf(sharpe).setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

    // ---- Benchmark ----

    public static BigDecimal buyAndHoldReturn(List<PriceData> data) {
        if (data == null || data.isEmpty()) return BigDecimal.ZERO;
        BigDecimal first = data.getFirst().close();
        BigDecimal last = data.getLast().close();
        return last.subtract(first).divide(first, CALC_SCALE, RoundingMode.HALF_UP).multiply(HUNDRED);
    }

    // ---- Reporting ----

    public void printReport(List<PriceData> data) {
        String separator = "=".repeat(70);
        String thinSep = "-".repeat(70);

        System.out.println();
        System.out.println(separator);
        System.out.println("  BACKTEST REPORT: " + strategyName);
        System.out.println(separator);
        System.out.println();

        System.out.println("  Period:              " + startDate + " to " + endDate);
        System.out.printf("  Initial Capital:     $%s%n", initialCapital.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP));
        System.out.printf("  Final Capital:       $%s%n", finalCapital.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP));
        System.out.println();

        System.out.println(thinSep);
        System.out.println("  PERFORMANCE METRICS");
        System.out.println(thinSep);
        System.out.printf("  Total Return:        %s%%%n", getTotalReturn().setScale(DISPLAY_SCALE, RoundingMode.HALF_UP));
        System.out.printf("  Buy & Hold Return:   %s%%%n", buyAndHoldReturn(data).setScale(DISPLAY_SCALE, RoundingMode.HALF_UP));
        System.out.printf("  Total P/L:           $%s%n", getTotalProfitLoss().setScale(DISPLAY_SCALE, RoundingMode.HALF_UP));
        System.out.printf("  Sharpe Ratio:        %s%n", getSharpeRatio(TechnicalIndicators.TRADING_DAYS_PER_YEAR));
        System.out.printf("  Max Drawdown:        %s%%%n", getMaxDrawdown().setScale(DISPLAY_SCALE, RoundingMode.HALF_UP));
        System.out.printf("  Profit Factor:       %s%n", getProfitFactor());
        System.out.println();

        System.out.println(thinSep);
        System.out.println("  TRADE STATISTICS");
        System.out.println(thinSep);
        System.out.printf("  Total Trades:        %d%n", getTotalTrades());
        System.out.printf("  Winning Trades:      %d%n", getWinningTrades());
        System.out.printf("  Losing Trades:       %d%n", getLosingTrades());
        System.out.printf("  Win Rate:            %s%%%n", getWinRate().setScale(DISPLAY_SCALE, RoundingMode.HALF_UP));
        System.out.printf("  Average Win:         $%s%n", getAverageWin());
        System.out.printf("  Average Loss:        $%s%n", getAverageLoss());
        System.out.println();

        if (!trades.isEmpty()) {
            System.out.println(thinSep);
            System.out.println("  TRADE LOG");
            System.out.println(thinSep);
            int count = 1;
            for (Trade t : trades) {
                if (t.isClosed()) {
                    System.out.printf("  #%-4d %s%n", count++, t);
                }
            }
        }

        System.out.println();
        System.out.println(separator);
    }

    // ---- Getters ----

    public String getStrategyName()        { return strategyName; }
    public BigDecimal getInitialCapital()   { return initialCapital; }
    public BigDecimal getFinalCapital()     { return finalCapital; }
    public List<Trade> getTrades()          { return trades; }
    public List<BigDecimal> getEquityCurve() { return equityCurve; }
    public LocalDate getStartDate()         { return startDate; }
    public LocalDate getEndDate()           { return endDate; }

    // ---- Builder ----

    /**
     * Builder for {@link BacktestResult}. Collects trades and equity points during
     * the backtest run, then produces an immutable result snapshot via {@link #build()}.
     */
    public static final class Builder {
        private String strategyName;
        private BigDecimal initialCapital;
        private BigDecimal finalCapital;
        private final List<Trade> trades = new ArrayList<>();
        private final List<BigDecimal> equityCurve = new ArrayList<>();
        private LocalDate startDate;
        private LocalDate endDate;

        public Builder strategyName(String name)            { this.strategyName = name; return this; }
        public Builder initialCapital(BigDecimal capital)    { this.initialCapital = capital; return this; }
        public Builder finalCapital(BigDecimal capital)      { this.finalCapital = capital; return this; }
        public Builder startDate(LocalDate date)             { this.startDate = date; return this; }
        public Builder endDate(LocalDate date)               { this.endDate = date; return this; }

        public Builder addTrade(Trade trade) {
            trades.add(Objects.requireNonNull(trade));
            return this;
        }

        public Builder addEquityPoint(BigDecimal equity) {
            equityCurve.add(Objects.requireNonNull(equity));
            return this;
        }

        public BacktestResult build() {
            Objects.requireNonNull(strategyName, "strategyName is required");
            Objects.requireNonNull(initialCapital, "initialCapital is required");
            if (finalCapital == null) {
                finalCapital = initialCapital;
            }
            return new BacktestResult(this);
        }
    }
}
