import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Trade {

    public enum Direction { LONG, SHORT }
    public enum Status { OPEN, CLOSED }

    private final Direction direction;
    private final String entryDate;
    private final BigDecimal entryPrice;
    private final int shares;

    private Status status;
    private String exitDate;
    private BigDecimal exitPrice;
    private BigDecimal profitLoss;
    private BigDecimal returnPct;

    public Trade(Direction direction, String entryDate, BigDecimal entryPrice, int shares) {
        this.direction = Objects.requireNonNull(direction, "direction must not be null");
        this.entryDate = Objects.requireNonNull(entryDate, "entryDate must not be null");
        this.entryPrice = Objects.requireNonNull(entryPrice, "entryPrice must not be null");
        if (shares <= 0) {
            throw new IllegalArgumentException("shares must be positive");
        }
        this.shares = shares;
        this.status = Status.OPEN;
        this.profitLoss = BigDecimal.ZERO;
        this.returnPct = BigDecimal.ZERO;
    }

    public void close(String exitDate, BigDecimal exitPrice) {
        Objects.requireNonNull(exitDate, "exitDate must not be null");
        Objects.requireNonNull(exitPrice, "exitPrice must not be null");
        if (this.status == Status.CLOSED) {
            throw new IllegalStateException("Trade is already closed");
        }

        this.exitDate = exitDate;
        this.exitPrice = exitPrice;
        this.status = Status.CLOSED;

        BigDecimal priceDiff = switch (direction) {
            case LONG -> exitPrice.subtract(entryPrice);
            case SHORT -> entryPrice.subtract(exitPrice);
        };

        this.profitLoss = priceDiff.multiply(BigDecimal.valueOf(shares));
        this.returnPct = priceDiff
                .divide(entryPrice, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    public boolean isWin() {
        return profitLoss.compareTo(BigDecimal.ZERO) > 0;
    }

    public Direction getDirection() { return direction; }
    public Status getStatus() { return status; }
    public String getEntryDate() { return entryDate; }
    public String getExitDate() { return exitDate; }
    public BigDecimal getEntryPrice() { return entryPrice; }
    public BigDecimal getExitPrice() { return exitPrice; }
    public int getShares() { return shares; }
    public BigDecimal getProfitLoss() { return profitLoss; }
    public BigDecimal getReturnPct() { return returnPct; }

    @Override
    public String toString() {
        return "%s %s: Entry %s @ $%s -> Exit %s @ $%s | P/L: $%s (%.2f%%)".formatted(
                direction, status,
                entryDate, entryPrice.setScale(2, RoundingMode.HALF_UP),
                exitDate != null ? exitDate : "N/A",
                exitPrice != null ? exitPrice.setScale(2, RoundingMode.HALF_UP) : "N/A",
                profitLoss.setScale(2, RoundingMode.HALF_UP),
                returnPct.doubleValue());
    }
}
