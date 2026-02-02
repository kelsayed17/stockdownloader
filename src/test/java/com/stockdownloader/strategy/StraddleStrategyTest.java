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

class StraddleStrategyTest {

    @Test
    void defaultConstruction() {
        var strategy = new StraddleStrategy();
        assertEquals("Long Straddle (20 SMA, 30DTE, 50% target)", strategy.getName());
        assertEquals(30, strategy.getTargetDTE());
    }

    @Test
    void holdDuringWarmup() {
        var strategy = new StraddleStrategy(5, 30, 0, new BigDecimal("50"), new BigDecimal("0.04"));
        List<PriceData> data = generateFlatPrices(4, 100);
        assertEquals(OptionsStrategy.Action.HOLD, strategy.evaluate(data, 3, List.of()));
    }

    @Test
    void openWhenLowVolatility() {
        var strategy = new StraddleStrategy(5, 30, 0, new BigDecimal("50"), new BigDecimal("0.10"));
        // Flat prices = very low Bollinger Band width, below threshold
        List<PriceData> data = generateFlatPrices(10, 100);
        OptionsStrategy.Action action = strategy.evaluate(data, 9, List.of());
        assertEquals(OptionsStrategy.Action.OPEN, action);
    }

    @Test
    void createTradesProducesTwoLegs() {
        var strategy = new StraddleStrategy(5, 30, 0, new BigDecimal("50"), new BigDecimal("0.10"));
        List<PriceData> data = generateFlatPrices(10, 100);
        List<OptionTrade> trades = strategy.createTrades(data, 9, new BigDecimal("100000"));

        assertEquals(2, trades.size());

        OptionTrade callLeg = trades.stream()
                .filter(t -> t.getOptionType() == OptionType.CALL)
                .findFirst().orElseThrow();
        OptionTrade putLeg = trades.stream()
                .filter(t -> t.getOptionType() == OptionType.PUT)
                .findFirst().orElseThrow();

        assertEquals(Trade.Direction.LONG, callLeg.getDirection());
        assertEquals(Trade.Direction.LONG, putLeg.getDirection());
        // Same strike (ATM)
        assertEquals(0, callLeg.getStrike().compareTo(putLeg.getStrike()));
        // Same expiration
        assertEquals(callLeg.getExpirationDate(), putLeg.getExpirationDate());
    }

    @Test
    void closeAtExpiration() {
        var strategy = new StraddleStrategy(5, 30, 0, new BigDecimal("50"), new BigDecimal("0.10"));
        List<PriceData> data = generateFlatPrices(10, 100);

        var callTrade = new OptionTrade(OptionType.CALL, Trade.Direction.LONG,
                new BigDecimal("100"), "2024-01-05", "2024-01-01",
                new BigDecimal("3.00"), 1, 1000);
        var putTrade = new OptionTrade(OptionType.PUT, Trade.Direction.LONG,
                new BigDecimal("100"), "2024-01-05", "2024-01-01",
                new BigDecimal("3.00"), 1, 1000);

        // Date at index 9 is "2024-01-10", which is after expiration "2024-01-05"
        OptionsStrategy.Action action = strategy.evaluate(data, 9, List.of(callTrade, putTrade));
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
