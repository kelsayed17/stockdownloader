package com.stockdownloader.integration;

import com.stockdownloader.backtest.BacktestEngine;
import com.stockdownloader.backtest.BacktestResult;
import com.stockdownloader.data.CsvPriceDataLoader;
import com.stockdownloader.model.PriceData;
import com.stockdownloader.strategy.MACDStrategy;
import com.stockdownloader.strategy.RSIStrategy;
import com.stockdownloader.strategy.SMACrossoverStrategy;
import com.stockdownloader.strategy.TradingStrategy;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that runs multiple strategies against the same dataset
 * and validates cross-strategy consistency and relative metric properties.
 */
class MultiStrategyComparisonIntegrationTest {

    private static final BigDecimal INITIAL_CAPITAL = new BigDecimal("100000.00");
    private static List<PriceData> priceData;
    private static List<BacktestResult> results;
    private static List<TradingStrategy> strategies;

    @BeforeAll
    static void runAllStrategies() {
        priceData = CsvPriceDataLoader.loadFromStream(
                MultiStrategyComparisonIntegrationTest.class.getResourceAsStream("/test-price-data.csv"));
        assertFalse(priceData.isEmpty());

        strategies = List.of(
                new SMACrossoverStrategy(20, 50),
                new SMACrossoverStrategy(50, 200),
                new RSIStrategy(14, 30, 70),
                new RSIStrategy(14, 25, 75),
                new MACDStrategy(12, 26, 9)
        );

        var engine = new BacktestEngine(INITIAL_CAPITAL, BigDecimal.ZERO);
        results = new ArrayList<>();

        for (TradingStrategy strategy : strategies) {
            results.add(engine.run(strategy, priceData));
        }
    }

    @Test
    void allStrategiesProduceResults() {
        assertEquals(strategies.size(), results.size());
        for (BacktestResult result : results) {
            assertNotNull(result);
            assertNotNull(result.getStrategyName());
            assertNotNull(result.getFinalCapital());
        }
    }

    @Test
    void allStrategiesHaveSameInitialCapital() {
        for (BacktestResult result : results) {
            assertEquals(INITIAL_CAPITAL, result.getInitialCapital());
        }
    }

    @Test
    void allStrategiesHaveSameDateRange() {
        String expectedStart = priceData.getFirst().date();
        String expectedEnd = priceData.getLast().date();

        for (BacktestResult result : results) {
            assertEquals(expectedStart, result.getStartDate());
            assertEquals(expectedEnd, result.getEndDate());
        }
    }

    @Test
    void allStrategiesHaveSameBuyAndHoldReturn() {
        BigDecimal expected = results.getFirst().getBuyAndHoldReturn(priceData);
        for (BacktestResult result : results) {
            assertEquals(0, expected.compareTo(result.getBuyAndHoldReturn(priceData)),
                    "Buy and hold return should be identical across strategies");
        }
    }

    @Test
    void equityCurveSizeMatchesDataSize() {
        for (BacktestResult result : results) {
            assertEquals(priceData.size(), result.getEquityCurve().size(),
                    "Equity curve size should match data size for " + result.getStrategyName());
        }
    }

    @Test
    void metricsAreWithinReasonableBounds() {
        for (BacktestResult result : results) {
            String name = result.getStrategyName();

            BigDecimal totalReturn = result.getTotalReturn();
            assertTrue(totalReturn.compareTo(new BigDecimal("-100")) >= 0,
                    name + " return should be >= -100%");
            assertTrue(totalReturn.compareTo(new BigDecimal("1000")) <= 0,
                    name + " return should be <= 1000% for ~1 year of data");

            BigDecimal maxDrawdown = result.getMaxDrawdown();
            assertTrue(maxDrawdown.compareTo(BigDecimal.ZERO) >= 0,
                    name + " max drawdown should be >= 0");
            assertTrue(maxDrawdown.compareTo(new BigDecimal("100")) <= 0,
                    name + " max drawdown should be <= 100%");

            BigDecimal winRate = result.getWinRate();
            assertTrue(winRate.compareTo(BigDecimal.ZERO) >= 0,
                    name + " win rate should be >= 0");
            assertTrue(winRate.compareTo(new BigDecimal("100")) <= 0,
                    name + " win rate should be <= 100");
        }
    }

    @Test
    void shorterSmaPeriodGeneratesMoreTrades() {
        // SMA(20/50) should generally generate >= trades than SMA(50/200) on same data
        BacktestResult sma2050 = results.get(0);
        BacktestResult sma50200 = results.get(1);

        assertTrue(sma2050.getTotalTrades() >= sma50200.getTotalTrades(),
                "SMA(20/50) should generate >= trades than SMA(50/200)");
    }

    @Test
    void profitFactorConsistencyAcrossStrategies() {
        for (BacktestResult result : results) {
            BigDecimal profitFactor = result.getProfitFactor();
            assertNotNull(profitFactor, "Profit factor should not be null for " + result.getStrategyName());
            assertTrue(profitFactor.compareTo(BigDecimal.ZERO) >= 0,
                    "Profit factor should be non-negative for " + result.getStrategyName());
        }
    }

    @Test
    void determinismSameInputSameOutput() {
        // Running the same strategy twice should produce identical results
        var engine = new BacktestEngine(INITIAL_CAPITAL, BigDecimal.ZERO);
        var strategy = new SMACrossoverStrategy(20, 50);

        BacktestResult run1 = engine.run(strategy, priceData);
        BacktestResult run2 = engine.run(strategy, priceData);

        assertEquals(run1.getFinalCapital(), run2.getFinalCapital());
        assertEquals(run1.getTotalTrades(), run2.getTotalTrades());
        assertEquals(0, run1.getTotalReturn().compareTo(run2.getTotalReturn()));
        assertEquals(0, run1.getMaxDrawdown().compareTo(run2.getMaxDrawdown()));
    }

    @Test
    void rsiWiderThresholdsFewerTrades() {
        // RSI(14, 25/75) is wider than RSI(14, 30/70), so should generate <= signals
        BacktestResult rsiNarrow = results.get(2); // 30/70
        BacktestResult rsiWide = results.get(3);   // 25/75

        assertTrue(rsiNarrow.getTotalTrades() >= rsiWide.getTotalTrades(),
                "Wider RSI thresholds should produce fewer or equal trades");
    }
}
