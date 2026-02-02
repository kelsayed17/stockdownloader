package com.stockdownloader.strategy;

import com.stockdownloader.model.PriceData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RSIStrategyTest {

    @Test
    void constructionWithValidParams() {
        var strategy = new RSIStrategy(14, 30, 70);
        assertEquals("RSI (14) [30.0/70.0]", strategy.getName());
        assertEquals(15, strategy.getWarmupPeriod());
    }

    @Test
    void zeroPeriodThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new RSIStrategy(0, 30, 70));
    }

    @Test
    void oversoldGreaterThanOverboughtThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new RSIStrategy(14, 80, 70));
    }

    @Test
    void equalThresholdsThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new RSIStrategy(14, 50, 50));
    }

    @Test
    void negativeOversoldThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new RSIStrategy(14, -10, 70));
    }

    @Test
    void overboughtAbove100Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new RSIStrategy(14, 30, 101));
    }

    @Test
    void holdDuringWarmupPeriod() {
        var strategy = new RSIStrategy(14, 30, 70);
        List<PriceData> data = generatePriceData(20, 100, 1);

        for (int i = 0; i <= 14; i++) {
            assertEquals(TradingStrategy.Signal.HOLD, strategy.evaluate(data, i));
        }
    }

    @Test
    void buySignalAfterOversoldRecovery() {
        var strategy = new RSIStrategy(5, 30, 70);

        // Create sharp decline followed by recovery (RSI goes from oversold back up)
        List<PriceData> data = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            data.add(makePriceData("day" + i, 100 - i * 8)); // Sharp decline
        }
        for (int i = 8; i < 15; i++) {
            data.add(makePriceData("day" + i, 36 + (i - 8) * 10)); // Recovery
        }

        boolean foundBuy = false;
        for (int i = 6; i < data.size(); i++) {
            if (strategy.evaluate(data, i) == TradingStrategy.Signal.BUY) {
                foundBuy = true;
                break;
            }
        }
        assertTrue(foundBuy, "Should generate BUY when RSI crosses above oversold threshold");
    }

    @Test
    void holdOnFlatPrices() {
        var strategy = new RSIStrategy(5, 30, 70);
        List<PriceData> data = generatePriceData(20, 100, 0);

        for (int i = 6; i < data.size(); i++) {
            assertEquals(TradingStrategy.Signal.HOLD, strategy.evaluate(data, i));
        }
    }

    @Test
    void rsiMaxWhenAllGains() {
        var strategy = new RSIStrategy(5, 30, 70);
        // Steady upward movement - RSI should be very high
        List<PriceData> data = generatePriceData(20, 50, 5);

        // After warmup, with all gains, RSI should be 100
        // So no BUY signal (above oversold), might generate SELL if crossing overbought
        boolean anyBuy = false;
        for (int i = 6; i < data.size(); i++) {
            if (strategy.evaluate(data, i) == TradingStrategy.Signal.BUY) {
                anyBuy = true;
            }
        }
        assertFalse(anyBuy, "No BUY signal when RSI is consistently high (all gains)");
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
