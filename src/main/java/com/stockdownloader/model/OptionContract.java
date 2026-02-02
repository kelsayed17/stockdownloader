package com.stockdownloader.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Represents a single options contract with full market data including
 * strike price, expiration, premium, volume, open interest, and Greeks.
 */
public final class OptionContract {

    private final String contractSymbol;
    private final String underlyingSymbol;
    private final OptionType optionType;
    private final BigDecimal strike;
    private final String expirationDate;
    private final BigDecimal lastPrice;
    private final BigDecimal bid;
    private final BigDecimal ask;
    private final long volume;
    private final long openInterest;
    private final Greeks greeks;
    private final boolean inTheMoney;

    public OptionContract(String contractSymbol, String underlyingSymbol, OptionType optionType,
                          BigDecimal strike, String expirationDate, BigDecimal lastPrice,
                          BigDecimal bid, BigDecimal ask, long volume, long openInterest,
                          Greeks greeks, boolean inTheMoney) {
        this.contractSymbol = Objects.requireNonNull(contractSymbol, "contractSymbol must not be null");
        this.underlyingSymbol = Objects.requireNonNull(underlyingSymbol, "underlyingSymbol must not be null");
        this.optionType = Objects.requireNonNull(optionType, "optionType must not be null");
        this.strike = Objects.requireNonNull(strike, "strike must not be null");
        this.expirationDate = Objects.requireNonNull(expirationDate, "expirationDate must not be null");
        this.lastPrice = Objects.requireNonNull(lastPrice, "lastPrice must not be null");
        this.bid = Objects.requireNonNull(bid, "bid must not be null");
        this.ask = Objects.requireNonNull(ask, "ask must not be null");
        if (volume < 0) throw new IllegalArgumentException("volume must not be negative");
        if (openInterest < 0) throw new IllegalArgumentException("openInterest must not be negative");
        this.volume = volume;
        this.openInterest = openInterest;
        this.greeks = Objects.requireNonNull(greeks, "greeks must not be null");
        this.inTheMoney = inTheMoney;
    }

    public BigDecimal getMidPrice() {
        if (bid.compareTo(BigDecimal.ZERO) == 0 && ask.compareTo(BigDecimal.ZERO) == 0) {
            return lastPrice;
        }
        return bid.add(ask).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getSpread() {
        return ask.subtract(bid);
    }

    public BigDecimal getIntrinsicValue(BigDecimal underlyingPrice) {
        if (optionType == OptionType.CALL) {
            BigDecimal diff = underlyingPrice.subtract(strike);
            return diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO;
        } else {
            BigDecimal diff = strike.subtract(underlyingPrice);
            return diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO;
        }
    }

    public BigDecimal getExtrinsicValue(BigDecimal underlyingPrice) {
        return lastPrice.subtract(getIntrinsicValue(underlyingPrice)).max(BigDecimal.ZERO);
    }

    public int getDaysToExpiration(String currentDate) {
        try {
            var current = java.time.LocalDate.parse(currentDate);
            var expiry = java.time.LocalDate.parse(expirationDate);
            return (int) java.time.temporal.ChronoUnit.DAYS.between(current, expiry);
        } catch (Exception e) {
            return 0;
        }
    }

    public String getContractSymbol() { return contractSymbol; }
    public String getUnderlyingSymbol() { return underlyingSymbol; }
    public OptionType getOptionType() { return optionType; }
    public BigDecimal getStrike() { return strike; }
    public String getExpirationDate() { return expirationDate; }
    public BigDecimal getLastPrice() { return lastPrice; }
    public BigDecimal getBid() { return bid; }
    public BigDecimal getAsk() { return ask; }
    public long getVolume() { return volume; }
    public long getOpenInterest() { return openInterest; }
    public Greeks getGreeks() { return greeks; }
    public boolean isInTheMoney() { return inTheMoney; }

    @Override
    public String toString() {
        return "%s %s %s $%s Exp:%s Last:$%s Bid:$%s Ask:$%s Vol:%d OI:%d %s".formatted(
                underlyingSymbol, optionType, contractSymbol,
                strike.setScale(2, RoundingMode.HALF_UP),
                expirationDate,
                lastPrice.setScale(2, RoundingMode.HALF_UP),
                bid.setScale(2, RoundingMode.HALF_UP),
                ask.setScale(2, RoundingMode.HALF_UP),
                volume, openInterest,
                inTheMoney ? "ITM" : "OTM");
    }
}
