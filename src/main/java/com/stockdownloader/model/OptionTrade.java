package com.stockdownloader.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Tracks an individual options trade from entry to exit, including
 * premium collected/paid, strike price, expiration, and P/L calculations.
 * One contract represents 100 shares of the underlying.
 */
public final class OptionTrade {

    public enum Status { OPEN, CLOSED_EXIT, CLOSED_EXPIRED, CLOSED_ASSIGNED }

    private static final int SHARES_PER_CONTRACT = 100;

    private final OptionType optionType;
    private final Trade.Direction direction; // LONG = bought, SHORT = sold/written
    private final BigDecimal strike;
    private final String expirationDate;
    private final String entryDate;
    private final BigDecimal entryPremium;
    private final int contracts;
    private final long entryVolume;

    private Status status;
    private String exitDate;
    private BigDecimal exitPremium;
    private BigDecimal profitLoss;
    private BigDecimal returnPct;
    private long exitVolume;

    public OptionTrade(OptionType optionType, Trade.Direction direction, BigDecimal strike,
                       String expirationDate, String entryDate, BigDecimal entryPremium,
                       int contracts, long entryVolume) {
        this.optionType = Objects.requireNonNull(optionType);
        this.direction = Objects.requireNonNull(direction);
        this.strike = Objects.requireNonNull(strike);
        this.expirationDate = Objects.requireNonNull(expirationDate);
        this.entryDate = Objects.requireNonNull(entryDate);
        this.entryPremium = Objects.requireNonNull(entryPremium);
        if (contracts <= 0) throw new IllegalArgumentException("contracts must be positive");
        this.contracts = contracts;
        this.entryVolume = entryVolume;
        this.status = Status.OPEN;
        this.profitLoss = BigDecimal.ZERO;
        this.returnPct = BigDecimal.ZERO;
    }

    public void closeAtPremium(String exitDate, BigDecimal exitPremium, long exitVolume) {
        Objects.requireNonNull(exitDate);
        Objects.requireNonNull(exitPremium);
        if (this.status != Status.OPEN) throw new IllegalStateException("Trade is already closed");

        this.exitDate = exitDate;
        this.exitPremium = exitPremium;
        this.exitVolume = exitVolume;
        this.status = Status.CLOSED_EXIT;
        computeProfitLoss();
    }

    public void closeAtExpiration(BigDecimal underlyingPriceAtExpiry) {
        if (this.status != Status.OPEN) throw new IllegalStateException("Trade is already closed");

        this.exitDate = expirationDate;
        BigDecimal intrinsicValue = computeIntrinsicValue(underlyingPriceAtExpiry);

        if (intrinsicValue.compareTo(BigDecimal.ZERO) > 0) {
            this.exitPremium = intrinsicValue;
            this.status = Status.CLOSED_ASSIGNED;
        } else {
            this.exitPremium = BigDecimal.ZERO;
            this.status = Status.CLOSED_EXPIRED;
        }
        computeProfitLoss();
    }

    private void computeProfitLoss() {
        BigDecimal multiplier = BigDecimal.valueOf((long) contracts * SHARES_PER_CONTRACT);
        if (direction == Trade.Direction.LONG) {
            profitLoss = exitPremium.subtract(entryPremium).multiply(multiplier);
        } else {
            profitLoss = entryPremium.subtract(exitPremium).multiply(multiplier);
        }

        BigDecimal cost = entryPremium.multiply(multiplier);
        if (cost.compareTo(BigDecimal.ZERO) != 0) {
            returnPct = profitLoss.divide(cost, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
    }

    private BigDecimal computeIntrinsicValue(BigDecimal underlyingPrice) {
        if (optionType == OptionType.CALL) {
            BigDecimal diff = underlyingPrice.subtract(strike);
            return diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO;
        } else {
            BigDecimal diff = strike.subtract(underlyingPrice);
            return diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO;
        }
    }

    public BigDecimal getMaxRisk() {
        BigDecimal multiplier = BigDecimal.valueOf((long) contracts * SHARES_PER_CONTRACT);
        if (direction == Trade.Direction.LONG) {
            return entryPremium.multiply(multiplier);
        } else {
            if (optionType == OptionType.PUT) {
                return strike.subtract(entryPremium).multiply(multiplier).max(BigDecimal.ZERO);
            }
            // Short call has theoretically unlimited risk; use strike as proxy
            return strike.multiply(multiplier);
        }
    }

    public boolean isWin() { return profitLoss.compareTo(BigDecimal.ZERO) > 0; }

    public OptionType getOptionType() { return optionType; }
    public Trade.Direction getDirection() { return direction; }
    public BigDecimal getStrike() { return strike; }
    public String getExpirationDate() { return expirationDate; }
    public String getEntryDate() { return entryDate; }
    public BigDecimal getEntryPremium() { return entryPremium; }
    public int getContracts() { return contracts; }
    public long getEntryVolume() { return entryVolume; }
    public Status getStatus() { return status; }
    public String getExitDate() { return exitDate; }
    public BigDecimal getExitPremium() { return exitPremium; }
    public BigDecimal getProfitLoss() { return profitLoss; }
    public BigDecimal getReturnPct() { return returnPct; }
    public long getExitVolume() { return exitVolume; }

    @Override
    public String toString() {
        return "%s %s %s Strike:$%s Exp:%s Entry:%s @$%s -> Exit:%s @$%s | P/L: $%s (%.2f%%) Vol:%d".formatted(
                direction, optionType, status,
                strike.setScale(2, RoundingMode.HALF_UP),
                expirationDate, entryDate,
                entryPremium.setScale(2, RoundingMode.HALF_UP),
                exitDate != null ? exitDate : "N/A",
                exitPremium != null ? exitPremium.setScale(2, RoundingMode.HALF_UP) : "N/A",
                profitLoss.setScale(2, RoundingMode.HALF_UP),
                returnPct.doubleValue(),
                entryVolume);
    }
}
