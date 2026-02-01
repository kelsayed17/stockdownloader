import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Stateless utility class providing common technical indicator calculations.
 *
 * <p>All methods are static and side-effect-free. Extracted from individual strategy
 * classes to follow the DRY principle and Single Responsibility Principle.</p>
 *
 * <p>Precision is controlled via {@link #CALC_SCALE} (internal) and methods return
 * values at that precision. Callers can round further for display.</p>
 */
public final class TechnicalIndicators {

    /** Internal calculation scale for BigDecimal divisions. */
    static final int CALC_SCALE = 10;

    /** Standard number of trading days per year. */
    public static final int TRADING_DAYS_PER_YEAR = 252;

    private TechnicalIndicators() {
        throw new AssertionError("Utility class — do not instantiate");
    }

    // ---- Simple Moving Average ----

    /**
     * Calculates the Simple Moving Average ending at {@code endIndex} over {@code period} bars.
     *
     * @param data     price history
     * @param endIndex last bar index (inclusive)
     * @param period   number of bars to average
     * @return the SMA value
     */
    public static BigDecimal sma(List<PriceData> data, int endIndex, int period) {
        validateIndicatorInputs(data, endIndex, period);

        BigDecimal sum = BigDecimal.ZERO;
        int startIndex = endIndex - period + 1;
        for (int i = startIndex; i <= endIndex; i++) {
            sum = sum.add(data.get(i).close());
        }
        return sum.divide(BigDecimal.valueOf(period), CALC_SCALE, RoundingMode.HALF_UP);
    }

    // ---- Exponential Moving Average ----

    /**
     * Calculates the Exponential Moving Average ending at {@code endIndex} over {@code period} bars.
     * Uses the first {@code period} bars as an SMA seed, then applies the EMA smoothing formula.
     *
     * @param data     price history
     * @param endIndex last bar index (inclusive)
     * @param period   EMA period
     * @return the EMA value
     */
    public static BigDecimal ema(List<PriceData> data, int endIndex, int period) {
        validateIndicatorInputs(data, endIndex, period);

        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (period + 1));
        BigDecimal complement = BigDecimal.ONE.subtract(multiplier);

        // Use the earliest possible window as SMA seed
        int seedStart = Math.max(0, endIndex - period * 2);
        int seedEnd = Math.min(seedStart + period - 1, endIndex);

        BigDecimal sum = BigDecimal.ZERO;
        for (int i = seedStart; i <= seedEnd; i++) {
            sum = sum.add(data.get(i).close());
        }
        BigDecimal ema = sum.divide(BigDecimal.valueOf(seedEnd - seedStart + 1), CALC_SCALE, RoundingMode.HALF_UP);

        // Apply EMA formula forward from the bar after the seed
        for (int i = seedEnd + 1; i <= endIndex; i++) {
            ema = data.get(i).close().multiply(multiplier)
                    .add(ema.multiply(complement))
                    .setScale(CALC_SCALE, RoundingMode.HALF_UP);
        }

        return ema;
    }

    // ---- Relative Strength Index ----

    /**
     * Calculates the RSI ending at {@code endIndex} using Wilder's smoothed method.
     *
     * @param data     price history
     * @param endIndex last bar index (inclusive)
     * @param period   RSI lookback period (typically 14)
     * @return RSI value between 0 and 100
     */
    public static BigDecimal rsi(List<PriceData> data, int endIndex, int period) {
        validateIndicatorInputs(data, endIndex, period);
        if (endIndex < period) {
            throw new IllegalArgumentException(
                    "endIndex (%d) must be >= period (%d) for RSI calculation".formatted(endIndex, period));
        }

        BigDecimal avgGain = BigDecimal.ZERO;
        BigDecimal avgLoss = BigDecimal.ZERO;

        // Initial SMA of gains/losses over the first 'period' changes
        int startIndex = endIndex - period + 1;
        for (int i = startIndex; i <= endIndex; i++) {
            BigDecimal change = data.get(i).close().subtract(data.get(i - 1).close());
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                avgGain = avgGain.add(change);
            } else {
                avgLoss = avgLoss.add(change.abs());
            }
        }

        avgGain = avgGain.divide(BigDecimal.valueOf(period), CALC_SCALE, RoundingMode.HALF_UP);
        avgLoss = avgLoss.divide(BigDecimal.valueOf(period), CALC_SCALE, RoundingMode.HALF_UP);

        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
        }

        BigDecimal rs = avgGain.divide(avgLoss, CALC_SCALE, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(100).subtract(
                BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), CALC_SCALE, RoundingMode.HALF_UP));
    }

    // ---- MACD Components ----

    /**
     * Calculates the MACD line (fast EMA - slow EMA).
     *
     * @param data       price history
     * @param endIndex   last bar index (inclusive)
     * @param fastPeriod fast EMA period (e.g. 12)
     * @param slowPeriod slow EMA period (e.g. 26)
     * @return the MACD line value
     */
    public static BigDecimal macdLine(List<PriceData> data, int endIndex, int fastPeriod, int slowPeriod) {
        return ema(data, endIndex, fastPeriod).subtract(ema(data, endIndex, slowPeriod));
    }

    /**
     * Calculates the MACD signal line (EMA of MACD line values).
     *
     * @param data         price history
     * @param endIndex     last bar index (inclusive)
     * @param fastPeriod   fast EMA period
     * @param slowPeriod   slow EMA period
     * @param signalPeriod signal EMA period (e.g. 9)
     * @return the signal line value
     */
    public static BigDecimal macdSignal(List<PriceData> data, int endIndex,
                                         int fastPeriod, int slowPeriod, int signalPeriod) {
        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (signalPeriod + 1));
        BigDecimal complement = BigDecimal.ONE.subtract(multiplier);

        int windowStart = Math.max(slowPeriod, endIndex - signalPeriod + 1);

        // SMA seed of MACD values
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        int seedEnd = Math.min(windowStart + signalPeriod - 1, endIndex);
        for (int i = windowStart; i <= seedEnd; i++) {
            sum = sum.add(macdLine(data, i, fastPeriod, slowPeriod));
            count++;
        }
        if (count == 0) return BigDecimal.ZERO;

        BigDecimal signalEma = sum.divide(BigDecimal.valueOf(count), CALC_SCALE, RoundingMode.HALF_UP);

        // Apply EMA forward
        for (int i = seedEnd + 1; i <= endIndex; i++) {
            BigDecimal macd = macdLine(data, i, fastPeriod, slowPeriod);
            signalEma = macd.multiply(multiplier)
                    .add(signalEma.multiply(complement))
                    .setScale(CALC_SCALE, RoundingMode.HALF_UP);
        }

        return signalEma;
    }

    /**
     * Calculates the MACD histogram (MACD line - signal line).
     */
    public static BigDecimal macdHistogram(List<PriceData> data, int endIndex,
                                            int fastPeriod, int slowPeriod, int signalPeriod) {
        return macdLine(data, endIndex, fastPeriod, slowPeriod)
                .subtract(macdSignal(data, endIndex, fastPeriod, slowPeriod, signalPeriod));
    }

    // ---- Validation ----

    private static void validateIndicatorInputs(List<PriceData> data, int endIndex, int period) {
        Objects.requireNonNull(data, "price data must not be null");
        if (period <= 0) {
            throw new IllegalArgumentException("period must be positive: " + period);
        }
        if (endIndex < 0 || endIndex >= data.size()) {
            throw new IndexOutOfBoundsException(
                    "endIndex %d out of range [0, %d)".formatted(endIndex, data.size()));
        }
    }
}
