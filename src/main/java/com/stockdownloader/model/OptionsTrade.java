package com.stockdownloader.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Tracks an individual options trade from entry to exit.
 * Each contract represents 100 shares of the underlying.
 */
public final class OptionsTrade {

    public enum Direction { BUY, SELL }
    public enum Status { OPEN, CLOSED, EXPIRED }

    private static final int CONTRACT_MULTIPLIER = 100;

    private final OptionType optionType;
    private final Direction direction;
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

    public OptionsTrade(OptionType optionType, Direction direction, BigDecimal strike,
                        String expirationDate, String entryDate, BigDecimal entryPremium,
                        int contracts, long entryVolume) {
        this.optionType = Objects.requireNonNull(optionType, "optionType must not be null");
        this.direction = Objects.requireNonNull(direction, "direction must not be null");
        this.strike = Objects.requireNonNull(strike, "strike must not be null");
        this.expirationDate = Objects.requireNonNull(expirationDate, "expirationDate must not be null");
        this.entryDate = Objects.requireNonNull(entryDate, "entryDate must not be null");
        this.entryPremium = Objects.requireNonNull(entryPremium, "entryPremium must not be null");
        if (contracts <= 0) {
            throw new IllegalArgumentException("contracts must be positive");
        }
        this.contracts = contracts;
        this.entryVolume = entryVolume;
        this.status = Status.OPEN;
        this.profitLoss = BigDecimal.ZERO;
        this.returnPct = BigDecimal.ZERO;
    }

    /**
     * Close the trade at the given premium.
     */
    public void close(String exitDate, BigDecimal exitPremium) {
        Objects.requireNonNull(exitDate, "exitDate must not be null");
        Objects.requireNonNull(exitPremium, "exitPremium must not be null");
        if (this.status != Status.OPEN) {
            throw new IllegalStateException("Trade is not open, current status: " + status);
        }
        this.exitDate = exitDate;
        this.exitPremium = exitPremium;
        this.status = Status.CLOSED;
        calculateProfitLoss();
    }

    /**
     * Mark the trade as expired (option expired worthless or exercised).
     */
    public void expire(String expiryDate, BigDecimal settlementPremium) {
        Objects.requireNonNull(expiryDate, "expiryDate must not be null");
        Objects.requireNonNull(settlementPremium, "settlementPremium must not be null");
        if (this.status != Status.OPEN) {
            throw new IllegalStateException("Trade is not open, current status: " + status);
        }
        this.exitDate = expiryDate;
        this.exitPremium = settlementPremium;
        this.status = Status.EXPIRED;
        calculateProfitLoss();
    }

    private void calculateProfitLoss() {
        BigDecimal premiumDiff = switch (direction) {
            case BUY -> exitPremium.subtract(entryPremium);
            case SELL -> entryPremium.subtract(exitPremium);
        };
        this.profitLoss = premiumDiff
                .multiply(BigDecimal.valueOf(contracts))
                .multiply(BigDecimal.valueOf(CONTRACT_MULTIPLIER));

        BigDecimal totalCost = entryPremium
                .multiply(BigDecimal.valueOf(contracts))
                .multiply(BigDecimal.valueOf(CONTRACT_MULTIPLIER));
        if (totalCost.compareTo(BigDecimal.ZERO) != 0) {
            this.returnPct = profitLoss
                    .divide(totalCost, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
    }

    public boolean isWin() {
        return profitLoss.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Total premium paid/received at entry (contracts * 100 * premium).
     */
    public BigDecimal totalEntryCost() {
        return entryPremium.multiply(BigDecimal.valueOf((long) contracts * CONTRACT_MULTIPLIER));
    }

    public OptionType getOptionType() { return optionType; }
    public Direction getDirection() { return direction; }
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

    @Override
    public String toString() {
        return "%s %s %s $%s exp:%s | Entry:%s @$%s -> Exit:%s @$%s | P/L:$%s (%.2f%%) vol:%d".formatted(
                direction, optionType, status, strike.setScale(2, RoundingMode.HALF_UP),
                expirationDate,
                entryDate, entryPremium.setScale(2, RoundingMode.HALF_UP),
                exitDate != null ? exitDate : "N/A",
                exitPremium != null ? exitPremium.setScale(2, RoundingMode.HALF_UP) : "N/A",
                profitLoss.setScale(2, RoundingMode.HALF_UP),
                returnPct.doubleValue(),
                entryVolume);
    }
}
