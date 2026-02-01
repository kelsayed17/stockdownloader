import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

        result.setStartDate(data.getFirst().getDate());
        result.setEndDate(data.getLast().getDate());

        for (int i = 0; i < data.size(); i++) {
            PriceData bar = data.get(i);
            TradingStrategy.Signal signal = strategy.evaluate(data, i);

            // Calculate current equity
            BigDecimal equity = cash;
            if (currentTrade != null && currentTrade.getStatus() == Trade.Status.OPEN) {
                BigDecimal positionValue = bar.getClose().multiply(BigDecimal.valueOf(currentTrade.getShares()));
                equity = cash.add(positionValue);
            }
            equityCurve.add(equity);

            // Process signals
            if (signal == TradingStrategy.Signal.BUY && currentTrade == null) {
                int shares = cash.subtract(commission)
                        .divide(bar.getClose(), 0, RoundingMode.DOWN)
                        .intValue();

                if (shares > 0) {
                    BigDecimal cost = bar.getClose().multiply(BigDecimal.valueOf(shares)).add(commission);
                    cash = cash.subtract(cost);
                    currentTrade = new Trade(Trade.Direction.LONG, bar.getDate(), bar.getClose(), shares);
                }

            } else if (signal == TradingStrategy.Signal.SELL
                    && currentTrade != null
                    && currentTrade.getStatus() == Trade.Status.OPEN) {
                cash = closePosition(currentTrade, bar, cash);
                result.addTrade(currentTrade);
                currentTrade = null;
            }
        }

        // Close any open position at the last bar
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
        BigDecimal proceeds = bar.getClose()
                .multiply(BigDecimal.valueOf(trade.getShares()))
                .subtract(commission);
        trade.close(bar.getDate(), bar.getClose());
        return cash.add(proceeds);
    }
}
