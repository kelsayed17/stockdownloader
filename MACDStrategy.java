import java.util.List;

/**
 * MACD (Moving Average Convergence Divergence) signal-line crossover strategy.
 *
 * <ul>
 *   <li><b>BUY:</b> MACD line crosses above the signal line (bullish momentum)</li>
 *   <li><b>SELL:</b> MACD line crosses below the signal line (bearish momentum)</li>
 * </ul>
 *
 * <p>Standard configuration: fast=12, slow=26, signal=9.</p>
 */
public final class MACDStrategy implements TradingStrategy {

    private final int fastPeriod;
    private final int slowPeriod;
    private final int signalPeriod;

    public MACDStrategy(int fastPeriod, int slowPeriod, int signalPeriod) {
        if (fastPeriod <= 0 || slowPeriod <= 0 || signalPeriod <= 0) {
            throw new IllegalArgumentException(
                    "All periods must be positive: fast=%d, slow=%d, signal=%d"
                            .formatted(fastPeriod, slowPeriod, signalPeriod));
        }
        if (fastPeriod >= slowPeriod) {
            throw new IllegalArgumentException(
                    "Fast period (%d) must be less than slow period (%d)"
                            .formatted(fastPeriod, slowPeriod));
        }

        this.fastPeriod = fastPeriod;
        this.slowPeriod = slowPeriod;
        this.signalPeriod = signalPeriod;
    }

    @Override
    public String getName() {
        return "MACD (%d/%d/%d)".formatted(fastPeriod, slowPeriod, signalPeriod);
    }

    @Override
    public Signal evaluate(List<PriceData> data, int currentIndex) {
        validateEvaluationInputs(data, currentIndex);

        if (!isWarmedUp(currentIndex)) {
            return Signal.HOLD;
        }

        var currentMACD = TechnicalIndicators.macdLine(data, currentIndex, fastPeriod, slowPeriod);
        var currentSignal = TechnicalIndicators.macdSignal(data, currentIndex, fastPeriod, slowPeriod, signalPeriod);
        var prevMACD = TechnicalIndicators.macdLine(data, currentIndex - 1, fastPeriod, slowPeriod);
        var prevSignal = TechnicalIndicators.macdSignal(data, currentIndex - 1, fastPeriod, slowPeriod, signalPeriod);

        boolean aboveNow = currentMACD.compareTo(currentSignal) > 0;
        boolean abovePrev = prevMACD.compareTo(prevSignal) > 0;

        if (aboveNow && !abovePrev) return Signal.BUY;
        if (!aboveNow && abovePrev) return Signal.SELL;

        return Signal.HOLD;
    }

    @Override
    public int getWarmupPeriod() {
        return slowPeriod + signalPeriod;
    }

    public int getFastPeriod()   { return fastPeriod; }
    public int getSlowPeriod()   { return slowPeriod; }
    public int getSignalPeriod() { return signalPeriod; }

    @Override
    public String toString() {
        return getName();
    }
}
