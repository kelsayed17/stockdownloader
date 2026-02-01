package com.stockdownloader.strategy;

import com.stockdownloader.model.PriceData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SMACrossoverStrategyTest {

    @Test
    void constructionWithValidPeriods() {
        var strategy = new SMACrossoverStrategy(10, 20);
        assertEquals("SMA Crossover (10/20)", strategy.getName());
        assertEquals(20, strategy.getWarmupPeriod());
    }

    @Test
    void shortPeriodGreaterThanLongThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new SMACrossoverStrategy(20, 10));
    }

    @Test
    void equalPeriodsThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new SMACrossoverStrategy(10, 10));
    }

    @Test
    void zeroPeriodThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new SMACrossoverStrategy(0, 10));
    }

    @Test
    void negativePeriodThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new SMACrossoverStrategy(-5, 10));
    }

    @Test
    void holdDuringWarmupPeriod() {
        var strategy = new SMACrossoverStrategy(2, 5);
        List<PriceData> data = generatePriceData(10, 100, 1);

        for (int i = 0; i < 5; i++) {
            assertEquals(TradingStrategy.Signal.HOLD, strategy.evaluate(data, i));
        }
    }

    @Test
    void buySignalOnGoldenCross() {
        var strategy = new SMACrossoverStrategy(3, 5);

        // Create data where short SMA crosses above long SMA
        List<PriceData> data = new ArrayList<>();
        // Declining prices (short < long)
        for (int i = 0; i < 10; i++) {
            data.add(makePriceData("day" + i, 100 - i));
        }
        // Sharp upturn
        for (int i = 10; i < 20; i++) {
            data.add(makePriceData("day" + i, 90 + (i - 10) * 5));
        }

        // Find the first BUY signal after warmup
        boolean foundBuy = false;
        for (int i = 5; i < data.size(); i++) {
            if (strategy.evaluate(data, i) == TradingStrategy.Signal.BUY) {
                foundBuy = true;
                break;
            }
        }
        assertTrue(foundBuy, "Should generate a BUY signal on golden cross");
    }

    @Test
    void sellSignalOnDeathCross() {
        var strategy = new SMACrossoverStrategy(3, 5);

        // Create data where short SMA crosses below long SMA
        List<PriceData> data = new ArrayList<>();
        // Rising prices (short > long)
        for (int i = 0; i < 10; i++) {
            data.add(makePriceData("day" + i, 100 + i * 3));
        }
        // Sharp downturn
        for (int i = 10; i < 20; i++) {
            data.add(makePriceData("day" + i, 130 - (i - 10) * 5));
        }

        boolean foundSell = false;
        for (int i = 5; i < data.size(); i++) {
            if (strategy.evaluate(data, i) == TradingStrategy.Signal.SELL) {
                foundSell = true;
                break;
            }
        }
        assertTrue(foundSell, "Should generate a SELL signal on death cross");
    }

    @Test
    void holdWhenNoSignalCrossover() {
        var strategy = new SMACrossoverStrategy(2, 3);
        // Flat prices -> no crossover
        List<PriceData> data = generatePriceData(10, 100, 0);

        for (int i = 3; i < data.size(); i++) {
            assertEquals(TradingStrategy.Signal.HOLD, strategy.evaluate(data, i));
        }
    }

    private static List<PriceData> generatePriceData(int count, double startPrice, double increment) {
        List<PriceData> data = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double price = startPrice + i * increment;
            data.add(makePriceData("2024-01-" + String.format("%02d", i + 1), price));
        }
        return data;
    }

    private static PriceData makePriceData(String date, double close) {
        BigDecimal p = BigDecimal.valueOf(close);
        return new PriceData(date, p, p, p, p, p, 1000);
    }
}
