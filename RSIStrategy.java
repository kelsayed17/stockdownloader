import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class RSIStrategy implements TradingStrategy {
    private int period;
    private BigDecimal oversoldThreshold;
    private BigDecimal overboughtThreshold;

    public RSIStrategy(int period, double oversold, double overbought) {
        this.period = period;
        this.oversoldThreshold = BigDecimal.valueOf(oversold);
        this.overboughtThreshold = BigDecimal.valueOf(overbought);
    }

    @Override
    public String getName() {
        return "RSI (" + period + ") [" + oversoldThreshold + "/" + overboughtThreshold + "]";
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

        // Calculate initial average gain/loss
        for (int i = endIndex - period + 1; i <= endIndex; i++) {
            BigDecimal change = data.get(i).getClose().subtract(data.get(i - 1).getClose());
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                avgGain = avgGain.add(change);
            } else {
                avgLoss = avgLoss.add(change.abs());
            }
        }

        avgGain = avgGain.divide(BigDecimal.valueOf(period), 10, RoundingMode.HALF_UP);
        avgLoss = avgLoss.divide(BigDecimal.valueOf(period), 10, RoundingMode.HALF_UP);

        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
        }

        BigDecimal rs = avgGain.divide(avgLoss, 10, RoundingMode.HALF_UP);
        BigDecimal rsi = BigDecimal.valueOf(100).subtract(
                BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), 6, RoundingMode.HALF_UP));

        return rsi;
    }
}
