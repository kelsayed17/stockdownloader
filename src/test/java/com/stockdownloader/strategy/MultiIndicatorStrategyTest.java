package com.stockdownloader.strategy;

import com.stockdownloader.model.PriceData;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the MultiIndicator confluence strategy.
 */
class MultiIndicatorStrategyTest {

    @Test
    void constructor_defaultValues() {
        var strategy = new MultiIndicatorStrategy();
        assertTrue(strategy.getName().contains("Multi-Indicator"));
    }

    @Test
    void constructor_invalidThreshold() {
        assertThrows(IllegalArgumentException.class,
                () -> new MultiIndicatorStrategy(0, 3));
    }

    @Test
    void evaluate_holdDuringWarmup() {
        var strategy = new MultiIndicatorStrategy();
        List<PriceData> data = generateTestData(300);
        assertEquals(TradingStrategy.Signal.HOLD, strategy.evaluate(data, 50));
    }

    @Test
    void evaluate_handlesEnoughData() {
        var strategy = new MultiIndicatorStrategy();
        List<PriceData> data = generateTestData(300);
        TradingStrategy.Signal signal = strategy.evaluate(data, 250);
        assertNotNull(signal);
    }

    @Test
    void getWarmupPeriod_isSufficient() {
        var strategy = new MultiIndicatorStrategy();
        assertEquals(201, strategy.getWarmupPeriod());
    }

    @Test
    void customThresholds() {
        var strategy = new MultiIndicatorStrategy(3, 3);
        assertTrue(strategy.getName().contains("Buy>=3"));
        assertTrue(strategy.getName().contains("Sell>=3"));
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
