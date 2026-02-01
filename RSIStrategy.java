import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class RSIStrategy implements TradingStrategy {

    private static final BigDecimal RSI_MAX = BigDecimal.valueOf(100);

    private final int period;
    private final BigDecimal oversoldThreshold;
    private final BigDecimal overboughtThreshold;

    public RSIStrategy(int period, double oversold, double overbought) {
        if (period <= 0) {
            throw new IllegalArgumentException("Period must be positive");
        }
        if (oversold < 0 || overbought > 100 || oversold >= overbought) {
            throw new IllegalArgumentException("Invalid threshold values: oversold must be < overbought, within [0, 100]");
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
        if (currentIndex < period + 1) {
            return Signal.HOLD;
        }

        BigDecimal currentRSI = calculateRSI(data, currentIndex);
        BigDecimal prevRSI = calculateRSI(data, currentIndex - 1);

        // Buy when RSI crosses above oversold threshold (coming out of oversold)
        if (currentRSI.compareTo(oversoldThreshold) > 0 && prevRSI.compareTo(oversoldThreshold) <= 0) {
            return Signal.BUY;
        }

        // Sell when RSI crosses below overbought threshold (coming out of overbought)
        if (currentRSI.compareTo(overboughtThreshold) < 0 && prevRSI.compareTo(overboughtThreshold) >= 0) {
            return Signal.SELL;
        }

        return Signal.HOLD;
    }

    @Override
    public int getWarmupPeriod() {
        return period + 1;
    }

    private BigDecimal calculateRSI(List<PriceData> data, int endIndex) {
        BigDecimal avgGain = BigDecimal.ZERO;
        BigDecimal avgLoss = BigDecimal.ZERO;

        for (int i = endIndex - period + 1; i <= endIndex; i++) {
            BigDecimal change = data.get(i).getClose().subtract(data.get(i - 1).getClose());
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                avgGain = avgGain.add(change);
            } else {
                avgLoss = avgLoss.add(change.abs());
            }
        }

        BigDecimal periodBD = BigDecimal.valueOf(period);
        avgGain = avgGain.divide(periodBD, 10, RoundingMode.HALF_UP);
        avgLoss = avgLoss.divide(periodBD, 10, RoundingMode.HALF_UP);

        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
            return RSI_MAX;
        }

        BigDecimal rs = avgGain.divide(avgLoss, 10, RoundingMode.HALF_UP);
        return RSI_MAX.subtract(
                RSI_MAX.divide(BigDecimal.ONE.add(rs), 6, RoundingMode.HALF_UP));
    }
}
