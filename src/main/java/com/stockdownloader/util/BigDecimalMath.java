package com.stockdownloader.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for common BigDecimal arithmetic with safe division handling.
 */
public final class BigDecimalMath {

    private static final int DEFAULT_SCALE = 10;

    private BigDecimalMath() {}

    public static BigDecimal divide(BigDecimal dividend, BigDecimal divisor) {
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return dividend.divide(divisor, DEFAULT_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal divide(BigDecimal dividend, BigDecimal divisor, int scale) {
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return dividend.divide(divisor, scale, RoundingMode.HALF_UP);
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return a.multiply(b);
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return a.add(b);
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return a.subtract(b);
    }

    public static BigDecimal scale2(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal average(BigDecimal... values) {
        if (values == null || values.length == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (BigDecimal v : values) {
            if (v.compareTo(BigDecimal.ZERO) != 0) {
                sum = sum.add(v);
                count++;
            }
        }
        if (count == 0) {
            return BigDecimal.ZERO;
        }
        return sum.divide(BigDecimal.valueOf(count), DEFAULT_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal percentChange(BigDecimal from, BigDecimal to) {
        if (from.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return to.subtract(from)
                .divide(from, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
