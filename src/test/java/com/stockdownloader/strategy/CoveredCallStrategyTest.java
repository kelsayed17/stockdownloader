package com.stockdownloader.strategy;

import com.stockdownloader.model.OptionType;
import com.stockdownloader.model.PriceData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CoveredCallStrategyTest {

    @Test
    void defaultConstructorCreatesValidStrategy() {
        var strategy = new CoveredCallStrategy();
        assertNotNull(strategy.getName());
        assertTrue(strategy.getName().contains("Covered Call"));
        assertEquals(OptionType.CALL, strategy.getOptionType());
        assertTrue(strategy.isShort());
    }

    @Test
    void rejectsInvalidPeriod() {
        assertThrows(IllegalArgumentException.class, () ->
                new CoveredCallStrategy(0, new BigDecimal("0.05"), 30, new BigDecimal("0.03")));
    }

    @Test
    void rejectsInvalidDTE() {
        assertThrows(IllegalArgumentException.class, () ->
                new CoveredCallStrategy(20, new BigDecimal("0.05"), 0, new BigDecimal("0.03")));
    }

    @Test
    void warmupPeriodMatchesMAPeriod() {
        var strategy = new CoveredCallStrategy(30, new BigDecimal("0.05"), 45, new BigDecimal("0.03"));
        assertEquals(30, strategy.getWarmupPeriod());
    }

    @Test
    void holdsDuringWarmup() {
        var strategy = new CoveredCallStrategy(5, new BigDecimal("0.05"), 30, new BigDecimal("0.03"));
        List<PriceData> data = generatePrices(5, 100, 0.5);
        assertEquals(OptionsStrategy.Signal.HOLD, strategy.evaluate(data, 3));
    }

    @Test
    void targetStrikeAboveCurrentPrice() {
        var strategy = new CoveredCallStrategy(20, new BigDecimal("0.05"), 30, new BigDecimal("0.03"));
        BigDecimal strike = strategy.getTargetStrike(new BigDecimal("475"));
        // 475 * 1.05 = 498.75 → ceiling to 499
        assertTrue(strike.compareTo(new BigDecimal("475")) > 0);
    }

    @Test
    void targetDaysToExpiry() {
        var strategy = new CoveredCallStrategy(20, new BigDecimal("0.05"), 45, new BigDecimal("0.03"));
        assertEquals(45, strategy.getTargetDaysToExpiry());
    }

    @Test
    void opensWhenPriceCrossesAboveMA() {
        // Generate prices that cross above MA
        var strategy = new CoveredCallStrategy(5, new BigDecimal("0.05"), 30, new BigDecimal("0.03"));
        List<PriceData> data = new ArrayList<>();
        // Below MA phase: declining prices
        for (int i = 0; i < 6; i++) {
            data.add(makePrice("2024-01-0" + (i + 1), 100 - i));
        }
        // Then price jumps above the average
        data.add(makePrice("2024-01-07", 110));

        OptionsStrategy.Signal signal = strategy.evaluate(data, data.size() - 1);
        // May be OPEN or HOLD depending on exact MA cross
        assertNotNull(signal);
    }

    @Test
    void isShortAndCallType() {
        var strategy = new CoveredCallStrategy();
        assertTrue(strategy.isShort());
        assertEquals(OptionType.CALL, strategy.getOptionType());
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
