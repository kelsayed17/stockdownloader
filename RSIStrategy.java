import java.math.BigDecimal;
import java.util.List;

/**
 * RSI (Relative Strength Index) mean-reversion strategy.
 *
 * <ul>
 *   <li><b>BUY:</b> RSI crosses above the oversold threshold (recovery from oversold)</li>
 *   <li><b>SELL:</b> RSI crosses below the overbought threshold (retreat from overbought)</li>
 * </ul>
 *
 * <p>Common configurations: period=14, oversold=30, overbought=70.</p>
 */
public final class RSIStrategy implements TradingStrategy {

    private final int period;
    private final BigDecimal oversoldThreshold;
    private final BigDecimal overboughtThreshold;

    public RSIStrategy(int period, double oversold, double overbought) {
        if (period <= 0) {
            throw new IllegalArgumentException("RSI period must be positive: " + period);
        }
        if (oversold < 0 || oversold > 100 || overbought < 0 || overbought > 100) {
            throw new IllegalArgumentException(
                    "Thresholds must be in [0, 100]: oversold=%s, overbought=%s"
                            .formatted(oversold, overbought));
        }
        if (oversold >= overbought) {
            throw new IllegalArgumentException(
                    "Oversold (%s) must be less than overbought (%s)".formatted(oversold, overbought));
        }

        this.period = period;
        this.oversoldThreshold = BigDecimal.valueOf(oversold);
        this.overboughtThreshold = BigDecimal.valueOf(overbought);
    }

    @Override
    public String getName() {
        return "RSI (%d) [%s/%s]".formatted(period, oversoldThreshold, overboughtThreshold);
    }

    @Override
    public Signal evaluate(List<PriceData> data, int currentIndex) {
        validateEvaluationInputs(data, currentIndex);

        if (!isWarmedUp(currentIndex)) {
            return Signal.HOLD;
        }

        var currentRSI = TechnicalIndicators.rsi(data, currentIndex, period);
        var prevRSI = TechnicalIndicators.rsi(data, currentIndex - 1, period);

        // Buy: RSI crosses above oversold threshold (recovery)
        if (currentRSI.compareTo(oversoldThreshold) > 0
                && prevRSI.compareTo(oversoldThreshold) <= 0) {
            return Signal.BUY;
        }

        // Sell: RSI crosses below overbought threshold (retreat)
        if (currentRSI.compareTo(overboughtThreshold) < 0
                && prevRSI.compareTo(overboughtThreshold) >= 0) {
            return Signal.SELL;
        }

        return Signal.HOLD;
    }

    @Override
    public int getWarmupPeriod() {
        return period + 1;
    }

    public int getPeriod()                    { return period; }
    public BigDecimal getOversoldThreshold()  { return oversoldThreshold; }
    public BigDecimal getOverboughtThreshold() { return overboughtThreshold; }

    @Override
    public String toString() {
        return getName();
    }
}
