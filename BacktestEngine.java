import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class BacktestEngine {
    private BigDecimal initialCapital;
    private BigDecimal commission;

    public BacktestEngine(BigDecimal initialCapital, BigDecimal commission) {
        this.initialCapital = initialCapital;
        this.commission = commission;
    }

    public BacktestResult run(TradingStrategy strategy, List<PriceData> data) {
        BacktestResult result = new BacktestResult(strategy.getName(), initialCapital);

        BigDecimal cash = initialCapital;
        Trade currentTrade = null;
        List<BigDecimal> equityCurve = new ArrayList<>();

        result.setStartDate(data.get(0).getDate());
        result.setEndDate(data.get(data.size() - 1).getDate());

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
                // Open a long position: use all available cash
                int shares = cash.subtract(commission)
                        .divide(bar.getClose(), 0, RoundingMode.DOWN)
                        .intValue();

                if (shares > 0) {
                    BigDecimal cost = bar.getClose().multiply(BigDecimal.valueOf(shares)).add(commission);
                    cash = cash.subtract(cost);
                    currentTrade = new Trade(Trade.Direction.LONG, bar.getDate(), bar.getClose(), shares);
                }

            } else if (signal == TradingStrategy.Signal.SELL && currentTrade != null
                    && currentTrade.getStatus() == Trade.Status.OPEN) {
                // Close the position
                BigDecimal proceeds = bar.getClose().multiply(BigDecimal.valueOf(currentTrade.getShares()))
                        .subtract(commission);
                cash = cash.add(proceeds);
                currentTrade.close(bar.getDate(), bar.getClose());
                result.addTrade(currentTrade);
                currentTrade = null;
            }
        }

        // Close any open position at the last bar
        if (currentTrade != null && currentTrade.getStatus() == Trade.Status.OPEN) {
            PriceData lastBar = data.get(data.size() - 1);
            BigDecimal proceeds = lastBar.getClose().multiply(BigDecimal.valueOf(currentTrade.getShares()))
                    .subtract(commission);
            cash = cash.add(proceeds);
            currentTrade.close(lastBar.getDate(), lastBar.getClose());
            result.addTrade(currentTrade);
        }

        result.setFinalCapital(cash);
        result.setEquityCurve(equityCurve);

        return result;
    }
}
