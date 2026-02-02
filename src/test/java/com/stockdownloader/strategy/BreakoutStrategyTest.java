package com.stockdownloader.strategy;

import com.stockdownloader.model.PriceData;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Breakout strategy.
 */
class BreakoutStrategyTest {

    @Test
    void constructor_defaultValues() {
        var strategy = new BreakoutStrategy();
        assertTrue(strategy.getName().contains("Breakout"));
    }

    @Test
    void evaluate_holdDuringWarmup() {
        var strategy = new BreakoutStrategy();
        List<PriceData> data = generateTestData(200);
        assertEquals(TradingStrategy.Signal.HOLD, strategy.evaluate(data, 10));
    }

    @Test
    void evaluate_handlesEnoughData() {
        var strategy = new BreakoutStrategy();
        List<PriceData> data = generateTestData(300);
        TradingStrategy.Signal signal = strategy.evaluate(data, 250);
        assertNotNull(signal);
    }

    @Test
    void getWarmupPeriod_coversSqueezeWindow() {
        var strategy = new BreakoutStrategy();
        assertTrue(strategy.getWarmupPeriod() >= 120,
                "Warmup should cover squeeze lookback");
    }

    @Test
    void customParameters() {
        var strategy = new BreakoutStrategy(30, 60, 2.0);
        assertTrue(strategy.getName().contains("BB30"));
        assertTrue(strategy.getName().contains("Squeeze60"));
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
