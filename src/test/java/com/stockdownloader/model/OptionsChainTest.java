package com.stockdownloader.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OptionsChainTest {

    @Test
    void createOptionsChain() {
        var chain = makeTestChain();
        assertEquals("SPY", chain.getUnderlyingSymbol());
        assertEquals(0, new BigDecimal("500").compareTo(chain.getUnderlyingPrice()));
        assertEquals(1, chain.getExpirationDates().size());
        assertFalse(chain.getCalls().isEmpty());
        assertFalse(chain.getPuts().isEmpty());
    }

    @Test
    void getCallsForExpiration() {
        var chain = makeTestChain();
        List<OptionContract> calls = chain.getCallsForExpiration("2024-02-16");
        assertEquals(3, calls.size());
        // Should be sorted by strike
        assertTrue(calls.get(0).getStrike().compareTo(calls.get(1).getStrike()) < 0);
    }

    @Test
    void getPutsForExpiration() {
        var chain = makeTestChain();
        List<OptionContract> puts = chain.getPutsForExpiration("2024-02-16");
        assertEquals(3, puts.size());
    }

    @Test
    void findExactContract() {
        var chain = makeTestChain();
        Optional<OptionContract> call = chain.findContract(OptionType.CALL, "2024-02-16", new BigDecimal("500"));
        assertTrue(call.isPresent());
        assertEquals(0, new BigDecimal("500").compareTo(call.get().getStrike()));
    }

    @Test
    void findContractNotFound() {
        var chain = makeTestChain();
        Optional<OptionContract> call = chain.findContract(OptionType.CALL, "2024-02-16", new BigDecimal("999"));
        assertFalse(call.isPresent());
    }

    @Test
    void findNearestStrike() {
        var chain = makeTestChain();
        Optional<OptionContract> call = chain.findNearestStrike(OptionType.CALL, "2024-02-16", new BigDecimal("498"));
        assertTrue(call.isPresent());
        assertEquals(0, new BigDecimal("500").compareTo(call.get().getStrike()));
    }

    @Test
    void findATMOption() {
        var chain = makeTestChain();
        Optional<OptionContract> atm = chain.findATMOption(OptionType.CALL, "2024-02-16");
        assertTrue(atm.isPresent());
        assertEquals(0, new BigDecimal("500").compareTo(atm.get().getStrike()));
    }

    @Test
    void findOTMOption() {
        var chain = makeTestChain();
        // 5% OTM call on a $500 stock = $525 target
        Optional<OptionContract> otm = chain.findOTMOption(OptionType.CALL, "2024-02-16", new BigDecimal("5"));
        assertTrue(otm.isPresent());
        assertEquals(0, new BigDecimal("520").compareTo(otm.get().getStrike()));
    }

    @Test
    void getStrikesForExpiration() {
        var chain = makeTestChain();
        List<BigDecimal> strikes = chain.getStrikesForExpiration("2024-02-16");
        assertEquals(5, strikes.size()); // 480, 490, 500, 510, 520
    }

    @Test
    void totalCallVolume() {
        var chain = makeTestChain();
        assertTrue(chain.getTotalCallVolume() > 0);
    }

    @Test
    void totalPutVolume() {
        var chain = makeTestChain();
        assertTrue(chain.getTotalPutVolume() > 0);
    }

    @Test
    void putCallVolumeRatio() {
        var chain = makeTestChain();
        BigDecimal ratio = chain.getPutCallVolumeRatio();
        assertTrue(ratio.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void totalOpenInterest() {
        var chain = makeTestChain();
        assertTrue(chain.getTotalCallOpenInterest() > 0);
        assertTrue(chain.getTotalPutOpenInterest() > 0);
    }

    @Test
    void putCallOIRatio() {
        var chain = makeTestChain();
        BigDecimal ratio = chain.getPutCallOIRatio();
        assertTrue(ratio.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void maxPainStrike() {
        var chain = makeTestChain();
        BigDecimal maxPain = chain.getMaxPainStrike("2024-02-16");
        assertTrue(maxPain.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void toStringFormatsCorrectly() {
        var chain = makeTestChain();
        String str = chain.toString();
        assertTrue(str.contains("SPY"));
        assertTrue(str.contains("500"));
    }

    private static OptionsChain makeTestChain() {
        List<OptionContract> calls = List.of(
                makeContract(OptionType.CALL, "490", "12.00", 800, 4000, true),
                makeContract(OptionType.CALL, "500", "7.50", 1200, 6000, true),
                makeContract(OptionType.CALL, "520", "2.00", 500, 3000, false)
        );
        List<OptionContract> puts = List.of(
                makeContract(OptionType.PUT, "480", "1.50", 400, 2000, false),
                makeContract(OptionType.PUT, "500", "7.00", 900, 5000, true),
                makeContract(OptionType.PUT, "510", "12.50", 600, 3500, true)
        );
        return new OptionsChain("SPY", new BigDecimal("500"), "2024-01-15",
                List.of("2024-02-16"), calls, puts);
    }

    private static OptionContract makeContract(OptionType type, String strike, String lastPrice,
                                                long volume, long oi, boolean itm) {
        return new OptionContract("SPY240216" + (type == OptionType.CALL ? "C" : "P") + strike,
                "SPY", type,
                new BigDecimal(strike), "2024-02-16",
                new BigDecimal(lastPrice),
                new BigDecimal(lastPrice).subtract(new BigDecimal("0.50")),
                new BigDecimal(lastPrice).add(new BigDecimal("0.50")),
                volume, oi, Greeks.zero(), itm);
    }
}
