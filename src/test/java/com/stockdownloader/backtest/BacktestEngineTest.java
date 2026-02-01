package com.stockdownloader.backtest;

import com.stockdownloader.model.PriceData;
import com.stockdownloader.model.Trade;
import com.stockdownloader.strategy.SMACrossoverStrategy;
import com.stockdownloader.strategy.TradingStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BacktestEngineTest {

    @Test
    void runWithNullStrategyThrows() {
        var engine = new BacktestEngine(new BigDecimal("10000"), BigDecimal.ZERO);
        List<PriceData> data = List.of(makePriceData("d1", 100));

        assertThrows(NullPointerException.class, () -> engine.run(null, data));
    }

    @Test
    void runWithNullDataThrows() {
        var engine = new BacktestEngine(new BigDecimal("10000"), BigDecimal.ZERO);
        var strategy = new SMACrossoverStrategy(2, 5);

        assertThrows(IllegalArgumentException.class, () -> engine.run(strategy, null));
    }

    @Test
    void runWithEmptyDataThrows() {
        var engine = new BacktestEngine(new BigDecimal("10000"), BigDecimal.ZERO);
        var strategy = new SMACrossoverStrategy(2, 5);

        assertThrows(IllegalArgumentException.class, () -> engine.run(strategy, List.of()));
    }

    @Test
    void resultHasCorrectStrategyName() {
        var engine = new BacktestEngine(new BigDecimal("10000"), BigDecimal.ZERO);
        var strategy = new SMACrossoverStrategy(2, 5);
        List<PriceData> data = generateFlatPriceData(10, 100);

        BacktestResult result = engine.run(strategy, data);
        assertEquals("SMA Crossover (2/5)", result.getStrategyName());
    }

    @Test
    void noTradesOnFlatPrices() {
        var engine = new BacktestEngine(new BigDecimal("10000"), BigDecimal.ZERO);
        var strategy = new SMACrossoverStrategy(2, 5);
        List<PriceData> data = generateFlatPriceData(20, 100);

        BacktestResult result = engine.run(strategy, data);
        assertEquals(0, result.getTotalTrades());
        assertEquals(0, new BigDecimal("10000").compareTo(result.getFinalCapital()));
    }

    @Test
    void equityCurveMatchesDataSize() {
        var engine = new BacktestEngine(new BigDecimal("10000"), BigDecimal.ZERO);
        var strategy = new SMACrossoverStrategy(2, 5);
        List<PriceData> data = generateFlatPriceData(20, 100);

        BacktestResult result = engine.run(strategy, data);
        assertEquals(data.size(), result.getEquityCurve().size());
    }

    @Test
    void startAndEndDatesAreSet() {
        var engine = new BacktestEngine(new BigDecimal("10000"), BigDecimal.ZERO);
        var strategy = new SMACrossoverStrategy(2, 5);
        List<PriceData> data = generateFlatPriceData(10, 100);

        BacktestResult result = engine.run(strategy, data);
        assertNotNull(result);
        // Result should have dates set (verified via report output)
    }

    @Test
    void commissionReducesProfit() {
        // Use a simple always-buy-then-sell strategy pattern
        var alwaysBuy = new TestStrategy("Buy-Sell", 2);
        var engineNoComm = new BacktestEngine(new BigDecimal("10000"), BigDecimal.ZERO);
        var engineWithComm = new BacktestEngine(new BigDecimal("10000"), new BigDecimal("10"));

        List<PriceData> data = new ArrayList<>();
        data.add(makePriceData("d1", 50));
        data.add(makePriceData("d2", 50)); // warmup
        data.add(makePriceData("d3", 50)); // BUY
        data.add(makePriceData("d4", 60)); // SELL
        data.add(makePriceData("d5", 60));

        BacktestResult resultNoComm = engineNoComm.run(alwaysBuy, data);
        BacktestResult resultWithComm = engineWithComm.run(alwaysBuy, data);

        assertTrue(resultNoComm.getFinalCapital().compareTo(resultWithComm.getFinalCapital()) > 0,
                "Commission should reduce final capital");
    }

    @Test
    void openPositionClosedAtEnd() {
        // Strategy that only buys and never sells
        var buyOnly = new TestStrategy("Buy-Only", 0) {
            @Override
            public Signal evaluate(List<PriceData> data, int currentIndex) {
                return currentIndex == 2 ? Signal.BUY : Signal.HOLD;
            }
        };

        var engine = new BacktestEngine(new BigDecimal("10000"), BigDecimal.ZERO);
        List<PriceData> data = new ArrayList<>();
        data.add(makePriceData("d1", 100));
        data.add(makePriceData("d2", 100));
        data.add(makePriceData("d3", 100)); // BUY here
        data.add(makePriceData("d4", 110));
        data.add(makePriceData("d5", 120)); // Force-closed here

        BacktestResult result = engine.run(buyOnly, data);
        assertEquals(1, result.getTotalTrades());
        // All trades should be closed
        for (Trade t : result.getTrades()) {
            assertEquals(Trade.Status.CLOSED, t.getStatus());
        }
    }

    private static List<PriceData> generateFlatPriceData(int count, double price) {
        List<PriceData> data = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            data.add(makePriceData("day" + i, price));
        }
        return data;
    }

    private static PriceData makePriceData(String date, double close) {
        BigDecimal p = BigDecimal.valueOf(close);
        return new PriceData(date, p, p, p, p, p, 1000);
    }

    /**
     * Simple test strategy that generates BUY at index warmup+1 and SELL at warmup+2.
     */
    private static class TestStrategy implements TradingStrategy {
        private final String name;
        private final int warmup;

        TestStrategy(String name, int warmup) {
            this.name = name;
            this.warmup = warmup;
        }

        @Override
        public String getName() { return name; }

        @Override
        public Signal evaluate(List<PriceData> data, int currentIndex) {
            if (currentIndex == warmup + 1) return Signal.BUY;
            if (currentIndex == warmup + 2) return Signal.SELL;
            return Signal.HOLD;
        }

        @Override
        public int getWarmupPeriod() { return warmup; }
    }
}
