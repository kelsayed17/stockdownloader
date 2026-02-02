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

class IronCondorStrategyTest {

    @Test
    void defaultConstruction() {
        var strategy = new IronCondorStrategy();
        assertEquals("Iron Condor (20 SMA, 5%/3% width, 30DTE)", strategy.getName());
        assertEquals(30, strategy.getTargetDTE());
    }

    @Test
    void holdDuringWarmup() {
        var strategy = new IronCondorStrategy(5, new BigDecimal("5"), new BigDecimal("3"), 30, 0);
        List<PriceData> data = generateFlatPrices(4, 100);
        assertEquals(OptionsStrategy.Action.HOLD, strategy.evaluate(data, 3, List.of()));
    }

    @Test
    void openWhenRangeBound() {
        var strategy = new IronCondorStrategy(5, new BigDecimal("5"), new BigDecimal("3"), 30, 0);
        // Flat prices = near SMA = range-bound
        List<PriceData> data = generateFlatPrices(10, 100);
        OptionsStrategy.Action action = strategy.evaluate(data, 9, List.of());
        assertEquals(OptionsStrategy.Action.OPEN, action);
    }

    @Test
    void createTradesProducesFourLegs() {
        var strategy = new IronCondorStrategy(5, new BigDecimal("5"), new BigDecimal("3"), 30, 0);
        List<PriceData> data = generateFlatPrices(10, 500);
        List<OptionTrade> trades = strategy.createTrades(data, 9, new BigDecimal("100000"));

        assertEquals(4, trades.size());

        // Should have: short put, long put, short call, long call
        long shortPuts = trades.stream()
                .filter(t -> t.getOptionType() == OptionType.PUT && t.getDirection() == Trade.Direction.SHORT)
                .count();
        long longPuts = trades.stream()
                .filter(t -> t.getOptionType() == OptionType.PUT && t.getDirection() == Trade.Direction.LONG)
                .count();
        long shortCalls = trades.stream()
                .filter(t -> t.getOptionType() == OptionType.CALL && t.getDirection() == Trade.Direction.SHORT)
                .count();
        long longCalls = trades.stream()
                .filter(t -> t.getOptionType() == OptionType.CALL && t.getDirection() == Trade.Direction.LONG)
                .count();

        assertEquals(1, shortPuts, "Should have 1 short put");
        assertEquals(1, longPuts, "Should have 1 long put (wing)");
        assertEquals(1, shortCalls, "Should have 1 short call");
        assertEquals(1, longCalls, "Should have 1 long call (wing)");
    }

    @Test
    void strikeOrdering() {
        var strategy = new IronCondorStrategy(5, new BigDecimal("5"), new BigDecimal("3"), 30, 0);
        List<PriceData> data = generateFlatPrices(10, 500);
        List<OptionTrade> trades = strategy.createTrades(data, 9, new BigDecimal("100000"));

        BigDecimal longPutStrike = trades.stream()
                .filter(t -> t.getOptionType() == OptionType.PUT && t.getDirection() == Trade.Direction.LONG)
                .findFirst().get().getStrike();
        BigDecimal shortPutStrike = trades.stream()
                .filter(t -> t.getOptionType() == OptionType.PUT && t.getDirection() == Trade.Direction.SHORT)
                .findFirst().get().getStrike();
        BigDecimal shortCallStrike = trades.stream()
                .filter(t -> t.getOptionType() == OptionType.CALL && t.getDirection() == Trade.Direction.SHORT)
                .findFirst().get().getStrike();
        BigDecimal longCallStrike = trades.stream()
                .filter(t -> t.getOptionType() == OptionType.CALL && t.getDirection() == Trade.Direction.LONG)
                .findFirst().get().getStrike();

        // Long put < Short put < current price < Short call < Long call
        assertTrue(longPutStrike.compareTo(shortPutStrike) < 0, "Long put should be below short put");
        assertTrue(shortPutStrike.compareTo(new BigDecimal("500")) < 0, "Short put should be below price");
        assertTrue(shortCallStrike.compareTo(new BigDecimal("500")) > 0, "Short call should be above price");
        assertTrue(longCallStrike.compareTo(shortCallStrike) > 0, "Long call should be above short call");
    }

    @Test
    void closeWhenPriceBreachesShortStrike() {
        var strategy = new IronCondorStrategy(5, new BigDecimal("5"), new BigDecimal("3"), 30, 0);
        List<PriceData> data = generateFlatPrices(10, 500);

        // Create iron condor trades
        var shortPut = new OptionTrade(OptionType.PUT, Trade.Direction.SHORT,
                new BigDecimal("475"), "2099-12-31", "2024-01-01",
                new BigDecimal("3.00"), 1, 1000);
        var shortCall = new OptionTrade(OptionType.CALL, Trade.Direction.SHORT,
                new BigDecimal("525"), "2099-12-31", "2024-01-01",
                new BigDecimal("3.00"), 1, 1000);

        // Replace last bar with price above short call strike
        List<PriceData> breachedData = new ArrayList<>(data);
        BigDecimal high = new BigDecimal("530");
        breachedData.set(9, new PriceData("2024-01-10", high, high, high, high, high, 10000));

        OptionsStrategy.Action action = strategy.evaluate(breachedData, 9, List.of(shortPut, shortCall));
        assertEquals(OptionsStrategy.Action.CLOSE, action);
    }

    private static List<PriceData> generateFlatPrices(int count, double price) {
        List<PriceData> data = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            BigDecimal p = BigDecimal.valueOf(price);
            data.add(new PriceData("2024-01-%02d".formatted(i + 1), p, p, p, p, p, 10000));
        }
        return data;
    }
}
