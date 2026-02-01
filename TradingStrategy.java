import java.util.List;
import java.util.Objects;

/**
 * Strategy pattern interface for trading signal generation.
 *
 * <p>Implementations receive the full price history and current bar index, then return
 * a {@link Signal} indicating whether to buy, sell, or hold. Each strategy declares a
 * warmup period — the minimum number of bars required before signals become meaningful.</p>
 *
 * <p>Sealed to the known strategy implementations in this project, ensuring exhaustive
 * handling in switch expressions if needed.</p>
 */
public sealed interface TradingStrategy
        permits SMACrossoverStrategy, RSIStrategy, MACDStrategy {

    enum Signal {
        BUY,
        SELL,
        HOLD;

        public boolean isActionable() {
            return this != HOLD;
        }
    }

    /** Human-readable name for reports and logging. */
    String getName();

    /**
     * Evaluate the strategy at the given bar index.
     *
     * @param data         full price history (must not be null or empty)
     * @param currentIndex index of the bar being evaluated (0-based)
     * @return the trading signal; never null
     * @throws IndexOutOfBoundsException if currentIndex is out of range
     */
    Signal evaluate(List<PriceData> data, int currentIndex);

    /** Minimum number of bars needed before this strategy can produce meaningful signals. */
    int getWarmupPeriod();

    /** Returns true if the given index has enough history for a meaningful signal. */
    default boolean isWarmedUp(int currentIndex) {
        return currentIndex >= getWarmupPeriod();
    }

    /** Validates inputs common to all strategy evaluations. */
    default void validateEvaluationInputs(List<PriceData> data, int currentIndex) {
        Objects.requireNonNull(data, "price data must not be null");
        if (data.isEmpty()) {
            throw new IllegalArgumentException("price data must not be empty");
        }
        if (currentIndex < 0 || currentIndex >= data.size()) {
            throw new IndexOutOfBoundsException(
                    "currentIndex %d out of range [0, %d)".formatted(currentIndex, data.size()));
        }
    }
}
