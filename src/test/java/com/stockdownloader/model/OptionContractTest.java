package com.stockdownloader.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OptionContractTest {

    private OptionContract sampleCall() {
        return new OptionContract(
                "SPY240119C00480000", OptionType.CALL,
                new BigDecimal("480.00"), "2024-01-19",
                new BigDecimal("5.50"), new BigDecimal("5.40"), new BigDecimal("5.60"),
                1500, 25000,
                new BigDecimal("0.18"),
                new BigDecimal("0.45"), new BigDecimal("0.02"),
                new BigDecimal("-0.15"), new BigDecimal("0.25"),
                false);
    }

    private OptionContract samplePut() {
        return new OptionContract(
                "SPY240119P00460000", OptionType.PUT,
                new BigDecimal("460.00"), "2024-01-19",
                new BigDecimal("3.20"), new BigDecimal("3.10"), new BigDecimal("3.30"),
                800, 18000,
                new BigDecimal("0.20"),
                new BigDecimal("-0.30"), new BigDecimal("0.01"),
                new BigDecimal("-0.12"), new BigDecimal("0.20"),
                false);
    }

    @Test
    void constructorValidatesNullFields() {
        assertThrows(NullPointerException.class, () ->
                new OptionContract(null, OptionType.CALL, BigDecimal.ONE, "2024-01-19",
                        BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 0, 0,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false));
    }

    @Test
    void constructorRejectsNegativeVolume() {
        assertThrows(IllegalArgumentException.class, () ->
                new OptionContract("SYM", OptionType.CALL, BigDecimal.ONE, "2024-01-19",
                        BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, -1, 0,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false));
    }

    @Test
    void constructorRejectsNegativeOpenInterest() {
        assertThrows(IllegalArgumentException.class, () ->
                new OptionContract("SYM", OptionType.CALL, BigDecimal.ONE, "2024-01-19",
                        BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 0, -1,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false));
    }

    @Test
    void midPriceCalculation() {
        OptionContract call = sampleCall();
        // (5.40 + 5.60) / 2 = 5.50
        assertEquals(new BigDecimal("5.5000"), call.midPrice());
    }

    @Test
    void spreadCalculation() {
        OptionContract call = sampleCall();
        // 5.60 - 5.40 = 0.20
        assertEquals(new BigDecimal("0.20"), call.spread());
    }

    @Test
    void notionalValueCalculation() {
        OptionContract call = sampleCall();
        // 5.50 * 100 = 550
        assertEquals(0, new BigDecimal("550.00").compareTo(call.notionalValue()));
    }

    @Test
    void recordAccessors() {
        OptionContract call = sampleCall();
        assertEquals("SPY240119C00480000", call.contractSymbol());
        assertEquals(OptionType.CALL, call.type());
        assertEquals(new BigDecimal("480.00"), call.strike());
        assertEquals("2024-01-19", call.expirationDate());
        assertEquals(1500, call.volume());
        assertEquals(25000, call.openInterest());
        assertFalse(call.inTheMoney());
    }

    @Test
    void putContractFields() {
        OptionContract put = samplePut();
        assertEquals(OptionType.PUT, put.type());
        assertEquals(new BigDecimal("460.00"), put.strike());
        assertEquals(800, put.volume());
        assertEquals(18000, put.openInterest());
    }

    @Test
    void toStringContainsKeyFields() {
        OptionContract call = sampleCall();
        String s = call.toString();
        assertTrue(s.contains("CALL"));
        assertTrue(s.contains("480"));
        assertTrue(s.contains("vol:1500"));
        assertTrue(s.contains("OI:25000"));
    }
}
