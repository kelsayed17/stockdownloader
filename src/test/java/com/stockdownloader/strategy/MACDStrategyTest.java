package com.stockdownloader.strategy;

import com.stockdownloader.model.PriceData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MACDStrategyTest {

    @Test
    void constructionWithValidParams() {
        var strategy = new MACDStrategy(12, 26, 9);
        assertEquals("MACD (12/26/9)", strategy.getName());
        assertEquals(35, strategy.getWarmupPeriod());
    }

    @Test
    void fastGreaterThanSlowThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MACDStrategy(26, 12, 9);
        });
    }

    @Test
    void equalFastAndSlowThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MACDStrategy(12, 12, 9);
        });
    }

    @Test
    void zeroPeriodThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MACDStrategy(0, 26, 9);
        });
    }

    @Test
    void negativePeriodThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MACDStrategy(-1, 26, 9);
        });
    }

    @Test
    void holdDuringWarmupPeriod() {
        var strategy = new MACDStrategy(12, 26, 9);
        List<PriceData> data = generatePriceData(50, 100, 0.5);

        for (int i = 0; i < 35; i++) {
            assertEquals(TradingStrategy.Signal.HOLD, strategy.evaluate(data, i));
        }
    }

    @Test
    void producesSignalsAfterWarmup() {
        var strategy = new MACDStrategy(3, 7, 3);

        List<PriceData> data = new ArrayList<>();
        // Declining phase
        for (int i = 0; i < 20; i++) {
            data.add(makePriceData("day" + i, 100 - i * 2));
        }
        // Sharp recovery
        for (int i = 20; i < 40; i++) {
            data.add(makePriceData("day" + i, 60 + (i - 20) * 4));
        }

        boolean foundSignal = false;
        for (int i = 10; i < data.size(); i++) {
            TradingStrategy.Signal signal = strategy.evaluate(data, i);
            if (signal != TradingStrategy.Signal.HOLD) {
                foundSignal = true;
                break;
            }
        }
        assertTrue(foundSignal, "Should produce BUY or SELL signal after warmup with trend reversal");
    }

    @Test
    void holdOnFlatPrices() {
        var strategy = new MACDStrategy(3, 7, 3);
        List<PriceData> data = generatePriceData(30, 100, 0);

        for (int i = 10; i < data.size(); i++) {
            assertEquals(TradingStrategy.Signal.HOLD, strategy.evaluate(data, i));
        }
    }

    @Test
    void buySignalOnBullishCrossover() {
        var strategy = new MACDStrategy(3, 7, 3);

        List<PriceData> data = new ArrayList<>();
        // Decline
        for (int i = 0; i < 15; i++) {
            data.add(makePriceData("day" + i, 100 - i * 3));
        }
        // Strong reversal
        for (int i = 15; i < 30; i++) {
            data.add(makePriceData("day" + i, 55 + (i - 15) * 5));
        }

        boolean foundBuy = false;
        for (int i = 10; i < data.size(); i++) {
            if (strategy.evaluate(data, i) == TradingStrategy.Signal.BUY) {
                foundBuy = true;
                break;
            }
        }
        assertTrue(foundBuy, "Should produce BUY on bullish MACD crossover");
    }

    private static List<PriceData> generatePriceData(int count, double startPrice, double increment) {
        List<PriceData> data = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double price = Math.max(1, startPrice + i * increment);
            data.add(makePriceData("2024-01-" + String.format("%02d", i + 1), price));
        }
        return data;
    }

    private static PriceData makePriceData(String date, double close) {
        BigDecimal p = BigDecimal.valueOf(close);
        return new PriceData(date, p, p, p, p, p, 1000);
    }
}
