import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a single trade in the backtest simulation.
 *
 * <p>State transitions follow a strict lifecycle: a trade is created OPEN via the
 * constructor, then transitions to CLOSED exactly once via {@link #close(LocalDate, BigDecimal)}.
 * Once closed, a trade is effectively immutable — all computed metrics (P/L, return %) are
 * calculated at close time and cached.</p>
 */
public final class Trade {

    public enum Direction { LONG, SHORT }
    public enum Status { OPEN, CLOSED }

    private static final int PRICE_SCALE = 6;
    private static final int DISPLAY_SCALE = 2;

    private final Direction direction;
    private final LocalDate entryDate;
    private final BigDecimal entryPrice;
    private final int shares;

    private Status status;
    private LocalDate exitDate;
    private BigDecimal exitPrice;
    private BigDecimal profitLoss;
    private BigDecimal returnPct;

    public Trade(Direction direction, LocalDate entryDate, BigDecimal entryPrice, int shares) {
        this.direction = Objects.requireNonNull(direction, "direction must not be null");
        this.entryDate = Objects.requireNonNull(entryDate, "entryDate must not be null");
        this.entryPrice = Objects.requireNonNull(entryPrice, "entryPrice must not be null");

        if (shares <= 0) {
            throw new IllegalArgumentException("shares must be positive: " + shares);
        }
        if (entryPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("entryPrice must be positive: " + entryPrice);
        }

        this.shares = shares;
        this.status = Status.OPEN;
        this.profitLoss = BigDecimal.ZERO;
        this.returnPct = BigDecimal.ZERO;
    }

    /**
     * Closes this trade, computing final P/L and return percentage.
     *
     * @throws IllegalStateException if the trade is already closed
     */
    public void close(LocalDate exitDate, BigDecimal exitPrice) {
        if (this.status == Status.CLOSED) {
            throw new IllegalStateException("Trade already closed on " + this.exitDate);
        }
        Objects.requireNonNull(exitDate, "exitDate must not be null");
        Objects.requireNonNull(exitPrice, "exitPrice must not be null");

        this.exitDate = exitDate;
        this.exitPrice = exitPrice;
        this.status = Status.CLOSED;

        BigDecimal priceDiff = switch (direction) {
            case LONG -> exitPrice.subtract(entryPrice);
            case SHORT -> entryPrice.subtract(exitPrice);
        };

        this.profitLoss = priceDiff.multiply(BigDecimal.valueOf(shares));
        this.returnPct = priceDiff
                .divide(entryPrice, PRICE_SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    public boolean isOpen() {
        return status == Status.OPEN;
    }

    public boolean isClosed() {
        return status == Status.CLOSED;
    }

    public boolean isWin() {
        return profitLoss.compareTo(BigDecimal.ZERO) > 0;
    }

    /** Duration in calendar days. Returns empty if trade is still open. */
    public Optional<Long> holdingDays() {
        if (exitDate == null) return Optional.empty();
        return Optional.of(java.time.temporal.ChronoUnit.DAYS.between(entryDate, exitDate));
    }

    // --- Getters (immutable entry fields return directly; exit fields use Optional for open trades) ---

    public Direction getDirection()      { return direction; }
    public Status getStatus()            { return status; }
    public LocalDate getEntryDate()      { return entryDate; }
    public BigDecimal getEntryPrice()    { return entryPrice; }
    public int getShares()               { return shares; }
    public BigDecimal getProfitLoss()    { return profitLoss; }
    public BigDecimal getReturnPct()     { return returnPct; }

    public Optional<LocalDate> getExitDate()    { return Optional.ofNullable(exitDate); }
    public Optional<BigDecimal> getExitPrice()  { return Optional.ofNullable(exitPrice); }

    @Override
    public String toString() {
        String exitDateStr = getExitDate().map(LocalDate::toString).orElse("N/A");
        String exitPriceStr = getExitPrice()
                .map(p -> "$" + p.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP))
                .orElse("N/A");

        return String.format("%s %s: Entry %s @ $%s -> Exit %s @ %s | P/L: $%s (%.2f%%)",
                direction, status,
                entryDate, entryPrice.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP),
                exitDateStr, exitPriceStr,
                profitLoss.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP),
                returnPct.doubleValue());
    }
}
