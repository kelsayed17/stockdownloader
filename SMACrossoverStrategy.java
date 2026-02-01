import java.util.List;

/**
 * SMA Crossover strategy — generates buy/sell signals based on the crossover of
 * a short-period Simple Moving Average over a long-period SMA.
 *
 * <ul>
 *   <li><b>Golden Cross (BUY):</b> short SMA crosses above long SMA</li>
 *   <li><b>Death Cross (SELL):</b> short SMA crosses below long SMA</li>
 * </ul>
 *
 * <p>Common configurations: 50/200 (long-term trend), 20/50 (medium-term swing).</p>
 */
public final class SMACrossoverStrategy implements TradingStrategy {

    private final int shortPeriod;
    private final int longPeriod;

    public SMACrossoverStrategy(int shortPeriod, int longPeriod) {
        if (shortPeriod <= 0 || longPeriod <= 0) {
            throw new IllegalArgumentException("Periods must be positive: short=%d, long=%d"
                    .formatted(shortPeriod, longPeriod));
        }
        if (shortPeriod >= longPeriod) {
            throw new IllegalArgumentException("Short period (%d) must be less than long period (%d)"
                    .formatted(shortPeriod, longPeriod));
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
        validateEvaluationInputs(data, currentIndex);

        if (!isWarmedUp(currentIndex)) {
            return Signal.HOLD;
        }

        var currentShortSMA = TechnicalIndicators.sma(data, currentIndex, shortPeriod);
        var currentLongSMA = TechnicalIndicators.sma(data, currentIndex, longPeriod);
        var prevShortSMA = TechnicalIndicators.sma(data, currentIndex - 1, shortPeriod);
        var prevLongSMA = TechnicalIndicators.sma(data, currentIndex - 1, longPeriod);

        boolean shortAboveNow = currentShortSMA.compareTo(currentLongSMA) > 0;
        boolean shortAbovePrev = prevShortSMA.compareTo(prevLongSMA) > 0;

        if (shortAboveNow && !shortAbovePrev) return Signal.BUY;
        if (!shortAboveNow && shortAbovePrev) return Signal.SELL;

        return Signal.HOLD;
    }

    @Override
    public int getWarmupPeriod() {
        return longPeriod;
    }

    public int getShortPeriod() { return shortPeriod; }
    public int getLongPeriod()  { return longPeriod; }

    @Override
    public String toString() {
        return getName();
    }
}
