package com.stockdownloader.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Full options chain for an underlying symbol, organized by expiration date.
 * Tracks calls and puts separately with aggregate volume metrics.
 */
public final class OptionsChain {

    private final String underlyingSymbol;
    private BigDecimal underlyingPrice = BigDecimal.ZERO;
    private final List<String> expirationDates = new ArrayList<>();
    private final Map<String, List<OptionContract>> callsByExpiration = new LinkedHashMap<>();
    private final Map<String, List<OptionContract>> putsByExpiration = new LinkedHashMap<>();

    public OptionsChain(String underlyingSymbol) {
        this.underlyingSymbol = Objects.requireNonNull(underlyingSymbol, "underlyingSymbol must not be null");
    }

    public void setUnderlyingPrice(BigDecimal price) {
        this.underlyingPrice = Objects.requireNonNull(price);
    }

    public void addExpirationDate(String date) {
        if (!expirationDates.contains(date)) {
            expirationDates.add(date);
        }
    }

    public void addCall(String expiration, OptionContract contract) {
        callsByExpiration.computeIfAbsent(expiration, k -> new ArrayList<>()).add(contract);
    }

    public void addPut(String expiration, OptionContract contract) {
        putsByExpiration.computeIfAbsent(expiration, k -> new ArrayList<>()).add(contract);
    }

    public List<OptionContract> getCalls(String expiration) {
        return List.copyOf(callsByExpiration.getOrDefault(expiration, List.of()));
    }

    public List<OptionContract> getPuts(String expiration) {
        return List.copyOf(putsByExpiration.getOrDefault(expiration, List.of()));
    }

    public List<OptionContract> getAllCalls() {
        return callsByExpiration.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    public List<OptionContract> getAllPuts() {
        return putsByExpiration.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    public long getTotalCallVolume() {
        return callsByExpiration.values().stream()
                .flatMap(List::stream)
                .mapToLong(OptionContract::volume)
                .sum();
    }

    public long getTotalPutVolume() {
        return putsByExpiration.values().stream()
                .flatMap(List::stream)
                .mapToLong(OptionContract::volume)
                .sum();
    }

    public long getTotalVolume() {
        return getTotalCallVolume() + getTotalPutVolume();
    }

    public long getTotalCallOpenInterest() {
        return callsByExpiration.values().stream()
                .flatMap(List::stream)
                .mapToLong(OptionContract::openInterest)
                .sum();
    }

    public long getTotalPutOpenInterest() {
        return putsByExpiration.values().stream()
                .flatMap(List::stream)
                .mapToLong(OptionContract::openInterest)
                .sum();
    }

    /**
     * Put/Call ratio based on volume. Values > 1 indicate bearish sentiment.
     */
    public BigDecimal getPutCallRatio() {
        long callVol = getTotalCallVolume();
        if (callVol == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(getTotalPutVolume())
                .divide(BigDecimal.valueOf(callVol), 4, RoundingMode.HALF_UP);
    }

    /**
     * Finds the nearest strike to a target price for a given expiration and type.
     */
    public Optional<OptionContract> findNearestStrike(String expiration, OptionType type, BigDecimal targetPrice) {
        List<OptionContract> contracts = type == OptionType.CALL
                ? getCalls(expiration) : getPuts(expiration);
        return contracts.stream()
                .min(Comparator.comparing(c -> c.strike().subtract(targetPrice).abs()));
    }

    /**
     * Gets contracts for a specific strike and expiration.
     */
    public List<OptionContract> getContractsAtStrike(String expiration, BigDecimal strike) {
        List<OptionContract> result = new ArrayList<>();
        for (OptionContract c : getCalls(expiration)) {
            if (c.strike().compareTo(strike) == 0) result.add(c);
        }
        for (OptionContract p : getPuts(expiration)) {
            if (p.strike().compareTo(strike) == 0) result.add(p);
        }
        return List.copyOf(result);
    }

    public String getUnderlyingSymbol() { return underlyingSymbol; }
    public BigDecimal getUnderlyingPrice() { return underlyingPrice; }
    public List<String> getExpirationDates() { return List.copyOf(expirationDates); }
    public Map<String, List<OptionContract>> getCallsByExpiration() { return Collections.unmodifiableMap(callsByExpiration); }
    public Map<String, List<OptionContract>> getPutsByExpiration() { return Collections.unmodifiableMap(putsByExpiration); }
}
