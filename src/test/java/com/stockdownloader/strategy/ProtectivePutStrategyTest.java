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

class ProtectivePutStrategyTest {

    @Test
    void defaultConstruction() {
        var strategy = new ProtectivePutStrategy();
        assertEquals("Protective Put (20 SMA, 3% OTM, 30DTE)", strategy.getName());
        assertEquals(30, strategy.getTargetDTE());
        assertEquals(20, strategy.getWarmupPeriod());
    }

    @Test
    void holdDuringWarmup() {
        var strategy = new ProtectivePutStrategy(5, new BigDecimal("3"), 30, 0, new BigDecimal("2"));
        List<PriceData> data = generatePrices(4, 100, 0);
        assertEquals(OptionsStrategy.Action.HOLD, strategy.evaluate(data, 3, List.of()));
    }

    @Test
    void openWhenBelowSMA() {
        var strategy = new ProtectivePutStrategy(5, new BigDecimal("3"), 30, 0, new BigDecimal("2"));
        // Falling prices ensure current price is below SMA
        List<PriceData> data = generatePrices(10, 200, -3);
        OptionsStrategy.Action action = strategy.evaluate(data, 9, List.of());
        assertEquals(OptionsStrategy.Action.OPEN, action);
    }

    @Test
    void holdWhenAboveSMA() {
        var strategy = new ProtectivePutStrategy(5, new BigDecimal("3"), 30, 0, new BigDecimal("2"));
        // Rising prices ensure current price is above SMA
        List<PriceData> data = generatePrices(10, 100, 3);
        OptionsStrategy.Action action = strategy.evaluate(data, 9, List.of());
        assertEquals(OptionsStrategy.Action.HOLD, action);
    }

    @Test
    void closeWhenPriceRecoversAboveSMA() {
        var strategy = new ProtectivePutStrategy(5, new BigDecimal("3"), 30, 0, new BigDecimal("2"));
        // Rising prices: price above SMA
        List<PriceData> data = generatePrices(10, 100, 3);
        var openTrade = new OptionTrade(OptionType.PUT, Trade.Direction.LONG,
                new BigDecimal("95"), "2099-12-31", "2024-01-01",
                new BigDecimal("3.00"), 1, 1000);
        OptionsStrategy.Action action = strategy.evaluate(data, 9, List.of(openTrade));
        assertEquals(OptionsStrategy.Action.CLOSE, action);
    }

    @Test
    void createTradesProducesLongPut() {
        var strategy = new ProtectivePutStrategy(5, new BigDecimal("3"), 30, 0, new BigDecimal("2"));
        List<PriceData> data = generatePrices(10, 200, -3);
        List<OptionTrade> trades = strategy.createTrades(data, 9, new BigDecimal("100000"));

        assertEquals(1, trades.size());
        OptionTrade trade = trades.getFirst();
        assertEquals(OptionType.PUT, trade.getOptionType());
        assertEquals(Trade.Direction.LONG, trade.getDirection());
        assertTrue(trade.getStrike().compareTo(data.get(9).close()) < 0,
                "Strike should be below current price (OTM put)");
    }

    private static List<PriceData> generatePrices(int count, double startPrice, double dailyChange) {
        List<PriceData> data = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double price = startPrice + i * dailyChange;
            BigDecimal p = BigDecimal.valueOf(price);
            data.add(new PriceData("2024-01-%02d".formatted(i + 1), p, p, p, p, p, 10000));
        }
        return data;
    }
}
