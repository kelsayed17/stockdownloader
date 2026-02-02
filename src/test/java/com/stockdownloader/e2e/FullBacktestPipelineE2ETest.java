package com.stockdownloader.e2e;

import com.stockdownloader.backtest.BacktestEngine;
import com.stockdownloader.backtest.BacktestReportFormatter;
import com.stockdownloader.backtest.BacktestResult;
import com.stockdownloader.data.CsvPriceDataLoader;
import com.stockdownloader.model.PriceData;
import com.stockdownloader.model.Trade;
import com.stockdownloader.strategy.MACDStrategy;
import com.stockdownloader.strategy.RSIStrategy;
import com.stockdownloader.strategy.SMACrossoverStrategy;
import com.stockdownloader.strategy.TradingStrategy;
import com.stockdownloader.util.BigDecimalMath;
import com.stockdownloader.util.MovingAverageCalculator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end test that replicates the full SPYBacktestApp pipeline:
 * Load real SPY CSV data -> configure all 5 strategies -> run BacktestEngine
 * -> compute BacktestResult metrics -> generate reports and comparison.
 *
 * Uses real historical SPY data from test-price-data.csv (272 trading days).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullBacktestPipelineE2ETest {

    private static final BigDecimal INITIAL_CAPITAL = new BigDecimal("100000.00");
    private static final BigDecimal ZERO_COMMISSION = BigDecimal.ZERO;
    private static final int TRADING_DAYS_PER_YEAR = 252;

    private static List<PriceData> spyData;
    private static List<TradingStrategy> strategies;
    private static List<BacktestResult> results;

    @BeforeAll
    static void loadDataAndRunAllStrategies() {
        // Step 1: Load real SPY data via CsvPriceDataLoader (mirrors SPYBacktestApp.loadFromFile)
        spyData = CsvPriceDataLoader.loadFromStream(
                FullBacktestPipelineE2ETest.class.getResourceAsStream("/test-price-data.csv"));

        assertFalse(spyData.isEmpty(), "Real SPY data must be loaded");
        assertTrue(spyData.size() > 250, "Should have ~272 trading days of real data");

        // Step 2: Configure the exact 5 strategies from SPYBacktestApp
        strategies = List.of(
                new SMACrossoverStrategy(50, 200),
                new SMACrossoverStrategy(20, 50),
                new RSIStrategy(14, 30, 70),
                new RSIStrategy(14, 25, 75),
                new MACDStrategy(12, 26, 9)
        );

        // Step 3: Run backtest engine on all strategies (mirrors SPYBacktestApp main loop)
        var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);
        results = new ArrayList<>(strategies.size());

        for (TradingStrategy strategy : strategies) {
            BacktestResult result = engine.run(strategy, spyData);
            results.add(result);
        }

        assertEquals(5, results.size(), "All 5 strategies should produce results");
    }

    // ========== Data Loading Verification ==========

    @Test
    @Order(1)
    void realSpyDataHasCorrectDateRange() {
        assertEquals("2023-01-03", spyData.getFirst().date());
        assertEquals("2024-01-31", spyData.getLast().date());
    }

    @Test
    @Order(2)
    void realSpyDataOHLCVIntegrity() {
        for (PriceData bar : spyData) {
            assertNotNull(bar.date());
            assertTrue(bar.low().compareTo(bar.high()) <= 0,
                    "Low should be <= High on " + bar.date());
            assertTrue(bar.open().compareTo(bar.high()) <= 0,
                    "Open should be <= High on " + bar.date());
            assertTrue(bar.open().compareTo(bar.low()) >= 0,
                    "Open should be >= Low on " + bar.date());
            assertTrue(bar.close().compareTo(bar.high()) <= 0,
                    "Close should be <= High on " + bar.date());
            assertTrue(bar.close().compareTo(bar.low()) >= 0,
                    "Close should be >= Low on " + bar.date());
            assertTrue(bar.volume() > 0, "Volume should be positive on " + bar.date());
        }
    }

    @Test
    @Order(3)
    void realSpyDataChronologicalOrder() {
        for (int i = 1; i < spyData.size(); i++) {
            assertTrue(spyData.get(i).date().compareTo(spyData.get(i - 1).date()) > 0,
                    "Data should be chronologically ordered at index " + i);
        }
    }

    // ========== Strategy Name Verification ==========

    @Test
    @Order(4)
    void allStrategyNamesMatchExpected() {
        assertEquals("SMA Crossover (50/200)", results.get(0).getStrategyName());
        assertEquals("SMA Crossover (20/50)", results.get(1).getStrategyName());
        assertEquals("RSI (14) [30.0/70.0]", results.get(2).getStrategyName());
        assertEquals("RSI (14) [25.0/75.0]", results.get(3).getStrategyName());
        assertEquals("MACD (12/26/9)", results.get(4).getStrategyName());
    }

    // ========== SMA Crossover (50/200) End-to-End ==========

    @Test
    @Order(5)
    void sma50200ProducesValidEndToEndResults() {
        BacktestResult result = results.get(0);

        assertEquals(INITIAL_CAPITAL, result.getInitialCapital());
        assertTrue(result.getFinalCapital().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(spyData.size(), result.getEquityCurve().size());
        assertEquals("2023-01-03", result.getStartDate());
        assertEquals("2024-01-31", result.getEndDate());

        // With only 272 days, SMA(50/200) may have very few trades since warmup needs 200 bars
        // The strategy should still produce valid results
        assertTrue(result.getTotalTrades() >= 0);

        // Verify moving averages are correctly computed for the last bar
        int lastIdx = spyData.size() - 1;
        BigDecimal sma50 = MovingAverageCalculator.sma(spyData, lastIdx, 50);
        BigDecimal sma200 = MovingAverageCalculator.sma(spyData, lastIdx, 200);
        assertTrue(sma50.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(sma200.compareTo(BigDecimal.ZERO) > 0);
    }

    // ========== SMA Crossover (20/50) End-to-End ==========

    @Test
    @Order(6)
    void sma2050GeneratesTradesWithRealData() {
        BacktestResult result = results.get(1);

        // With 272 days, SMA(20/50) should have enough warmup to generate signals
        assertTrue(result.getTotalTrades() > 0,
                "SMA(20/50) should generate at least one trade with 272 days of real SPY data");

        // Verify every trade has valid entry/exit data
        for (Trade trade : result.getClosedTrades()) {
            assertEquals(Trade.Direction.LONG, trade.getDirection(), "Engine only produces LONG trades");
            assertEquals(Trade.Status.CLOSED, trade.getStatus());
            assertNotNull(trade.getEntryDate());
            assertNotNull(trade.getExitDate());
            assertTrue(trade.getEntryPrice().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(trade.getExitPrice().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(trade.getShares() > 0);
            // Verify P/L calculation: (exitPrice - entryPrice) * shares for LONG
            BigDecimal expectedPL = trade.getExitPrice().subtract(trade.getEntryPrice())
                    .multiply(BigDecimal.valueOf(trade.getShares()));
            assertEquals(0, expectedPL.compareTo(trade.getProfitLoss()),
                    "Trade P/L should match manual calculation");
        }
    }

    // ========== RSI Strategy End-to-End ==========

    @Test
    @Order(7)
    void rsiStrategiesProduceValidSignals() {
        BacktestResult rsi3070 = results.get(2);
        BacktestResult rsi2575 = results.get(3);

        // Both RSI strategies should produce valid results
        assertTrue(rsi3070.getFinalCapital().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(rsi2575.getFinalCapital().compareTo(BigDecimal.ZERO) > 0);

        // Wider thresholds (25/75) should generate fewer or equal trades than (30/70)
        assertTrue(rsi3070.getTotalTrades() >= rsi2575.getTotalTrades(),
                "RSI(30/70) should have >= trades than RSI(25/75)");
    }

    // ========== MACD Strategy End-to-End ==========

    @Test
    @Order(8)
    void macdStrategyFullPipeline() {
        BacktestResult result = results.get(4);

        assertTrue(result.getFinalCapital().compareTo(BigDecimal.ZERO) > 0);

        // MACD warmup = 26 + 9 = 35 bars, so plenty of room for signals in 272 days
        assertTrue(result.getTotalTrades() > 0,
                "MACD should generate at least one trade with real SPY data");

        // Verify Sharpe ratio is computable
        BigDecimal sharpe = result.getSharpeRatio(TRADING_DAYS_PER_YEAR);
        assertNotNull(sharpe);
    }

    // ========== Cross-Strategy Consistency ==========

    @Test
    @Order(9)
    void allStrategiesShareBuyAndHoldBenchmark() {
        BigDecimal firstClose = spyData.getFirst().close();
        BigDecimal lastClose = spyData.getLast().close();
        BigDecimal expectedBuyHold = lastClose.subtract(firstClose)
                .divide(firstClose, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        for (BacktestResult result : results) {
            BigDecimal buyHold = result.getBuyAndHoldReturn(spyData);
            assertEquals(0, expectedBuyHold.compareTo(buyHold),
                    "Buy-and-hold return should be identical for all strategies on " + result.getStrategyName());
        }
    }

    @Test
    @Order(10)
    void capitalConservationAcrossStrategies() {
        // With zero commission and the engine's logic, capital should be conserved
        // (initial capital redistributed between cash and positions)
        for (BacktestResult result : results) {
            // Final capital = initial capital + sum of all trade P/L
            BigDecimal totalPL = result.getTotalProfitLoss();
            BigDecimal expectedFinal = INITIAL_CAPITAL.add(totalPL);
            assertEquals(0, expectedFinal.compareTo(result.getFinalCapital()),
                    "Final capital should equal initial + total P/L for " + result.getStrategyName());
        }
    }

    @Test
    @Order(11)
    void equityCurveStartsAtInitialCapital() {
        for (BacktestResult result : results) {
            BigDecimal firstEquity = result.getEquityCurve().getFirst();
            // First equity point is computed before any trade on bar 0
            assertEquals(0, INITIAL_CAPITAL.compareTo(firstEquity),
                    "Equity curve should start at initial capital for " + result.getStrategyName());
        }
    }

    @Test
    @Order(12)
    void equityCurveAllPositiveValues() {
        for (BacktestResult result : results) {
            for (int i = 0; i < result.getEquityCurve().size(); i++) {
                assertTrue(result.getEquityCurve().get(i).compareTo(BigDecimal.ZERO) > 0,
                        "Equity should be positive at index " + i + " for " + result.getStrategyName());
            }
        }
    }

    // ========== Metrics Computation Verification ==========

    @Test
    @Order(13)
    void totalReturnConsistencyAcrossStrategies() {
        for (BacktestResult result : results) {
            // totalReturn = (finalCapital - initialCapital) / initialCapital * 100
            BigDecimal expected = result.getFinalCapital().subtract(result.getInitialCapital())
                    .divide(result.getInitialCapital(), 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            assertEquals(0, expected.compareTo(result.getTotalReturn()),
                    "Total return formula consistency for " + result.getStrategyName());
        }
    }

    @Test
    @Order(14)
    void winLossTradeCountsAddUp() {
        for (BacktestResult result : results) {
            assertEquals(result.getTotalTrades(), result.getWinningTrades() + result.getLosingTrades(),
                    "Win + Loss should equal total trades for " + result.getStrategyName());
        }
    }

    @Test
    @Order(15)
    void maxDrawdownBounds() {
        for (BacktestResult result : results) {
            BigDecimal maxDD = result.getMaxDrawdown();
            assertTrue(maxDD.compareTo(BigDecimal.ZERO) >= 0,
                    "Max drawdown >= 0 for " + result.getStrategyName());
            assertTrue(maxDD.compareTo(new BigDecimal("100")) <= 0,
                    "Max drawdown <= 100% for " + result.getStrategyName());
        }
    }

    @Test
    @Order(16)
    void profitFactorValidValues() {
        for (BacktestResult result : results) {
            BigDecimal pf = result.getProfitFactor();
            assertTrue(pf.compareTo(BigDecimal.ZERO) >= 0,
                    "Profit factor should be non-negative for " + result.getStrategyName());
        }
    }

    // ========== Report Generation E2E ==========

    @Test
    @Order(17)
    void individualReportsGenerateWithoutErrors() {
        PrintStream original = System.out;
        var capture = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capture));

        try {
            for (BacktestResult result : results) {
                BacktestReportFormatter.printReport(result, spyData);
            }
        } finally {
            System.setOut(original);
        }

        String output = capture.toString();
        // Verify each strategy's report was generated
        assertTrue(output.contains("BACKTEST REPORT: SMA Crossover (50/200)"));
        assertTrue(output.contains("BACKTEST REPORT: SMA Crossover (20/50)"));
        assertTrue(output.contains("BACKTEST REPORT: RSI (14) [30.0/70.0]"));
        assertTrue(output.contains("BACKTEST REPORT: RSI (14) [25.0/75.0]"));
        assertTrue(output.contains("BACKTEST REPORT: MACD (12/26/9)"));

        // Verify report sections exist
        assertTrue(output.contains("PERFORMANCE METRICS"));
        assertTrue(output.contains("TRADE STATISTICS"));
        assertTrue(output.contains("Total Return:"));
        assertTrue(output.contains("Buy & Hold Return:"));
        assertTrue(output.contains("Sharpe Ratio:"));
        assertTrue(output.contains("Max Drawdown:"));
        assertTrue(output.contains("Win Rate:"));
    }

    @Test
    @Order(18)
    void comparisonReportGeneratesWithoutErrors() {
        PrintStream original = System.out;
        var capture = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capture));

        try {
            BacktestReportFormatter.printComparison(results, spyData);
        } finally {
            System.setOut(original);
        }

        String output = capture.toString();
        assertTrue(output.contains("STRATEGY COMPARISON SUMMARY"));
        assertTrue(output.contains("Buy & Hold (Benchmark)"));
        assertTrue(output.contains("Best performing strategy:"));
        assertTrue(output.contains("DISCLAIMER"));

        // All strategy names should appear in comparison
        for (BacktestResult result : results) {
            assertTrue(output.contains(result.getStrategyName()),
                    "Comparison should include " + result.getStrategyName());
        }
    }

    @Test
    @Order(19)
    void bestStrategyIdentifiedCorrectly() {
        PrintStream original = System.out;
        var capture = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capture));

        try {
            BacktestReportFormatter.printComparison(results, spyData);
        } finally {
            System.setOut(original);
        }

        // Find the best strategy manually
        BacktestResult best = null;
        for (BacktestResult r : results) {
            if (best == null || r.getTotalReturn().compareTo(best.getTotalReturn()) > 0) {
                best = r;
            }
        }

        String output = capture.toString();
        assertNotNull(best);
        assertTrue(output.contains("Best performing strategy: " + best.getStrategyName()));
    }

    // ========== Commission Impact E2E ==========

    @Test
    @Order(20)
    void commissionImpactOnAllStrategies() {
        BigDecimal commission = new BigDecimal("9.99");
        var engineWithComm = new BacktestEngine(INITIAL_CAPITAL, commission);

        for (int i = 0; i < strategies.size(); i++) {
            BacktestResult noComm = results.get(i);
            BacktestResult withComm = engineWithComm.run(strategies.get(i), spyData);

            if (noComm.getTotalTrades() > 0) {
                assertTrue(noComm.getFinalCapital().compareTo(withComm.getFinalCapital()) >= 0,
                        "Commission should reduce final capital for " + strategies.get(i).getName());
            }
        }
    }

    // ========== Determinism E2E ==========

    @Test
    @Order(21)
    void fullPipelineIsDeterministic() {
        var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);
        List<BacktestResult> rerun = new ArrayList<>();

        for (TradingStrategy strategy : strategies) {
            rerun.add(engine.run(strategy, spyData));
        }

        for (int i = 0; i < results.size(); i++) {
            BacktestResult r1 = results.get(i);
            BacktestResult r2 = rerun.get(i);

            assertEquals(0, r1.getFinalCapital().compareTo(r2.getFinalCapital()),
                    "Determinism: final capital for " + r1.getStrategyName());
            assertEquals(r1.getTotalTrades(), r2.getTotalTrades(),
                    "Determinism: trade count for " + r1.getStrategyName());
            assertEquals(0, r1.getTotalReturn().compareTo(r2.getTotalReturn()),
                    "Determinism: total return for " + r1.getStrategyName());
            assertEquals(r1.getEquityCurve().size(), r2.getEquityCurve().size(),
                    "Determinism: equity curve size for " + r1.getStrategyName());
        }
    }

    // ========== Moving Average Integration ==========

    @Test
    @Order(22)
    void movingAveragesCorrectlyComputedOnRealData() {
        // Verify SMA/EMA computations on real SPY data match expected behavior
        int idx = 100; // Well past any warmup period
        BigDecimal sma20 = MovingAverageCalculator.sma(spyData, idx, 20);
        BigDecimal sma50 = MovingAverageCalculator.sma(spyData, idx, 50);
        BigDecimal ema12 = MovingAverageCalculator.ema(spyData, idx, 12);
        BigDecimal ema26 = MovingAverageCalculator.ema(spyData, idx, 26);

        // All should be in reasonable SPY price range ($350-$500)
        BigDecimal lowerBound = new BigDecimal("350");
        BigDecimal upperBound = new BigDecimal("500");

        assertTrue(sma20.compareTo(lowerBound) > 0 && sma20.compareTo(upperBound) < 0,
                "SMA(20) should be in SPY price range");
        assertTrue(sma50.compareTo(lowerBound) > 0 && sma50.compareTo(upperBound) < 0,
                "SMA(50) should be in SPY price range");
        assertTrue(ema12.compareTo(lowerBound) > 0 && ema12.compareTo(upperBound) < 0,
                "EMA(12) should be in SPY price range");
        assertTrue(ema26.compareTo(lowerBound) > 0 && ema26.compareTo(upperBound) < 0,
                "EMA(26) should be in SPY price range");
    }

    // ========== BigDecimalMath Integration ==========

    @Test
    @Order(23)
    void bigDecimalMathUsedInMetricComputations() {
        // Verify BigDecimalMath.percentChange gives same result as BacktestResult.getBuyAndHoldReturn
        BigDecimal firstClose = spyData.getFirst().close();
        BigDecimal lastClose = spyData.getLast().close();

        BigDecimal mathPctChange = BigDecimalMath.percentChange(firstClose, lastClose);
        BigDecimal resultPctChange = results.getFirst().getBuyAndHoldReturn(spyData);

        assertEquals(0, mathPctChange.compareTo(resultPctChange),
                "BigDecimalMath.percentChange should match BacktestResult.getBuyAndHoldReturn");
    }

    // ========== Varying Capital Sizes ==========

    @Test
    @Order(24)
    void differentCapitalSizesProduceProportionalResults() {
        var strategy = new SMACrossoverStrategy(20, 50);
        BigDecimal smallCap = new BigDecimal("10000.00");
        BigDecimal largeCap = new BigDecimal("1000000.00");

        var engineSmall = new BacktestEngine(smallCap, ZERO_COMMISSION);
        var engineLarge = new BacktestEngine(largeCap, ZERO_COMMISSION);

        BacktestResult resultSmall = engineSmall.run(strategy, spyData);
        BacktestResult resultLarge = engineLarge.run(strategy, spyData);

        // Total return percentage should be very close (within 1% due to share rounding)
        BigDecimal returnDiff = resultSmall.getTotalReturn().subtract(resultLarge.getTotalReturn()).abs();
        assertTrue(returnDiff.compareTo(new BigDecimal("1.0")) < 0,
                "Return % should be nearly proportional across capital sizes, diff=" + returnDiff);
    }
}
