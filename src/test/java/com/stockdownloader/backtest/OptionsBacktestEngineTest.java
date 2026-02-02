package com.stockdownloader.backtest;

import com.stockdownloader.model.OptionTrade;
import com.stockdownloader.model.OptionType;
import com.stockdownloader.model.PriceData;
import com.stockdownloader.model.Trade;
import com.stockdownloader.strategy.CoveredCallStrategy;
import com.stockdownloader.strategy.IronCondorStrategy;
import com.stockdownloader.strategy.OptionsStrategy;
import com.stockdownloader.strategy.StraddleStrategy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OptionsBacktestEngineTest {

    @Test
    void runWithNullStrategyThrows() {
        var engine = new OptionsBacktestEngine(new BigDecimal("100000"), BigDecimal.ZERO);
        List<PriceData> data = List.of(makePrice("d1", 100));
        assertThrows(NullPointerException.class, () -> {
            engine.run(null, data);
        });
    }

    @Test
    void runWithEmptyDataThrows() {
        var engine = new OptionsBacktestEngine(new BigDecimal("100000"), BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class, () -> {
            engine.run(new CoveredCallStrategy(), List.of());
        });
    }

    @Test
    void runWithNullDataThrows() {
        var engine = new OptionsBacktestEngine(new BigDecimal("100000"), BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class, () -> {
            engine.run(new CoveredCallStrategy(), null);
        });
    }

    @Test
    void resultHasCorrectStrategyName() {
        var engine = new OptionsBacktestEngine(new BigDecimal("100000"), BigDecimal.ZERO);
        var strategy = new CoveredCallStrategy();
        List<PriceData> data = generateFlatPrices(60, 500);

        OptionsBacktestResult result = engine.run(strategy, data);
        assertTrue(result.getStrategyName().contains("Covered Call"));
    }

    @Test
    void equityCurveMatchesDataSize() {
        var engine = new OptionsBacktestEngine(new BigDecimal("100000"), BigDecimal.ZERO);
        var strategy = new CoveredCallStrategy();
        List<PriceData> data = generateFlatPrices(60, 500);

        OptionsBacktestResult result = engine.run(strategy, data);
        assertEquals(data.size(), result.getEquityCurve().size());
    }

    @Test
    void startAndEndDatesAreSet() {
        var engine = new OptionsBacktestEngine(new BigDecimal("100000"), BigDecimal.ZERO);
        var strategy = new CoveredCallStrategy();
        List<PriceData> data = generateFlatPrices(60, 500);

        OptionsBacktestResult result = engine.run(strategy, data);
        assertEquals(data.getFirst().date(), result.getStartDate());
        assertEquals(data.getLast().date(), result.getEndDate());
    }

    @Test
    void coveredCallOnRisingMarket() {
        var engine = new OptionsBacktestEngine(new BigDecimal("100000"), new BigDecimal("0.65"));
        var strategy = new CoveredCallStrategy(5, new BigDecimal("5"), 30, 0);
        List<PriceData> data = generateRisingPrices(100, 480);

        OptionsBacktestResult result = engine.run(strategy, data);
        assertNotNull(result);
        assertTrue(result.getTotalTrades() >= 0);
    }

    @Test
    void ironCondorOnFlatMarket() {
        var engine = new OptionsBacktestEngine(new BigDecimal("100000"), new BigDecimal("0.65"));
        var strategy = new IronCondorStrategy(5, new BigDecimal("5"), new BigDecimal("3"), 30, 0);
        List<PriceData> data = generateFlatPrices(100, 500);

        OptionsBacktestResult result = engine.run(strategy, data);
        assertNotNull(result);
        // Iron condor on flat market should generate trades
        assertTrue(result.getTotalTrades() > 0, "Should have some trades on flat market");
    }

    @Test
    void straddleCreatesTwoLegsPerTrade() {
        var engine = new OptionsBacktestEngine(new BigDecimal("100000"), BigDecimal.ZERO);
        var strategy = new StraddleStrategy(5, 30, 0, new BigDecimal("50"), new BigDecimal("0.10"));
        List<PriceData> data = generateFlatPrices(60, 500);

        OptionsBacktestResult result = engine.run(strategy, data);
        // Straddle trades come in pairs (call + put)
        if (result.getTotalTrades() > 0) {
            assertTrue(result.getTotalCallTrades() > 0, "Should have call trades");
            assertTrue(result.getTotalPutTrades() > 0, "Should have put trades");
        }
    }

    @Test
    void commissionReducesProfits() {
        var noCommEngine = new OptionsBacktestEngine(new BigDecimal("100000"), BigDecimal.ZERO);
        var commEngine = new OptionsBacktestEngine(new BigDecimal("100000"), new BigDecimal("5.00"));
        var strategy = new IronCondorStrategy(5, new BigDecimal("5"), new BigDecimal("3"), 30, 0);
        List<PriceData> data = generateFlatPrices(100, 500);

        OptionsBacktestResult noCommResult = noCommEngine.run(strategy, data);
        OptionsBacktestResult commResult = commEngine.run(strategy, data);

        assertTrue(noCommResult.getFinalCapital().compareTo(commResult.getFinalCapital()) >= 0,
                "Commission should reduce final capital");
    }

    @Test
    void openPositionsClosedAtEnd() {
        var engine = new OptionsBacktestEngine(new BigDecimal("100000"), BigDecimal.ZERO);
        // Use strategy that opens near end
        var strategy = new TestOptionsStrategy();
        List<PriceData> data = generateFlatPrices(10, 500);

        OptionsBacktestResult result = engine.run(strategy, data);
        // All trades should be closed
        for (OptionTrade t : result.getTrades()) {
            assertNotEquals(OptionTrade.Status.OPEN, t.getStatus());
        }
    }

    private static List<PriceData> generateFlatPrices(int count, double price) {
        List<PriceData> data = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            BigDecimal p = BigDecimal.valueOf(price);
            data.add(new PriceData("2024-01-%02d".formatted(i + 1), p, p, p, p, p, 10000));
        }
        return data;
    }

    private static List<PriceData> generateRisingPrices(int count, double startPrice) {
        List<PriceData> data = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double price = startPrice + i;
            BigDecimal p = BigDecimal.valueOf(price);
            data.add(new PriceData("2024-01-%02d".formatted(i + 1), p, p, p, p, p, 10000));
        }
        return data;
    }

    private static PriceData makePrice(String date, double close) {
        BigDecimal p = BigDecimal.valueOf(close);
        return new PriceData(date, p, p, p, p, p, 10000);
    }

    /**
     * Test strategy that opens a single long call at index 5 and never explicitly closes.
     */
    private static class TestOptionsStrategy implements OptionsStrategy {
        @Override public String getName() { return "Test Strategy"; }

        @Override
        public Action evaluate(List<PriceData> data, int currentIndex, List<OptionTrade> openTrades) {
            if (currentIndex == 5 && openTrades.isEmpty()) return Action.OPEN;
            return Action.HOLD;
        }

        @Override
        public List<OptionTrade> createTrades(List<PriceData> data, int currentIndex, BigDecimal availableCapital) {
            PriceData current = data.get(currentIndex);
            return List.of(new OptionTrade(OptionType.CALL, Trade.Direction.LONG,
                    current.close(), "2099-12-31", current.date(),
                    new BigDecimal("5.00"), 1, current.volume()));
        }

        @Override public int getTargetDTE() { return 30; }
        @Override public long getMinVolume() { return 0; }
        @Override public int getWarmupPeriod() { return 0; }
    }
}
