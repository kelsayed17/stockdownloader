package com.stockdownloader.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PriceDataTest {

    @Test
    void createValidPriceData() {
        var pd = new PriceData("2024-01-01",
                new BigDecimal("100"), new BigDecimal("110"),
                new BigDecimal("95"), new BigDecimal("105"),
                new BigDecimal("105"), 1000L);

        assertEquals("2024-01-01", pd.date());
        assertEquals(new BigDecimal("100"), pd.open());
        assertEquals(new BigDecimal("110"), pd.high());
        assertEquals(new BigDecimal("95"), pd.low());
        assertEquals(new BigDecimal("105"), pd.close());
        assertEquals(new BigDecimal("105"), pd.adjClose());
        assertEquals(1000L, pd.volume());
    }

    @Test
    void nullDateThrows() {
        assertThrows(NullPointerException.class, () -> {
            new PriceData(null, BigDecimal.ONE, BigDecimal.ONE,
                    BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 0);
        });
    }

    @Test
    void nullCloseThrows() {
        assertThrows(NullPointerException.class, () -> {
            new PriceData("2024-01-01", BigDecimal.ONE, BigDecimal.ONE,
                    BigDecimal.ONE, null, BigDecimal.ONE, 0);
        });
    }

    @Test
    void negativeVolumeThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PriceData("2024-01-01", BigDecimal.ONE, BigDecimal.ONE,
                    BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, -1);
        });
    }

    @Test
    void zeroVolumeIsValid() {
        var pd = new PriceData("2024-01-01",
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.ONE, 0);
        assertEquals(0L, pd.volume());
    }

    @Test
    void toStringContainsAllFields() {
        var pd = new PriceData("2024-01-01",
                new BigDecimal("100"), new BigDecimal("110"),
                new BigDecimal("95"), new BigDecimal("105"),
                new BigDecimal("105"), 5000L);
        String str = pd.toString();
        assertTrue(str.contains("2024-01-01"));
        assertTrue(str.contains("100"));
        assertTrue(str.contains("5000"));
    }

    @Test
    void recordEquality() {
        var pd1 = new PriceData("2024-01-01",
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN,
                BigDecimal.TEN, BigDecimal.TEN, 100);
        var pd2 = new PriceData("2024-01-01",
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN,
                BigDecimal.TEN, BigDecimal.TEN, 100);
        assertEquals(pd1, pd2);
        assertEquals(pd1.hashCode(), pd2.hashCode());
    }
}
