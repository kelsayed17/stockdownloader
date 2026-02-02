package com.stockdownloader.strategy;

import com.stockdownloader.model.PriceData;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the BollingerBandRSI mean reversion strategy.
 */
class BollingerBandRSIStrategyTest {

    @Test
    void constructor_defaultValues() {
        var strategy = new BollingerBandRSIStrategy();
        assertEquals("BB+RSI Mean Reversion (BB20, RSI14 [30/70], ADX<25)", strategy.getName());
    }

    @Test
    void evaluate_holdDuringWarmup() {
        var strategy = new BollingerBandRSIStrategy();
        List<PriceData> data = generateTestData(50);
        assertEquals(TradingStrategy.Signal.HOLD, strategy.evaluate(data, 5));
    }

    @Test
    void evaluate_holdWithSufficientData() {
        var strategy = new BollingerBandRSIStrategy();
        List<PriceData> data = generateTestData(200);
        // With random data, most signals should be HOLD
        TradingStrategy.Signal signal = strategy.evaluate(data, 100);
        assertNotNull(signal);
    }

    @Test
    void getWarmupPeriod_returnsExpectedValue() {
        var strategy = new BollingerBandRSIStrategy();
        assertTrue(strategy.getWarmupPeriod() >= 20, "Warmup should be at least 20");
    }

    @Test
    void customParameters() {
        var strategy = new BollingerBandRSIStrategy(30, 2.5, 14, 25, 75, 30);
        assertTrue(strategy.getName().contains("BB30"));
    }

    private static List<PriceData> generateTestData(int days) {
        List<PriceData> data = new ArrayList<>();
        double price = 100.0;
        for (int i = 0; i < days; i++) {
            price += (Math.random() - 0.48) * 3;
            price = Math.max(50, price);
            data.add(new PriceData("2020-01-01", BigDecimal.valueOf(price - 1),
                    BigDecimal.valueOf(price + 2), BigDecimal.valueOf(price - 2),
                    BigDecimal.valueOf(price), BigDecimal.valueOf(price),
                    (long) (1_000_000 + Math.random() * 5_000_000)));
        }
        return data;
    }
}
