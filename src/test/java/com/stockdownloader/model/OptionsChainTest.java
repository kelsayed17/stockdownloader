package com.stockdownloader.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OptionsChainTest {

    private OptionsChain chain;

    @BeforeEach
    void setUp() {
        chain = new OptionsChain("SPY");
        chain.setUnderlyingPrice(new BigDecimal("475.00"));

        chain.addExpirationDate("2024-01-19");
        chain.addExpirationDate("2024-02-16");

        // Add calls for first expiration
        chain.addCall("2024-01-19", makeContract("C1", OptionType.CALL, "470", "2024-01-19", 500, 10000));
        chain.addCall("2024-01-19", makeContract("C2", OptionType.CALL, "480", "2024-01-19", 300, 8000));
        chain.addCall("2024-01-19", makeContract("C3", OptionType.CALL, "490", "2024-01-19", 200, 5000));

        // Add puts for first expiration
        chain.addPut("2024-01-19", makeContract("P1", OptionType.PUT, "460", "2024-01-19", 400, 12000));
        chain.addPut("2024-01-19", makeContract("P2", OptionType.PUT, "470", "2024-01-19", 600, 15000));

        // Add contracts for second expiration
        chain.addCall("2024-02-16", makeContract("C4", OptionType.CALL, "480", "2024-02-16", 150, 3000));
        chain.addPut("2024-02-16", makeContract("P3", OptionType.PUT, "460", "2024-02-16", 250, 7000));
    }

    @Test
    void basicProperties() {
        assertEquals("SPY", chain.getUnderlyingSymbol());
        assertEquals(new BigDecimal("475.00"), chain.getUnderlyingPrice());
        assertEquals(2, chain.getExpirationDates().size());
    }

    @Test
    void duplicateExpirationDatesIgnored() {
        chain.addExpirationDate("2024-01-19");
        assertEquals(2, chain.getExpirationDates().size());
    }

    @Test
    void callsAndPutsByExpiration() {
        assertEquals(3, chain.getCalls("2024-01-19").size());
        assertEquals(2, chain.getPuts("2024-01-19").size());
        assertEquals(1, chain.getCalls("2024-02-16").size());
        assertEquals(1, chain.getPuts("2024-02-16").size());
    }

    @Test
    void emptyExpirationReturnsEmptyList() {
        assertTrue(chain.getCalls("2099-12-31").isEmpty());
        assertTrue(chain.getPuts("2099-12-31").isEmpty());
    }

    @Test
    void totalCallVolume() {
        // 500 + 300 + 200 + 150 = 1150
        assertEquals(1150, chain.getTotalCallVolume());
    }

    @Test
    void totalPutVolume() {
        // 400 + 600 + 250 = 1250
        assertEquals(1250, chain.getTotalPutVolume());
    }

    @Test
    void totalVolume() {
        assertEquals(2400, chain.getTotalVolume());
    }

    @Test
    void totalOpenInterest() {
        // Calls: 10000 + 8000 + 5000 + 3000 = 26000
        assertEquals(26000, chain.getTotalCallOpenInterest());
        // Puts: 12000 + 15000 + 7000 = 34000
        assertEquals(34000, chain.getTotalPutOpenInterest());
    }

    @Test
    void putCallRatio() {
        // 1250 / 1150 = 1.0870
        BigDecimal ratio = chain.getPutCallRatio();
        assertTrue(ratio.doubleValue() > 1.0);
        assertTrue(ratio.doubleValue() < 1.2);
    }

    @Test
    void putCallRatioZeroCallVolume() {
        OptionsChain empty = new OptionsChain("TEST");
        assertEquals(BigDecimal.ZERO, empty.getPutCallRatio());
    }

    @Test
    void findNearestStrike() {
        Optional<OptionContract> nearest = chain.findNearestStrike(
                "2024-01-19", OptionType.CALL, new BigDecimal("477"));
        assertTrue(nearest.isPresent());
        assertEquals(new BigDecimal("480"), nearest.get().strike());
    }

    @Test
    void findNearestStrikeExactMatch() {
        Optional<OptionContract> nearest = chain.findNearestStrike(
                "2024-01-19", OptionType.PUT, new BigDecimal("470"));
        assertTrue(nearest.isPresent());
        assertEquals(new BigDecimal("470"), nearest.get().strike());
    }

    @Test
    void getContractsAtStrike() {
        var contracts = chain.getContractsAtStrike("2024-01-19", new BigDecimal("470"));
        assertEquals(2, contracts.size()); // one call and one put at 470
    }

    @Test
    void getAllCallsAndPuts() {
        assertEquals(4, chain.getAllCalls().size());
        assertEquals(3, chain.getAllPuts().size());
    }

    @Test
    void returnedListsAreImmutable() {
        assertThrows(UnsupportedOperationException.class, () ->
                chain.getExpirationDates().add("2024-03-15"));
        assertThrows(UnsupportedOperationException.class, () ->
                chain.getCalls("2024-01-19").clear());
    }

    private static OptionContract makeContract(String symbol, OptionType type, String strike,
                                               String exp, long volume, long oi) {
        return new OptionContract(
                symbol, type, new BigDecimal(strike), exp,
                new BigDecimal("5.00"), new BigDecimal("4.90"), new BigDecimal("5.10"),
                volume, oi,
                new BigDecimal("0.20"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                false);
    }
}
