import java.math.BigDecimal;
import java.math.RoundingMode;

public class Trade {
    public enum Direction { LONG, SHORT }
    public enum Status { OPEN, CLOSED }

    private Direction direction;
    private Status status;
    private String entryDate;
    private String exitDate;
    private BigDecimal entryPrice;
    private BigDecimal exitPrice;
    private int shares;
    private BigDecimal profitLoss;
    private BigDecimal returnPct;

    public Trade(Direction direction, String entryDate, BigDecimal entryPrice, int shares) {
        this.direction = direction;
        this.entryDate = entryDate;
        this.entryPrice = entryPrice;
        this.shares = shares;
        this.status = Status.OPEN;
        this.profitLoss = BigDecimal.ZERO;
        this.returnPct = BigDecimal.ZERO;
    }

    public void close(String exitDate, BigDecimal exitPrice) {
        this.exitDate = exitDate;
        this.exitPrice = exitPrice;
        this.status = Status.CLOSED;

        if (direction == Direction.LONG) {
            this.profitLoss = exitPrice.subtract(entryPrice).multiply(BigDecimal.valueOf(shares));
            this.returnPct = exitPrice.subtract(entryPrice)
                    .divide(entryPrice, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        } else {
            this.profitLoss = entryPrice.subtract(exitPrice).multiply(BigDecimal.valueOf(shares));
            this.returnPct = entryPrice.subtract(exitPrice)
                    .divide(entryPrice, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
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
        return String.format("%s %s: Entry %s @ $%s -> Exit %s @ $%s | P/L: $%s (%.2f%%)",
                direction, status, entryDate, entryPrice.setScale(2, RoundingMode.HALF_UP),
                exitDate != null ? exitDate : "N/A",
                exitPrice != null ? exitPrice.setScale(2, RoundingMode.HALF_UP) : "N/A",
                profitLoss.setScale(2, RoundingMode.HALF_UP),
                returnPct.doubleValue());
    }
}
