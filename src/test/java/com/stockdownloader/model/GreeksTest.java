package com.stockdownloader.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class GreeksTest {

    @Test
    void createGreeks() {
        var greeks = new Greeks(
                new BigDecimal("0.55"), new BigDecimal("0.03"),
                new BigDecimal("-0.05"), new BigDecimal("0.15"),
                new BigDecimal("0.02"), new BigDecimal("0.25"));

        assertEquals(0, new BigDecimal("0.55").compareTo(greeks.delta()));
        assertEquals(0, new BigDecimal("0.03").compareTo(greeks.gamma()));
        assertEquals(0, new BigDecimal("-0.05").compareTo(greeks.theta()));
        assertEquals(0, new BigDecimal("0.15").compareTo(greeks.vega()));
        assertEquals(0, new BigDecimal("0.02").compareTo(greeks.rho()));
        assertEquals(0, new BigDecimal("0.25").compareTo(greeks.impliedVolatility()));
    }

    @Test
    void zeroGreeks() {
        var greeks = Greeks.zero();
        assertEquals(0, BigDecimal.ZERO.compareTo(greeks.delta()));
        assertEquals(0, BigDecimal.ZERO.compareTo(greeks.gamma()));
        assertEquals(0, BigDecimal.ZERO.compareTo(greeks.theta()));
        assertEquals(0, BigDecimal.ZERO.compareTo(greeks.vega()));
        assertEquals(0, BigDecimal.ZERO.compareTo(greeks.rho()));
        assertEquals(0, BigDecimal.ZERO.compareTo(greeks.impliedVolatility()));
    }

    @Test
    void nullDeltaThrows() {
        assertThrows(NullPointerException.class, () -> {
            new Greeks(null, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        });
    }

    @Test
    void toStringContainsValues() {
        var greeks = new Greeks(
                new BigDecimal("0.55"), new BigDecimal("0.03"),
                new BigDecimal("-0.05"), new BigDecimal("0.15"),
                new BigDecimal("0.02"), new BigDecimal("0.25"));
        String str = greeks.toString();
        assertTrue(str.contains("0.55"));
        assertTrue(str.contains("0.25"));
    }
}
