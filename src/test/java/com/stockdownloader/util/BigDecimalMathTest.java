package com.stockdownloader.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BigDecimalMathTest {

    @Test
    void divideNormal() {
        BigDecimal result = BigDecimalMath.divide(BigDecimal.TEN, new BigDecimal("3"));
        assertTrue(result.compareTo(new BigDecimal("3.33")) > 0);
        assertTrue(result.compareTo(new BigDecimal("3.34")) < 0);
    }

    @Test
    void divideByZeroReturnsZero() {
        BigDecimal result = BigDecimalMath.divide(BigDecimal.TEN, BigDecimal.ZERO);
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void divideWithScale() {
        BigDecimal result = BigDecimalMath.divide(BigDecimal.TEN, new BigDecimal("3"), 2);
        assertEquals(0, new BigDecimal("3.33").compareTo(result));
    }

    @Test
    void divideByZeroWithScaleReturnsZero() {
        BigDecimal result = BigDecimalMath.divide(BigDecimal.TEN, BigDecimal.ZERO, 2);
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void multiply() {
        BigDecimal result = BigDecimalMath.multiply(new BigDecimal("5"), new BigDecimal("3"));
        assertEquals(0, new BigDecimal("15").compareTo(result));
    }

    @Test
    void add() {
        BigDecimal result = BigDecimalMath.add(new BigDecimal("7"), new BigDecimal("3"));
        assertEquals(0, BigDecimal.TEN.compareTo(result));
    }

    @Test
    void subtract() {
        BigDecimal result = BigDecimalMath.subtract(BigDecimal.TEN, new BigDecimal("3"));
        assertEquals(0, new BigDecimal("7").compareTo(result));
    }

    @Test
    void scale2() {
        BigDecimal result = BigDecimalMath.scale2(new BigDecimal("3.14159"));
        assertEquals(0, new BigDecimal("3.14").compareTo(result));
    }

    @Test
    void scale2RoundsUp() {
        BigDecimal result = BigDecimalMath.scale2(new BigDecimal("3.145"));
        assertEquals(0, new BigDecimal("3.15").compareTo(result));
    }

    @Test
    void averageOfValues() {
        BigDecimal result = BigDecimalMath.average(
                new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("30"));
        assertEquals(0, new BigDecimal("20").compareTo(result.setScale(0, java.math.RoundingMode.HALF_UP)));
    }

    @Test
    void averageIgnoresZeros() {
        BigDecimal result = BigDecimalMath.average(
                new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("30"));
        assertEquals(0, new BigDecimal("20").compareTo(result.setScale(0, java.math.RoundingMode.HALF_UP)));
    }

    @Test
    void averageOfAllZeros() {
        BigDecimal result = BigDecimalMath.average(BigDecimal.ZERO, BigDecimal.ZERO);
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void averageOfEmptyReturnsZero() {
        assertEquals(0, BigDecimal.ZERO.compareTo(BigDecimalMath.average()));
    }

    @Test
    void averageOfNullReturnsZero() {
        assertEquals(0, BigDecimal.ZERO.compareTo(BigDecimalMath.average((BigDecimal[]) null)));
    }

    @Test
    void percentChangePositive() {
        BigDecimal result = BigDecimalMath.percentChange(new BigDecimal("100"), new BigDecimal("120"));
        assertEquals(0, new BigDecimal("20").compareTo(result.setScale(0, java.math.RoundingMode.HALF_UP)));
    }

    @Test
    void percentChangeNegative() {
        BigDecimal result = BigDecimalMath.percentChange(new BigDecimal("100"), new BigDecimal("80"));
        assertEquals(0, new BigDecimal("-20").compareTo(result.setScale(0, java.math.RoundingMode.HALF_UP)));
    }

    @Test
    void percentChangeFromZero() {
        BigDecimal result = BigDecimalMath.percentChange(BigDecimal.ZERO, BigDecimal.TEN);
        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }
}
