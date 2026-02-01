package com.stockdownloader.util;

import com.stockdownloader.model.PriceData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MovingAverageCalculatorTest {

    @Test
    void smaOfUniformPricesEqualsPrice() {
        List<PriceData> data = generatePriceData(10, 50.0, 0);
        BigDecimal sma = MovingAverageCalculator.sma(data, 9, 5);
        assertEquals(0, new BigDecimal("50").compareTo(sma.setScale(0, RoundingMode.HALF_UP)));
    }

    @Test
    void smaOfLinearlyIncreasingPrices() {
        // Prices: 1, 2, 3, 4, 5
        List<PriceData> data = generatePriceData(5, 1.0, 1.0);
        BigDecimal sma = MovingAverageCalculator.sma(data, 4, 5);
        // Average of 1,2,3,4,5 = 3
        assertEquals(0, new BigDecimal("3").compareTo(sma.setScale(0, RoundingMode.HALF_UP)));
    }

    @Test
    void smaWithSmallerWindow() {
        // Prices: 10, 20, 30, 40, 50
        List<PriceData> data = generatePriceData(5, 10.0, 10.0);
        BigDecimal sma = MovingAverageCalculator.sma(data, 4, 3);
        // Average of last 3: 30, 40, 50 = 40
        assertEquals(0, new BigDecimal("40").compareTo(sma.setScale(0, RoundingMode.HALF_UP)));
    }

    @Test
    void emaConvergesToPriceOnFlat() {
        List<PriceData> data = generatePriceData(20, 100.0, 0);
        BigDecimal ema = MovingAverageCalculator.ema(data, 19, 5);
        assertEquals(0, new BigDecimal("100").compareTo(ema.setScale(0, RoundingMode.HALF_UP)));
    }

    @Test
    void emaReactsToRecentPricesMoreThanSma() {
        List<PriceData> data = new ArrayList<>();
        // Flat at 100 for 15 days then jump to 200
        for (int i = 0; i < 15; i++) {
            data.add(makePriceData("d" + i, 100));
        }
        for (int i = 15; i < 20; i++) {
            data.add(makePriceData("d" + i, 200));
        }

        BigDecimal sma = MovingAverageCalculator.sma(data, 19, 10);
        BigDecimal ema = MovingAverageCalculator.ema(data, 19, 10);

        // EMA should be closer to 200 than SMA (more reactive)
        BigDecimal diff200Ema = new BigDecimal("200").subtract(ema).abs();
        BigDecimal diff200Sma = new BigDecimal("200").subtract(sma).abs();
        assertTrue(diff200Ema.compareTo(diff200Sma) < 0,
                "EMA should be closer to recent prices than SMA");
    }

    @Test
    void smaPeriodOf1EqualsLastPrice() {
        List<PriceData> data = generatePriceData(5, 10.0, 10.0);
        BigDecimal sma = MovingAverageCalculator.sma(data, 4, 1);
        assertEquals(0, new BigDecimal("50").compareTo(sma.setScale(0, RoundingMode.HALF_UP)));
    }

    private static List<PriceData> generatePriceData(int count, double start, double increment) {
        List<PriceData> data = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            data.add(makePriceData("d" + i, start + i * increment));
        }
        return data;
    }

    private static PriceData makePriceData(String date, double close) {
        BigDecimal p = BigDecimal.valueOf(close);
        return new PriceData(date, p, p, p, p, p, 1000);
    }
}
