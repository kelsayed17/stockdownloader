package com.stockdownloader.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable representation of a single options contract with full greeks and volume data.
 */
public record OptionContract(
        String contractSymbol,
        OptionType type,
        BigDecimal strike,
        String expirationDate,
        BigDecimal lastPrice,
        BigDecimal bid,
        BigDecimal ask,
        long volume,
        long openInterest,
        BigDecimal impliedVolatility,
        BigDecimal delta,
        BigDecimal gamma,
        BigDecimal theta,
        BigDecimal vega,
        boolean inTheMoney) {

    public OptionContract {
        Objects.requireNonNull(contractSymbol, "contractSymbol must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(strike, "strike must not be null");
        Objects.requireNonNull(expirationDate, "expirationDate must not be null");
        Objects.requireNonNull(lastPrice, "lastPrice must not be null");
        Objects.requireNonNull(bid, "bid must not be null");
        Objects.requireNonNull(ask, "ask must not be null");
        Objects.requireNonNull(impliedVolatility, "impliedVolatility must not be null");
        Objects.requireNonNull(delta, "delta must not be null");
        Objects.requireNonNull(gamma, "gamma must not be null");
        Objects.requireNonNull(theta, "theta must not be null");
        Objects.requireNonNull(vega, "vega must not be null");
        if (volume < 0) {
            throw new IllegalArgumentException("volume must not be negative");
        }
        if (openInterest < 0) {
            throw new IllegalArgumentException("openInterest must not be negative");
        }
    }

    /**
     * Returns the mid-price between bid and ask.
     */
    public BigDecimal midPrice() {
        return bid.add(ask).divide(BigDecimal.valueOf(2), 4, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Returns the bid-ask spread.
     */
    public BigDecimal spread() {
        return ask.subtract(bid);
    }

    /**
     * Returns the notional value per contract (premium * 100 shares).
     */
    public BigDecimal notionalValue() {
        return lastPrice.multiply(BigDecimal.valueOf(100));
    }

    @Override
    public String toString() {
        return "%s %s $%s exp:%s last:$%s bid:$%s ask:$%s vol:%d OI:%d IV:%.2f%%".formatted(
                contractSymbol, type, strike, expirationDate,
                lastPrice, bid, ask, volume, openInterest,
                impliedVolatility.doubleValue() * 100);
    }
}
