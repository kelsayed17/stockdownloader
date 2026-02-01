import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Engine that simulates trading a {@link TradingStrategy} against historical price data.
 *
 * <p>Constructed via {@link Builder} for clear, flexible configuration. The engine
 * iterates through each bar, evaluates the strategy signal, manages position entry/exit,
 * tracks the equity curve, and produces an immutable {@link BacktestResult}.</p>
 *
 * <p>Position sizing uses a full-investment approach: on a BUY signal, all available
 * cash (minus commission) is used to buy shares. Only one position is held at a time.</p>
 */
public final class BacktestEngine {

    private final BigDecimal initialCapital;
    private final BigDecimal commission;

    private BacktestEngine(Builder builder) {
        this.initialCapital = builder.initialCapital;
        this.commission = builder.commission;
    }

    /**
     * Runs the given strategy against the price data and returns the result.
     *
     * @param strategy the trading strategy to evaluate (must not be null)
     * @param data     chronologically-ordered price history (must not be null or empty)
     * @return an immutable BacktestResult with all metrics
     * @throws IllegalArgumentException if data is empty
     */
    public BacktestResult run(TradingStrategy strategy, List<PriceData> data) {
        Objects.requireNonNull(strategy, "strategy must not be null");
        Objects.requireNonNull(data, "price data must not be null");
        if (data.isEmpty()) {
            throw new IllegalArgumentException("price data must not be empty");
        }

        // Defensive copy to prevent external mutation during run
        List<PriceData> bars = Collections.unmodifiableList(data);

        var resultBuilder = new BacktestResult.Builder()
                .strategyName(strategy.getName())
                .initialCapital(initialCapital)
                .startDate(bars.getFirst().date())
                .endDate(bars.getLast().date());

        BigDecimal cash = initialCapital;
        Trade currentTrade = null;

        for (int i = 0; i < bars.size(); i++) {
            PriceData bar = bars.get(i);
            TradingStrategy.Signal signal = strategy.evaluate(bars, i);

            // Track equity (cash + mark-to-market position value)
            BigDecimal equity = cash;
            if (currentTrade != null && currentTrade.isOpen()) {
                equity = cash.add(bar.close().multiply(BigDecimal.valueOf(currentTrade.getShares())));
            }
            resultBuilder.addEquityPoint(equity);

            // --- Process signals ---
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
                    && currentTrade != null && currentTrade.isOpen()) {
                cash = closeTrade(currentTrade, bar, resultBuilder, cash);
                currentTrade = null;
            }
        }

        // Force-close any open position at the last bar
        if (currentTrade != null && currentTrade.isOpen()) {
            cash = closeTrade(currentTrade, bars.getLast(), resultBuilder, cash);
        }

        return resultBuilder.finalCapital(cash).build();
    }

    /**
     * Closes the given trade at the bar's closing price, deducting commission.
     *
     * @return updated cash balance after closing
     */
    private BigDecimal closeTrade(Trade trade, PriceData bar,
                                  BacktestResult.Builder resultBuilder, BigDecimal cash) {
        BigDecimal proceeds = bar.close()
                .multiply(BigDecimal.valueOf(trade.getShares()))
                .subtract(commission);
        cash = cash.add(proceeds);
        trade.close(bar.date(), bar.close());
        resultBuilder.addTrade(trade);
        return cash;
    }

    // ---- Getters ----

    public BigDecimal getInitialCapital() { return initialCapital; }
    public BigDecimal getCommission()     { return commission; }

    // ---- Builder ----

    /**
     * Builder for configuring and creating a {@link BacktestEngine}.
     *
     * <p>Usage:</p>
     * <pre>{@code
     * BacktestEngine engine = new BacktestEngine.Builder()
     *     .initialCapital(new BigDecimal("100000"))
     *     .commission(BigDecimal.ZERO)
     *     .build();
     * }</pre>
     */
    public static final class Builder {
        private BigDecimal initialCapital = new BigDecimal("100000.00");
        private BigDecimal commission = BigDecimal.ZERO;

        public Builder initialCapital(BigDecimal capital) {
            if (capital.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("initialCapital must be positive: " + capital);
            }
            this.initialCapital = capital;
            return this;
        }

        public Builder commission(BigDecimal commission) {
            if (commission.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("commission must be non-negative: " + commission);
            }
            this.commission = commission;
            return this;
        }

        public BacktestEngine build() {
            return new BacktestEngine(this);
        }
    }
}
