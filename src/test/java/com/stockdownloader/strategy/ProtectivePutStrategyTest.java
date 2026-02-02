package com.stockdownloader.strategy;

import com.stockdownloader.model.OptionType;
import com.stockdownloader.model.PriceData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProtectivePutStrategyTest {

    @Test
    void defaultConstructorCreatesValidStrategy() {
        var strategy = new ProtectivePutStrategy();
        assertNotNull(strategy.getName());
        assertTrue(strategy.getName().contains("Protective Put"));
        assertEquals(OptionType.PUT, strategy.getOptionType());
        assertFalse(strategy.isShort());
    }

    @Test
    void rejectsInvalidPeriod() {
        assertThrows(IllegalArgumentException.class, () ->
                new ProtectivePutStrategy(0, new BigDecimal("0.05"), 30, 5));
    }

    @Test
    void rejectsInvalidDTE() {
        assertThrows(IllegalArgumentException.class, () ->
                new ProtectivePutStrategy(20, new BigDecimal("0.05"), 0, 5));
    }

    @Test
    void rejectsInvalidMomentumLookback() {
        assertThrows(IllegalArgumentException.class, () ->
                new ProtectivePutStrategy(20, new BigDecimal("0.05"), 30, 0));
    }

    @Test
    void warmupPeriodIsMaxOfMaAndMomentum() {
        var strategy = new ProtectivePutStrategy(20, new BigDecimal("0.05"), 30, 10);
        assertEquals(20, strategy.getWarmupPeriod());

        var strategy2 = new ProtectivePutStrategy(5, new BigDecimal("0.05"), 30, 10);
        assertEquals(10, strategy2.getWarmupPeriod());
    }

    @Test
    void holdsDuringWarmup() {
        var strategy = new ProtectivePutStrategy(5, new BigDecimal("0.05"), 30, 3);
        List<PriceData> data = generatePrices(5, 100, -0.5);
        assertEquals(OptionsStrategy.Signal.HOLD, strategy.evaluate(data, 3));
    }

    @Test
    void targetStrikeBelowCurrentPrice() {
        var strategy = new ProtectivePutStrategy(20, new BigDecimal("0.05"), 45, 5);
        BigDecimal strike = strategy.getTargetStrike(new BigDecimal("475"));
        // 475 * 0.95 = 451.25 → floor to 451
        assertTrue(strike.compareTo(new BigDecimal("475")) < 0);
    }

    @Test
    void targetDaysToExpiry() {
        var strategy = new ProtectivePutStrategy(20, new BigDecimal("0.05"), 60, 5);
        assertEquals(60, strategy.getTargetDaysToExpiry());
    }

    @Test
    void isLongPutType() {
        var strategy = new ProtectivePutStrategy();
        assertFalse(strategy.isShort());
        assertEquals(OptionType.PUT, strategy.getOptionType());
    }

    @Test
    void signalOnDecline() {
        var strategy = new ProtectivePutStrategy(5, new BigDecimal("0.05"), 30, 3);
        List<PriceData> data = new ArrayList<>();
        // Start above MA
        for (int i = 0; i < 6; i++) {
            data.add(makePrice("2024-01-%02d".formatted(i + 1), 100 + i));
        }
        // Then drop below MA
        data.add(makePrice("2024-01-07", 90));

        OptionsStrategy.Signal signal = strategy.evaluate(data, data.size() - 1);
        assertNotNull(signal);
    }

    @Test
    void nameContainsParameters() {
        var strategy = new ProtectivePutStrategy(20, new BigDecimal("0.05"), 45, 5);
        assertTrue(strategy.getName().contains("MA20"));
        assertTrue(strategy.getName().contains("45DTE"));
    }

    private List<PriceData> generatePrices(int count, double start, double increment) {
        List<PriceData> data = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double price = start + i * increment;
            data.add(makePrice("2024-01-%02d".formatted(i + 1), price));
        }
        return data;
    }

    private PriceData makePrice(String date, double price) {
        BigDecimal p = BigDecimal.valueOf(price);
        return new PriceData(date, p, p.add(BigDecimal.ONE), p.subtract(BigDecimal.ONE), p, p, 100000);
    }
}
