package com.stockdownloader.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a complete options chain for a given underlying symbol,
 * organized by expiration date and strike price with both calls and puts.
 */
public final class OptionsChain {

    private final String underlyingSymbol;
    private final BigDecimal underlyingPrice;
    private final String quoteDate;
    private final List<String> expirationDates;
    private final List<OptionContract> calls;
    private final List<OptionContract> puts;

    public OptionsChain(String underlyingSymbol, BigDecimal underlyingPrice, String quoteDate,
                        List<String> expirationDates, List<OptionContract> calls, List<OptionContract> puts) {
        this.underlyingSymbol = Objects.requireNonNull(underlyingSymbol);
        this.underlyingPrice = Objects.requireNonNull(underlyingPrice);
        this.quoteDate = Objects.requireNonNull(quoteDate);
        this.expirationDates = List.copyOf(expirationDates);
        this.calls = List.copyOf(calls);
        this.puts = List.copyOf(puts);
    }

    public List<OptionContract> getCallsForExpiration(String expiration) {
        return calls.stream()
                .filter(c -> c.getExpirationDate().equals(expiration))
                .sorted(Comparator.comparing(OptionContract::getStrike))
                .toList();
    }

    public List<OptionContract> getPutsForExpiration(String expiration) {
        return puts.stream()
                .filter(p -> p.getExpirationDate().equals(expiration))
                .sorted(Comparator.comparing(OptionContract::getStrike))
                .toList();
    }

    public Optional<OptionContract> findContract(OptionType type, String expiration, BigDecimal strike) {
        List<OptionContract> contracts = type == OptionType.CALL ? calls : puts;
        return contracts.stream()
                .filter(c -> c.getExpirationDate().equals(expiration))
                .filter(c -> c.getStrike().compareTo(strike) == 0)
                .findFirst();
    }

    public Optional<OptionContract> findNearestStrike(OptionType type, String expiration, BigDecimal targetStrike) {
        List<OptionContract> contracts = type == OptionType.CALL
                ? getCallsForExpiration(expiration) : getPutsForExpiration(expiration);
        if (contracts.isEmpty()) return Optional.empty();
        return contracts.stream()
                .min(Comparator.comparing(c -> c.getStrike().subtract(targetStrike).abs()));
    }

    public Optional<OptionContract> findATMOption(OptionType type, String expiration) {
        return findNearestStrike(type, expiration, underlyingPrice);
    }

    public Optional<OptionContract> findOTMOption(OptionType type, String expiration, BigDecimal percentOTM) {
        BigDecimal targetStrike;
        if (type == OptionType.CALL) {
            targetStrike = underlyingPrice.multiply(BigDecimal.ONE.add(percentOTM.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)));
        } else {
            targetStrike = underlyingPrice.multiply(BigDecimal.ONE.subtract(percentOTM.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)));
        }
        return findNearestStrike(type, expiration, targetStrike);
    }

    public List<BigDecimal> getStrikesForExpiration(String expiration) {
        Set<BigDecimal> strikes = new TreeSet<>();
        calls.stream().filter(c -> c.getExpirationDate().equals(expiration))
                .forEach(c -> strikes.add(c.getStrike()));
        puts.stream().filter(p -> p.getExpirationDate().equals(expiration))
                .forEach(p -> strikes.add(p.getStrike()));
        return new ArrayList<>(strikes);
    }

    public long getTotalCallVolume() {
        return calls.stream().mapToLong(OptionContract::getVolume).sum();
    }

    public long getTotalPutVolume() {
        return puts.stream().mapToLong(OptionContract::getVolume).sum();
    }

    public BigDecimal getPutCallVolumeRatio() {
        long callVol = getTotalCallVolume();
        if (callVol == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(getTotalPutVolume())
                .divide(BigDecimal.valueOf(callVol), 4, RoundingMode.HALF_UP);
    }

    public long getTotalCallOpenInterest() {
        return calls.stream().mapToLong(OptionContract::getOpenInterest).sum();
    }

    public long getTotalPutOpenInterest() {
        return puts.stream().mapToLong(OptionContract::getOpenInterest).sum();
    }

    public BigDecimal getPutCallOIRatio() {
        long callOI = getTotalCallOpenInterest();
        if (callOI == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(getTotalPutOpenInterest())
                .divide(BigDecimal.valueOf(callOI), 4, RoundingMode.HALF_UP);
    }

    public BigDecimal getMaxPainStrike(String expiration) {
        List<BigDecimal> strikes = getStrikesForExpiration(expiration);
        if (strikes.isEmpty()) return BigDecimal.ZERO;

        BigDecimal minPain = null;
        BigDecimal maxPainStrike = BigDecimal.ZERO;

        for (BigDecimal testStrike : strikes) {
            BigDecimal totalPain = BigDecimal.ZERO;

            for (OptionContract call : getCallsForExpiration(expiration)) {
                if (testStrike.compareTo(call.getStrike()) > 0) {
                    totalPain = totalPain.add(
                            testStrike.subtract(call.getStrike())
                                    .multiply(BigDecimal.valueOf(call.getOpenInterest()))
                                    .multiply(BigDecimal.valueOf(100)));
                }
            }
            for (OptionContract put : getPutsForExpiration(expiration)) {
                if (testStrike.compareTo(put.getStrike()) < 0) {
                    totalPain = totalPain.add(
                            put.getStrike().subtract(testStrike)
                                    .multiply(BigDecimal.valueOf(put.getOpenInterest()))
                                    .multiply(BigDecimal.valueOf(100)));
                }
            }

            if (minPain == null || totalPain.compareTo(minPain) < 0) {
                minPain = totalPain;
                maxPainStrike = testStrike;
            }
        }
        return maxPainStrike;
    }

    public String getUnderlyingSymbol() { return underlyingSymbol; }
    public BigDecimal getUnderlyingPrice() { return underlyingPrice; }
    public String getQuoteDate() { return quoteDate; }
    public List<String> getExpirationDates() { return expirationDates; }
    public List<OptionContract> getCalls() { return calls; }
    public List<OptionContract> getPuts() { return puts; }

    @Override
    public String toString() {
        return "OptionsChain[%s @ $%s, %d expirations, %d calls, %d puts]".formatted(
                underlyingSymbol,
                underlyingPrice.setScale(2, RoundingMode.HALF_UP),
                expirationDates.size(),
                calls.size(),
                puts.size());
    }
}
