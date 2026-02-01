import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Immutable value object representing a single day's OHLCV price bar.
 * Uses Java record for concise, immutable data carrier semantics.
 */
public record PriceData(
        LocalDate date,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal adjClose,
        long volume
) {
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    /** Compact constructor with null-safety and invariant validation. */
    public PriceData {
        Objects.requireNonNull(date, "date must not be null");
        Objects.requireNonNull(open, "open must not be null");
        Objects.requireNonNull(high, "high must not be null");
        Objects.requireNonNull(low, "low must not be null");
        Objects.requireNonNull(close, "close must not be null");
        Objects.requireNonNull(adjClose, "adjClose must not be null");

        if (volume < 0) {
            throw new IllegalArgumentException("volume must be non-negative: " + volume);
        }
    }

    /** Factory method parsing a Yahoo Finance CSV row (Date,Open,High,Low,Close,Adj Close,Volume). */
    public static PriceData fromCsvRow(String[] fields) {
        Objects.requireNonNull(fields, "CSV fields must not be null");
        if (fields.length < 5) {
            throw new IllegalArgumentException("CSV row requires at least 5 fields, got " + fields.length);
        }

        LocalDate date = LocalDate.parse(fields[0].trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        BigDecimal open = new BigDecimal(fields[1].trim());
        BigDecimal high = new BigDecimal(fields[2].trim());
        BigDecimal low = new BigDecimal(fields[3].trim());
        BigDecimal close = new BigDecimal(fields[4].trim());
        BigDecimal adjClose = fields.length > 5 ? new BigDecimal(fields[5].trim()) : close;
        long volume = fields.length > 6 ? Long.parseLong(fields[6].trim()) : 0L;

        return new PriceData(date, open, high, low, close, adjClose, volume);
    }

    /** Returns the daily price range (high - low). */
    public BigDecimal range() {
        return high.subtract(low);
    }

    /** Returns the body size |close - open|. */
    public BigDecimal bodySize() {
        return close.subtract(open).abs();
    }

    /** True if close >= open (bullish or flat candle). */
    public boolean isBullish() {
        return close.compareTo(open) >= 0;
    }

    @Override
    public String toString() {
        return String.format("%s O:%s H:%s L:%s C:%s V:%d",
                date.format(DISPLAY_FORMAT), open, high, low, close, volume);
    }
}
