import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class SMACrossoverStrategy implements TradingStrategy {

    private final int shortPeriod;
    private final int longPeriod;

    public SMACrossoverStrategy(int shortPeriod, int longPeriod) {
        if (shortPeriod <= 0 || longPeriod <= 0) {
            throw new IllegalArgumentException("Periods must be positive");
        }
        if (shortPeriod >= longPeriod) {
            throw new IllegalArgumentException("Short period must be less than long period");
        }
        this.shortPeriod = shortPeriod;
        this.longPeriod = longPeriod;
    }

    @Override
    public String getName() {
        return "SMA Crossover (%d/%d)".formatted(shortPeriod, longPeriod);
    }

    @Override
    public Signal evaluate(List<PriceData> data, int currentIndex) {
        if (currentIndex < longPeriod) {
            return Signal.HOLD;
        }

        BigDecimal currentShortSMA = calculateSMA(data, currentIndex, shortPeriod);
        BigDecimal currentLongSMA = calculateSMA(data, currentIndex, longPeriod);
        BigDecimal prevShortSMA = calculateSMA(data, currentIndex - 1, shortPeriod);
        BigDecimal prevLongSMA = calculateSMA(data, currentIndex - 1, longPeriod);

        boolean shortAboveLongNow = currentShortSMA.compareTo(currentLongSMA) > 0;
        boolean shortAboveLongPrev = prevShortSMA.compareTo(prevLongSMA) > 0;

        // Golden cross: short SMA crosses above long SMA
        if (shortAboveLongNow && !shortAboveLongPrev) {
            return Signal.BUY;
        }

        // Death cross: short SMA crosses below long SMA
        if (!shortAboveLongNow && shortAboveLongPrev) {
            return Signal.SELL;
        }

        return Signal.HOLD;
    }

    @Override
    public int getWarmupPeriod() {
        return longPeriod;
    }

    private BigDecimal calculateSMA(List<PriceData> data, int endIndex, int period) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = endIndex - period + 1; i <= endIndex; i++) {
            sum = sum.add(data.get(i).getClose());
        }
        return sum.divide(BigDecimal.valueOf(period), 6, RoundingMode.HALF_UP);
    }
}
