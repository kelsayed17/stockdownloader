package com.stockdownloader.strategy;

import com.stockdownloader.model.OptionTrade;
import com.stockdownloader.model.OptionType;
import com.stockdownloader.model.PriceData;
import com.stockdownloader.model.Trade;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CoveredCallStrategyTest {

    @Test
    void defaultConstruction() {
        var strategy = new CoveredCallStrategy();
        assertEquals("Covered Call (50 SMA, 5% OTM, 30DTE)", strategy.getName());
        assertEquals(30, strategy.getTargetDTE());
        assertEquals(50, strategy.getWarmupPeriod());
    }

    @Test
    void customConstruction() {
        var strategy = new CoveredCallStrategy(20, new BigDecimal("3"), 45, 100);
        assertEquals("Covered Call (20 SMA, 3% OTM, 45DTE)", strategy.getName());
        assertEquals(45, strategy.getTargetDTE());
        assertEquals(100, strategy.getMinVolume());
    }

    @Test
    void holdDuringWarmup() {
        var strategy = new CoveredCallStrategy(5, new BigDecimal("5"), 30, 0);
        List<PriceData> data = generateRisingPrices(4, 100);
        assertEquals(OptionsStrategy.Action.HOLD, strategy.evaluate(data, 3, List.of()));
    }

    @Test
    void openWhenAboveSMA() {
        var strategy = new CoveredCallStrategy(5, new BigDecimal("5"), 30, 0);
        // Rising prices ensure current price is above 5-day SMA
        List<PriceData> data = generateRisingPrices(10, 100);
        OptionsStrategy.Action action = strategy.evaluate(data, 9, List.of());
        assertEquals(OptionsStrategy.Action.OPEN, action);
    }

    @Test
    void holdWhenBelowSMA() {
        var strategy = new CoveredCallStrategy(5, new BigDecimal("5"), 30, 0);
        // Falling prices ensure current price is below SMA
        List<PriceData> data = generateFallingPrices(10, 200);
        OptionsStrategy.Action action = strategy.evaluate(data, 9, List.of());
        assertEquals(OptionsStrategy.Action.HOLD, action);
    }

    @Test
    void holdWhenTradesAlreadyOpen() {
        var strategy = new CoveredCallStrategy(5, new BigDecimal("5"), 30, 0);
        List<PriceData> data = generateRisingPrices(10, 100);
        var openTrade = new OptionTrade(OptionType.CALL, Trade.Direction.SHORT,
                new BigDecimal("110"), "2099-12-31", "2024-01-01",
                new BigDecimal("3.00"), 1, 1000);
        OptionsStrategy.Action action = strategy.evaluate(data, 9, List.of(openTrade));
        assertEquals(OptionsStrategy.Action.HOLD, action);
    }

    @Test
    void createTradesProducesShortCall() {
        var strategy = new CoveredCallStrategy(5, new BigDecimal("5"), 30, 0);
        List<PriceData> data = generateRisingPrices(10, 100);
        List<OptionTrade> trades = strategy.createTrades(data, 9, new BigDecimal("100000"));

        assertEquals(1, trades.size());
        OptionTrade trade = trades.getFirst();
        assertEquals(OptionType.CALL, trade.getOptionType());
        assertEquals(Trade.Direction.SHORT, trade.getDirection());
        assertTrue(trade.getStrike().compareTo(data.get(9).close()) > 0,
                "Strike should be above current price (OTM)");
        assertTrue(trade.getContracts() > 0);
    }

    @Test
    void invalidSmaPeriodThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            new CoveredCallStrategy(0, new BigDecimal("5"), 30, 0);
        });
    }

    private static List<PriceData> generateRisingPrices(int count, double startPrice) {
        List<PriceData> data = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double price = startPrice + i * 2;
            BigDecimal p = BigDecimal.valueOf(price);
            data.add(new PriceData("2024-01-%02d".formatted(i + 1), p, p, p, p, p, 10000));
        }
        return data;
    }

    private static List<PriceData> generateFallingPrices(int count, double startPrice) {
        List<PriceData> data = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double price = startPrice - i * 2;
            BigDecimal p = BigDecimal.valueOf(price);
            data.add(new PriceData("2024-01-%02d".formatted(i + 1), p, p, p, p, p, 10000));
        }
        return data;
    }
}
