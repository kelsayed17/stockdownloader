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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive backtesting scenarios using real SPY data (Jan 2023 - Jan 2024).
 *
 * Tests strategies across different market regimes, parameter sensitivities,
 * capital/commission configurations, and produces a full scenario comparison report.
 *
 * Market segments identified in the data:
 *   - Q1 2023 Recovery (Jan-Mar ~60 days): Post-2022 bear recovery, choppy/sideways
 *   - Q2 2023 Bull Rally (Apr-Jul ~80 days): Sustained uptrend driven by AI enthusiasm
 *   - Q3 2023 Correction (Aug-Oct ~65 days): Pullback from summer highs
 *   - Q4 2023 Year-End Rally (Nov 2023-Jan 2024 ~67 days): Strong year-end rally
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SPYBacktestScenariosTest {

    private static final BigDecimal INITIAL_CAPITAL = new BigDecimal("100000.00");
    private static final BigDecimal ZERO_COMMISSION = BigDecimal.ZERO;
    private static final int TRADING_DAYS_PER_YEAR = 252;

    private static List<PriceData> fullData;

    // Market segments sliced from full data
    private static List<PriceData> q1Recovery;      // ~first 60 trading days
    private static List<PriceData> q2BullRally;      // ~next 80 trading days
    private static List<PriceData> q3Correction;     // ~next 65 trading days
    private static List<PriceData> q4YearEndRally;   // remaining ~67 trading days

    @BeforeAll
    static void loadAndSegmentData() {
        fullData = CsvPriceDataLoader.loadFromStream(
                SPYBacktestScenariosTest.class.getResourceAsStream("/test-price-data.csv"));
        assertFalse(fullData.isEmpty(), "SPY data must be loaded");
        assertTrue(fullData.size() >= 270, "Expected ~272 trading days, got " + fullData.size());

        // Segment into market regimes based on approximate date boundaries
        int q1End = findIndexForDate("2023-04-01");
        int q2End = findIndexForDate("2023-08-01");
        int q3End = findIndexForDate("2023-11-01");

        q1Recovery = fullData.subList(0, q1End);
        q2BullRally = fullData.subList(q1End, q2End);
        q3Correction = fullData.subList(q2End, q3End);
        q4YearEndRally = fullData.subList(q3End, fullData.size());

        assertTrue(q1Recovery.size() >= 50, "Q1 segment should have adequate data");
        assertTrue(q2BullRally.size() >= 60, "Q2 segment should have adequate data");
        assertTrue(q3Correction.size() >= 50, "Q3 segment should have adequate data");
        assertTrue(q4YearEndRally.size() >= 50, "Q4 segment should have adequate data");
    }

    /**
     * Find the index of the first bar on or after the given date.
     */
    private static int findIndexForDate(String targetDate) {
        for (int i = 0; i < fullData.size(); i++) {
            if (fullData.get(i).date().compareTo(targetDate) >= 0) {
                return i;
            }
        }
        return fullData.size();
    }

    // ======================================================================
    //  SCENARIO 1: Market Regime Characterization
    // ======================================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class MarketRegimeCharacterization {

        @Test
        @Order(1)
        void q1RecoveryBuyAndHoldPerformance() {
            BigDecimal firstClose = q1Recovery.getFirst().close();
            BigDecimal lastClose = q1Recovery.getLast().close();
            BigDecimal returnPct = lastClose.subtract(firstClose)
                    .divide(firstClose, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            System.out.println("Q1 2023 Recovery: " + q1Recovery.getFirst().date()
                    + " to " + q1Recovery.getLast().date()
                    + " | Buy-Hold Return: " + returnPct.setScale(2, RoundingMode.HALF_UP) + "%"
                    + " | Days: " + q1Recovery.size());

            // Q1 2023 was mostly sideways after 2022 decline
            assertNotNull(returnPct);
        }

        @Test
        @Order(2)
        void q2BullRallyBuyAndHoldPerformance() {
            BigDecimal firstClose = q2BullRally.getFirst().close();
            BigDecimal lastClose = q2BullRally.getLast().close();
            BigDecimal returnPct = lastClose.subtract(firstClose)
                    .divide(firstClose, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            System.out.println("Q2 2023 Bull Rally: " + q2BullRally.getFirst().date()
                    + " to " + q2BullRally.getLast().date()
                    + " | Buy-Hold Return: " + returnPct.setScale(2, RoundingMode.HALF_UP) + "%"
                    + " | Days: " + q2BullRally.size());

            // Q2 2023 saw strong AI-driven rally
            assertTrue(returnPct.compareTo(BigDecimal.ZERO) > 0,
                    "Q2 2023 should have positive buy-and-hold return (bull market)");
        }

        @Test
        @Order(3)
        void q3CorrectionBuyAndHoldPerformance() {
            BigDecimal firstClose = q3Correction.getFirst().close();
            BigDecimal lastClose = q3Correction.getLast().close();
            BigDecimal returnPct = lastClose.subtract(firstClose)
                    .divide(firstClose, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            System.out.println("Q3 2023 Correction: " + q3Correction.getFirst().date()
                    + " to " + q3Correction.getLast().date()
                    + " | Buy-Hold Return: " + returnPct.setScale(2, RoundingMode.HALF_UP) + "%"
                    + " | Days: " + q3Correction.size());

            // Q3 2023 saw a pullback from summer highs
            assertNotNull(returnPct);
        }

        @Test
        @Order(4)
        void q4YearEndRallyBuyAndHoldPerformance() {
            BigDecimal firstClose = q4YearEndRally.getFirst().close();
            BigDecimal lastClose = q4YearEndRally.getLast().close();
            BigDecimal returnPct = lastClose.subtract(firstClose)
                    .divide(firstClose, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            System.out.println("Q4 2023 Year-End Rally: " + q4YearEndRally.getFirst().date()
                    + " to " + q4YearEndRally.getLast().date()
                    + " | Buy-Hold Return: " + returnPct.setScale(2, RoundingMode.HALF_UP) + "%"
                    + " | Days: " + q4YearEndRally.size());

            // Q4 2023 had a strong year-end rally
            assertTrue(returnPct.compareTo(BigDecimal.ZERO) > 0,
                    "Q4 2023 year-end rally should have positive buy-and-hold return");
        }

        @Test
        @Order(5)
        void fullPeriodBuyAndHoldPerformance() {
            BigDecimal firstClose = fullData.getFirst().close();
            BigDecimal lastClose = fullData.getLast().close();
            BigDecimal returnPct = lastClose.subtract(firstClose)
                    .divide(firstClose, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            System.out.println("Full Period: " + fullData.getFirst().date()
                    + " to " + fullData.getLast().date()
                    + " | Buy-Hold Return: " + returnPct.setScale(2, RoundingMode.HALF_UP) + "%"
                    + " | Days: " + fullData.size());

            // SPY was positive over the full Jan 2023 - Jan 2024 period
            assertTrue(returnPct.compareTo(BigDecimal.ZERO) > 0,
                    "SPY full-period return should be positive (Jan 2023 to Jan 2024)");
        }

        @Test
        @Order(6)
        void segmentsAreMutuallyExclusiveAndExhaustive() {
            int totalSegmentDays = q1Recovery.size() + q2BullRally.size()
                    + q3Correction.size() + q4YearEndRally.size();
            assertEquals(fullData.size(), totalSegmentDays,
                    "Segments should cover all trading days");

            // Verify no overlap: last date of each segment < first date of next
            assertTrue(q1Recovery.getLast().date().compareTo(q2BullRally.getFirst().date()) < 0);
            assertTrue(q2BullRally.getLast().date().compareTo(q3Correction.getFirst().date()) < 0);
            assertTrue(q3Correction.getLast().date().compareTo(q4YearEndRally.getFirst().date()) < 0);
        }
    }

    // ======================================================================
    //  SCENARIO 2: SMA Crossover Parameter Sensitivity
    // ======================================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class SMAParameterSensitivity {

        private static final int[][] SMA_CONFIGS = {
                {5, 20},    // Aggressive short-term
                {10, 30},   // Short-term
                {20, 50},   // Medium-term
                {50, 100},  // Medium-long term
                {50, 200},  // Classic golden/death cross
        };

        @Test
        @Order(1)
        void shorterPeriodsGenerateMoreTrades() {
            var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);

            List<BacktestResult> smaResults = new ArrayList<>();
            for (int[] config : SMA_CONFIGS) {
                var strategy = new SMACrossoverStrategy(config[0], config[1]);
                smaResults.add(engine.run(strategy, fullData));
            }

            // Generally, shorter periods = more crossover signals = more trades
            // Compare (5/20) vs (50/200)
            BacktestResult aggressive = smaResults.getFirst();
            BacktestResult conservative = smaResults.getLast();

            System.out.println("SMA Parameter Sensitivity (Full Period):");
            for (int i = 0; i < SMA_CONFIGS.length; i++) {
                BacktestResult r = smaResults.get(i);
                System.out.printf("  SMA(%d/%d): Return=%.2f%%, Trades=%d, Sharpe=%s, MaxDD=%.2f%%%n",
                        SMA_CONFIGS[i][0], SMA_CONFIGS[i][1],
                        r.getTotalReturn().doubleValue(),
                        r.getTotalTrades(),
                        r.getSharpeRatio(TRADING_DAYS_PER_YEAR),
                        r.getMaxDrawdown().doubleValue());
            }

            assertTrue(aggressive.getTotalTrades() >= conservative.getTotalTrades(),
                    "Shorter SMA periods should generate >= trades than longer periods");
        }

        @Test
        @Order(2)
        void allSMAConfigsPreserveCapital() {
            var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);

            for (int[] config : SMA_CONFIGS) {
                var strategy = new SMACrossoverStrategy(config[0], config[1]);
                BacktestResult result = engine.run(strategy, fullData);

                // Final capital should be positive
                assertTrue(result.getFinalCapital().compareTo(BigDecimal.ZERO) > 0,
                        "SMA(%d/%d) should have positive final capital".formatted(config[0], config[1]));

                // Equity curve should have same size as data
                assertEquals(fullData.size(), result.getEquityCurve().size(),
                        "SMA(%d/%d) equity curve size mismatch".formatted(config[0], config[1]));
            }
        }

        @Test
        @Order(3)
        void smaReturnsAreConsistentWithTradeLog() {
            var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);

            for (int[] config : SMA_CONFIGS) {
                var strategy = new SMACrossoverStrategy(config[0], config[1]);
                BacktestResult result = engine.run(strategy, fullData);

                // Sum of trade P/L should equal total P/L
                BigDecimal tradePLSum = result.getClosedTrades().stream()
                        .map(Trade::getProfitLoss)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                assertEquals(0, tradePLSum.compareTo(result.getTotalProfitLoss()),
                        "SMA(%d/%d) trade P/L sum should match total P/L"
                                .formatted(config[0], config[1]));
            }
        }
    }

    // ======================================================================
    //  SCENARIO 3: RSI Threshold Sensitivity
    // ======================================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class RSIThresholdSensitivity {

        private static final double[][] RSI_CONFIGS = {
                {14, 20, 80},   // Very wide thresholds - fewer signals
                {14, 25, 75},   // Wide thresholds
                {14, 30, 70},   // Standard thresholds
                {14, 35, 65},   // Tight thresholds
                {14, 40, 60},   // Very tight thresholds - more signals
        };

        @Test
        @Order(1)
        void widerThresholdsProduceFewerTrades() {
            var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);

            List<BacktestResult> rsiResults = new ArrayList<>();
            for (double[] config : RSI_CONFIGS) {
                var strategy = new RSIStrategy((int) config[0], config[1], config[2]);
                rsiResults.add(engine.run(strategy, fullData));
            }

            System.out.println("RSI Threshold Sensitivity (Full Period):");
            for (int i = 0; i < RSI_CONFIGS.length; i++) {
                BacktestResult r = rsiResults.get(i);
                System.out.printf("  RSI(14, %.0f/%.0f): Return=%.2f%%, Trades=%d, WinRate=%.2f%%, Sharpe=%s%n",
                        RSI_CONFIGS[i][1], RSI_CONFIGS[i][2],
                        r.getTotalReturn().doubleValue(),
                        r.getTotalTrades(),
                        r.getWinRate().doubleValue(),
                        r.getSharpeRatio(TRADING_DAYS_PER_YEAR));
            }

            // Wider thresholds = fewer signals
            BacktestResult widest = rsiResults.getFirst();   // 20/80
            BacktestResult tightest = rsiResults.getLast();   // 40/60
            assertTrue(tightest.getTotalTrades() >= widest.getTotalTrades(),
                    "Tighter RSI thresholds should generate >= trades than wider ones");
        }

        @Test
        @Order(2)
        void rsiPeriodSensitivity() {
            var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);
            int[] periods = {7, 14, 21, 28};

            System.out.println("RSI Period Sensitivity (30/70 thresholds):");
            for (int period : periods) {
                var strategy = new RSIStrategy(period, 30, 70);
                BacktestResult result = engine.run(strategy, fullData);

                System.out.printf("  RSI(%d, 30/70): Return=%.2f%%, Trades=%d, Sharpe=%s%n",
                        period,
                        result.getTotalReturn().doubleValue(),
                        result.getTotalTrades(),
                        result.getSharpeRatio(TRADING_DAYS_PER_YEAR));

                assertTrue(result.getFinalCapital().compareTo(BigDecimal.ZERO) > 0,
                        "RSI(%d) should maintain positive capital".formatted(period));
            }
        }
    }

    // ======================================================================
    //  SCENARIO 4: MACD Parameter Variations
    // ======================================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class MACDParameterVariations {

        @Test
        @Order(1)
        void standardVsAggressiveMACDParameters() {
            var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);

            var standard = new MACDStrategy(12, 26, 9);
            var aggressive = new MACDStrategy(8, 17, 9);
            var conservative = new MACDStrategy(19, 39, 9);

            BacktestResult stdResult = engine.run(standard, fullData);
            BacktestResult aggResult = engine.run(aggressive, fullData);
            BacktestResult conResult = engine.run(conservative, fullData);

            System.out.println("MACD Parameter Variations (Full Period):");
            System.out.printf("  MACD(8/17/9) Aggressive:   Return=%.2f%%, Trades=%d, Sharpe=%s%n",
                    aggResult.getTotalReturn().doubleValue(), aggResult.getTotalTrades(),
                    aggResult.getSharpeRatio(TRADING_DAYS_PER_YEAR));
            System.out.printf("  MACD(12/26/9) Standard:    Return=%.2f%%, Trades=%d, Sharpe=%s%n",
                    stdResult.getTotalReturn().doubleValue(), stdResult.getTotalTrades(),
                    stdResult.getSharpeRatio(TRADING_DAYS_PER_YEAR));
            System.out.printf("  MACD(19/39/9) Conservative: Return=%.2f%%, Trades=%d, Sharpe=%s%n",
                    conResult.getTotalReturn().doubleValue(), conResult.getTotalTrades(),
                    conResult.getSharpeRatio(TRADING_DAYS_PER_YEAR));

            // Aggressive (shorter periods) should generate more signals
            assertTrue(aggResult.getTotalTrades() >= conResult.getTotalTrades(),
                    "Aggressive MACD should generate >= trades than conservative");
        }

        @Test
        @Order(2)
        void macdSignalPeriodSensitivity() {
            var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);
            int[] signalPeriods = {5, 7, 9, 12, 15};

            System.out.println("MACD Signal Period Sensitivity (12/26/x):");
            for (int signalPeriod : signalPeriods) {
                var strategy = new MACDStrategy(12, 26, signalPeriod);
                BacktestResult result = engine.run(strategy, fullData);

                System.out.printf("  MACD(12/26/%d): Return=%.2f%%, Trades=%d, MaxDD=%.2f%%%n",
                        signalPeriod,
                        result.getTotalReturn().doubleValue(),
                        result.getTotalTrades(),
                        result.getMaxDrawdown().doubleValue());

                assertEquals(fullData.size(), result.getEquityCurve().size());
            }
        }
    }

    // ======================================================================
    //  SCENARIO 5: Strategy Tournament Across Market Segments
    // ======================================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class StrategyTournament {

        private static final List<TradingStrategy> TOURNAMENT_STRATEGIES = List.of(
                new SMACrossoverStrategy(10, 30),
                new SMACrossoverStrategy(20, 50),
                new RSIStrategy(14, 30, 70),
                new MACDStrategy(12, 26, 9)
        );

        @Test
        @Order(1)
        void tournamentOnFullData() {
            var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);

            System.out.println("\n=== Strategy Tournament: Full Period ===");
            System.out.printf("%-25s %10s %10s %10s %10s%n",
                    "Strategy", "Return%", "Sharpe", "MaxDD%", "Trades");
            System.out.println("-".repeat(75));

            BigDecimal bestReturn = null;
            String bestStrategy = null;

            for (TradingStrategy strategy : TOURNAMENT_STRATEGIES) {
                BacktestResult result = engine.run(strategy, fullData);

                System.out.printf("%-25s %9.2f%% %10s %9.2f%% %10d%n",
                        strategy.getName(),
                        result.getTotalReturn().doubleValue(),
                        result.getSharpeRatio(TRADING_DAYS_PER_YEAR),
                        result.getMaxDrawdown().doubleValue(),
                        result.getTotalTrades());

                if (bestReturn == null || result.getTotalReturn().compareTo(bestReturn) > 0) {
                    bestReturn = result.getTotalReturn();
                    bestStrategy = strategy.getName();
                }
            }

            // Print buy-and-hold benchmark
            BigDecimal buyHold = new BacktestResult("BH", INITIAL_CAPITAL).getBuyAndHoldReturn(fullData);
            System.out.printf("%-25s %9.2f%% %10s %10s %10s%n",
                    "Buy & Hold", buyHold.doubleValue(), "N/A", "N/A", "1");
            System.out.println("Best strategy: " + bestStrategy + " (" + bestReturn.setScale(2, RoundingMode.HALF_UP) + "%)");

            assertNotNull(bestStrategy);
        }

        @Test
        @Order(2)
        void tournamentAcrossMarketSegments() {
            var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);

            Map<String, List<PriceData>> segments = new LinkedHashMap<>();
            segments.put("Q1 Recovery", q1Recovery);
            segments.put("Q2 Bull Rally", q2BullRally);
            segments.put("Q3 Correction", q3Correction);
            segments.put("Q4 Year-End Rally", q4YearEndRally);

            System.out.println("\n=== Strategy Tournament: By Market Segment ===");

            Map<String, Integer> winCounts = new LinkedHashMap<>();
            for (TradingStrategy strategy : TOURNAMENT_STRATEGIES) {
                winCounts.put(strategy.getName(), 0);
            }

            for (var entry : segments.entrySet()) {
                String segmentName = entry.getKey();
                List<PriceData> segmentData = entry.getValue();

                System.out.println("\n--- " + segmentName + " ("
                        + segmentData.getFirst().date() + " to " + segmentData.getLast().date()
                        + ", " + segmentData.size() + " days) ---");

                BigDecimal bestReturn = null;
                String bestName = null;

                for (TradingStrategy strategy : TOURNAMENT_STRATEGIES) {
                    BacktestResult result = engine.run(strategy, segmentData);

                    System.out.printf("  %-25s Return=%.2f%%, Trades=%d%n",
                            strategy.getName(),
                            result.getTotalReturn().doubleValue(),
                            result.getTotalTrades());

                    if (bestReturn == null || result.getTotalReturn().compareTo(bestReturn) > 0) {
                        bestReturn = result.getTotalReturn();
                        bestName = strategy.getName();
                    }
                }

                // Buy-and-hold for segment
                BigDecimal segBuyHold = segmentData.getLast().close()
                        .subtract(segmentData.getFirst().close())
                        .divide(segmentData.getFirst().close(), 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                System.out.printf("  %-25s Return=%.2f%%%n", "Buy & Hold", segBuyHold.doubleValue());

                if (bestName != null) {
                    winCounts.merge(bestName, 1, Integer::sum);
                    System.out.println("  Winner: " + bestName);
                }
            }

            System.out.println("\n--- Segment Win Counts ---");
            for (var entry : winCounts.entrySet()) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " wins");
            }

            // Every segment should produce results (no crashes)
            assertEquals(4, segments.size());
        }

        @Test
        @Order(3)
        void noStrategyLosesMoreThanFiftyPercentOnAnySegment() {
            var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);
            BigDecimal maxLossThreshold = new BigDecimal("-50");

            List<List<PriceData>> allSegments = List.of(
                    q1Recovery, q2BullRally, q3Correction, q4YearEndRally, fullData);

            for (List<PriceData> segment : allSegments) {
                for (TradingStrategy strategy : TOURNAMENT_STRATEGIES) {
                    BacktestResult result = engine.run(strategy, segment);
                    assertTrue(result.getTotalReturn().compareTo(maxLossThreshold) > 0,
                            strategy.getName() + " should not lose >50% on any segment");
                }
            }
        }
    }

    // ======================================================================
    //  SCENARIO 6: Commission Impact Analysis
    // ======================================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class CommissionImpactAnalysis {

        @Test
        @Order(1)
        void commissionScaleImpact() {
            BigDecimal[] commissions = {
                    BigDecimal.ZERO,
                    new BigDecimal("1.00"),
                    new BigDecimal("4.95"),
                    new BigDecimal("9.99"),
                    new BigDecimal("25.00")
            };

            // Use a strategy with many trades to see commission impact clearly
            var strategy = new SMACrossoverStrategy(10, 30);

            System.out.println("\nCommission Impact Analysis - " + strategy.getName());
            System.out.printf("%-15s %12s %10s %12s%n",
                    "Commission", "Final Cap", "Return%", "Trades");
            System.out.println("-".repeat(55));

            BigDecimal previousFinalCapital = null;

            for (BigDecimal commission : commissions) {
                var engine = new BacktestEngine(INITIAL_CAPITAL, commission);
                BacktestResult result = engine.run(strategy, fullData);

                System.out.printf("$%-14s $%,11.2f %9.2f%% %10d%n",
                        commission, result.getFinalCapital().doubleValue(),
                        result.getTotalReturn().doubleValue(),
                        result.getTotalTrades());

                if (previousFinalCapital != null && result.getTotalTrades() > 0) {
                    assertTrue(result.getFinalCapital().compareTo(previousFinalCapital) <= 0,
                            "Higher commission should result in lower or equal final capital");
                }
                previousFinalCapital = result.getFinalCapital();
            }
        }

        @Test
        @Order(2)
        void highCommissionCanTurnProfitableStrategyUnprofitable() {
            var strategy = new MACDStrategy(12, 26, 9);

            var noComm = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);
            BacktestResult noCommResult = noComm.run(strategy, fullData);

            // Try progressively higher commissions
            BigDecimal commission = new BigDecimal("100.00");
            var highComm = new BacktestEngine(INITIAL_CAPITAL, commission);
            BacktestResult highCommResult = highComm.run(strategy, fullData);

            if (noCommResult.getTotalTrades() > 0) {
                // With very high commission, returns should be worse
                assertTrue(noCommResult.getTotalReturn().compareTo(highCommResult.getTotalReturn()) >= 0,
                        "Very high commissions should reduce returns");
            }
        }

        @Test
        @Order(3)
        void zeroCommissionMatchesTradeLogExactly() {
            var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);
            var strategy = new SMACrossoverStrategy(20, 50);
            BacktestResult result = engine.run(strategy, fullData);

            // With zero commission, sum of trade P/L should exactly equal total P/L
            BigDecimal tradePLSum = result.getClosedTrades().stream()
                    .map(Trade::getProfitLoss)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertEquals(0, tradePLSum.compareTo(result.getTotalProfitLoss()),
                    "Zero-commission trade P/L sum must exactly match total P/L");

            // Verify final capital = initial + total P/L
            assertEquals(0, INITIAL_CAPITAL.add(tradePLSum).compareTo(result.getFinalCapital()),
                    "Final capital = initial + sum of trade P/L with zero commission");
        }
    }

    // ======================================================================
    //  SCENARIO 7: Capital Size Scenarios
    // ======================================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class CapitalSizeScenarios {

        @Test
        @Order(1)
        void returnPercentagesAreNearlyIdenticalAcrossCapitalSizes() {
            BigDecimal[] capitals = {
                    new BigDecimal("1000.00"),
                    new BigDecimal("10000.00"),
                    new BigDecimal("100000.00"),
                    new BigDecimal("1000000.00"),
                    new BigDecimal("10000000.00"),
            };

            var strategy = new MACDStrategy(12, 26, 9);

            System.out.println("\nCapital Size Scenarios - " + strategy.getName());
            System.out.printf("%-15s %15s %10s %10s%n", "Capital", "Final", "Return%", "Shares");
            System.out.println("-".repeat(55));

            List<BigDecimal> returns = new ArrayList<>();

            for (BigDecimal capital : capitals) {
                var engine = new BacktestEngine(capital, ZERO_COMMISSION);
                BacktestResult result = engine.run(strategy, fullData);

                int firstTradeShares = result.getClosedTrades().isEmpty()
                        ? 0 : result.getClosedTrades().getFirst().getShares();

                System.out.printf("$%,-14.2f $%,14.2f %9.2f%% %10d%n",
                        capital.doubleValue(),
                        result.getFinalCapital().doubleValue(),
                        result.getTotalReturn().doubleValue(),
                        firstTradeShares);

                returns.add(result.getTotalReturn());
            }

            // Returns should be within 2% of each other (due to share rounding)
            for (int i = 1; i < returns.size(); i++) {
                BigDecimal diff = returns.get(i).subtract(returns.getFirst()).abs();
                assertTrue(diff.compareTo(new BigDecimal("2.0")) < 0,
                        "Return % should be similar across capital sizes (diff=" + diff + ")");
            }
        }

        @Test
        @Order(2)
        void verySmallCapitalStillFunctions() {
            var engine = new BacktestEngine(new BigDecimal("500.00"), ZERO_COMMISSION);
            var strategy = new SMACrossoverStrategy(20, 50);
            BacktestResult result = engine.run(strategy, fullData);

            // Even with $500 we should be able to buy at least 1 share of SPY (~$380-490)
            assertTrue(result.getFinalCapital().compareTo(BigDecimal.ZERO) > 0,
                    "Small capital should still produce valid results");
            assertEquals(fullData.size(), result.getEquityCurve().size());
        }
    }

    // ======================================================================
    //  SCENARIO 8: Risk Metrics Deep Dive
    // ======================================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class RiskMetricsDeepDive {

        @Test
        @Order(1)
        void maxDrawdownComparison() {
            var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);

            List<TradingStrategy> strategies = List.of(
                    new SMACrossoverStrategy(10, 30),
                    new SMACrossoverStrategy(20, 50),
                    new SMACrossoverStrategy(50, 200),
                    new RSIStrategy(14, 30, 70),
                    new MACDStrategy(12, 26, 9)
            );

            System.out.println("\nMax Drawdown Comparison:");
            System.out.printf("%-25s %10s %10s%n", "Strategy", "MaxDD%", "Return%");
            System.out.println("-".repeat(50));

            for (TradingStrategy strategy : strategies) {
                BacktestResult result = engine.run(strategy, fullData);
                System.out.printf("%-25s %9.2f%% %9.2f%%%n",
                        strategy.getName(),
                        result.getMaxDrawdown().doubleValue(),
                        result.getTotalReturn().doubleValue());

                // Max drawdown should be bounded
                assertTrue(result.getMaxDrawdown().compareTo(BigDecimal.ZERO) >= 0);
                assertTrue(result.getMaxDrawdown().compareTo(new BigDecimal("100")) <= 0);
            }
        }

        @Test
        @Order(2)
        void sharpeRatioIsReasonable() {
            var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);

            List<TradingStrategy> strategies = List.of(
                    new SMACrossoverStrategy(20, 50),
                    new RSIStrategy(14, 30, 70),
                    new MACDStrategy(12, 26, 9)
            );

            for (TradingStrategy strategy : strategies) {
                BacktestResult result = engine.run(strategy, fullData);
                BigDecimal sharpe = result.getSharpeRatio(TRADING_DAYS_PER_YEAR);

                // Sharpe ratio for real equity strategies typically falls in -3 to +5 range
                assertTrue(sharpe.compareTo(new BigDecimal("-5")) > 0,
                        strategy.getName() + " Sharpe should be > -5");
                assertTrue(sharpe.compareTo(new BigDecimal("10")) < 0,
                        strategy.getName() + " Sharpe should be < 10");
            }
        }

        @Test
        @Order(3)
        void profitFactorCorrelatesWithWinRate() {
            var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);

            var strategy = new SMACrossoverStrategy(10, 30);
            BacktestResult result = engine.run(strategy, fullData);

            if (result.getTotalTrades() > 0) {
                BigDecimal profitFactor = result.getProfitFactor();
                BigDecimal winRate = result.getWinRate();

                // If win rate > 50%, profit factor is more likely > 1 (but not guaranteed
                // due to asymmetric win/loss sizes)
                System.out.printf("SMA(10/30): Win Rate=%.2f%%, Profit Factor=%.2f, Avg Win=$%.2f, Avg Loss=$%.2f%n",
                        winRate.doubleValue(), profitFactor.doubleValue(),
                        result.getAverageWin().doubleValue(), result.getAverageLoss().doubleValue());

                assertTrue(profitFactor.compareTo(BigDecimal.ZERO) >= 0,
                        "Profit factor should be non-negative");
            }
        }

        @Test
        @Order(4)
        void equityCurveNeverGoesNegative() {
            var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);

            List<TradingStrategy> strategies = List.of(
                    new SMACrossoverStrategy(5, 20),
                    new SMACrossoverStrategy(20, 50),
                    new RSIStrategy(7, 30, 70),
                    new RSIStrategy(14, 25, 75),
                    new MACDStrategy(8, 17, 9),
                    new MACDStrategy(12, 26, 9)
            );

            for (TradingStrategy strategy : strategies) {
                BacktestResult result = engine.run(strategy, fullData);
                for (int i = 0; i < result.getEquityCurve().size(); i++) {
                    assertTrue(result.getEquityCurve().get(i).compareTo(BigDecimal.ZERO) > 0,
                            strategy.getName() + " equity should never go negative (index " + i + ")");
                }
            }
        }
    }

    // ======================================================================
    //  SCENARIO 9: Head-to-Head Strategy Comparison with Report
    // ======================================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class HeadToHeadComparison {

        @Test
        @Order(1)
        void fullComparisonReportAllStrategies() {
            var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);

            List<TradingStrategy> strategies = List.of(
                    new SMACrossoverStrategy(5, 20),
                    new SMACrossoverStrategy(10, 30),
                    new SMACrossoverStrategy(20, 50),
                    new SMACrossoverStrategy(50, 200),
                    new RSIStrategy(7, 30, 70),
                    new RSIStrategy(14, 30, 70),
                    new RSIStrategy(14, 25, 75),
                    new RSIStrategy(21, 35, 65),
                    new MACDStrategy(8, 17, 9),
                    new MACDStrategy(12, 26, 9),
                    new MACDStrategy(19, 39, 9)
            );

            List<BacktestResult> allResults = new ArrayList<>();
            for (TradingStrategy strategy : strategies) {
                allResults.add(engine.run(strategy, fullData));
            }

            // Capture the comparison report
            PrintStream original = System.out;
            var capture = new ByteArrayOutputStream();
            System.setOut(new PrintStream(capture));
            try {
                BacktestReportFormatter.printComparison(allResults, fullData);
            } finally {
                System.setOut(original);
            }

            String report = capture.toString();

            // Print to console for visibility
            System.out.println(report);

            // Verify report contains all strategies
            for (BacktestResult result : allResults) {
                assertTrue(report.contains(result.getStrategyName()),
                        "Report should contain " + result.getStrategyName());
            }
            assertTrue(report.contains("STRATEGY COMPARISON SUMMARY"));
            assertTrue(report.contains("Best performing strategy:"));
        }

        @Test
        @Order(2)
        void rankStrategiesByRiskAdjustedReturn() {
            var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);

            List<TradingStrategy> strategies = List.of(
                    new SMACrossoverStrategy(20, 50),
                    new RSIStrategy(14, 30, 70),
                    new MACDStrategy(12, 26, 9)
            );

            System.out.println("\n=== Risk-Adjusted Return Rankings ===");

            record StrategyRanking(String name, BigDecimal ret, BigDecimal sharpe,
                                   BigDecimal maxDD, long trades) {}

            List<StrategyRanking> rankings = new ArrayList<>();

            for (TradingStrategy strategy : strategies) {
                BacktestResult result = engine.run(strategy, fullData);
                rankings.add(new StrategyRanking(
                        strategy.getName(),
                        result.getTotalReturn(),
                        result.getSharpeRatio(TRADING_DAYS_PER_YEAR),
                        result.getMaxDrawdown(),
                        result.getTotalTrades()));
            }

            // Rank by Sharpe ratio (best risk-adjusted measure)
            rankings.sort(Comparator.comparing(StrategyRanking::sharpe).reversed());

            System.out.printf("%-25s %10s %10s %10s %8s%n",
                    "Strategy", "Return%", "Sharpe", "MaxDD%", "Trades");
            System.out.println("-".repeat(70));
            int rank = 1;
            for (var r : rankings) {
                System.out.printf("#%d %-23s %9.2f%% %10s %9.2f%% %8d%n",
                        rank++, r.name(), r.ret().doubleValue(), r.sharpe(),
                        r.maxDD().doubleValue(), r.trades());
            }

            // At least one strategy should have been evaluated
            assertFalse(rankings.isEmpty());
        }
    }

    // ======================================================================
    //  SCENARIO 10: Edge Cases and Stress Testing
    // ======================================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class EdgeCasesAndStressTesting {

        @Test
        @Order(1)
        void minimumDataForEachStrategy() {
            var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);

            // SMA(50/200) needs at least 201 bars for warmup
            var sma = new SMACrossoverStrategy(50, 200);
            assertTrue(sma.getWarmupPeriod() == 200);

            // RSI(14, 30/70) needs 15 bars
            var rsi = new RSIStrategy(14, 30, 70);
            assertEquals(15, rsi.getWarmupPeriod());

            // MACD(12/26/9) needs 35 bars
            var macd = new MACDStrategy(12, 26, 9);
            assertEquals(35, macd.getWarmupPeriod());

            // Test with just enough data for each strategy
            // Use 36 bars for MACD (minimum + 1)
            List<PriceData> minData = fullData.subList(0, 36);
            BacktestResult macdResult = engine.run(macd, minData);
            assertEquals(36, macdResult.getEquityCurve().size());

            // Use 16 bars for RSI
            List<PriceData> rsiMinData = fullData.subList(0, 16);
            BacktestResult rsiResult = engine.run(rsi, rsiMinData);
            assertEquals(16, rsiResult.getEquityCurve().size());
        }

        @Test
        @Order(2)
        void allStrategiesHandleEntireDatasetGracefully() {
            var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);

            // Run every possible strategy configuration without errors
            List<TradingStrategy> allStrategies = List.of(
                    new SMACrossoverStrategy(5, 10),
                    new SMACrossoverStrategy(5, 20),
                    new SMACrossoverStrategy(10, 30),
                    new SMACrossoverStrategy(20, 50),
                    new SMACrossoverStrategy(50, 100),
                    new SMACrossoverStrategy(50, 200),
                    new RSIStrategy(7, 20, 80),
                    new RSIStrategy(7, 30, 70),
                    new RSIStrategy(14, 25, 75),
                    new RSIStrategy(14, 30, 70),
                    new RSIStrategy(14, 35, 65),
                    new RSIStrategy(21, 30, 70),
                    new RSIStrategy(28, 30, 70),
                    new MACDStrategy(8, 17, 9),
                    new MACDStrategy(12, 26, 9),
                    new MACDStrategy(12, 26, 5),
                    new MACDStrategy(12, 26, 15),
                    new MACDStrategy(19, 39, 9)
            );

            for (TradingStrategy strategy : allStrategies) {
                assertDoesNotThrow(() -> engine.run(strategy, fullData),
                        "Strategy " + strategy.getName() + " should handle full dataset without error");
            }
        }

        @Test
        @Order(3)
        void singleBarSegmentProducesZeroTrades() {
            var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);
            List<PriceData> oneBar = fullData.subList(0, 1);

            // Every strategy should handle a single bar (no trades possible)
            var rsi = new RSIStrategy(14, 30, 70);
            BacktestResult result = engine.run(rsi, oneBar);
            assertEquals(0, result.getTotalTrades());
            assertEquals(0, result.getFinalCapital().compareTo(INITIAL_CAPITAL));
        }

        @Test
        @Order(4)
        void multipleRunsOnSameEngineDontLeakState() {
            var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);
            var strategy = new SMACrossoverStrategy(20, 50);

            BacktestResult run1 = engine.run(strategy, fullData);
            BacktestResult run2 = engine.run(strategy, fullData);

            assertEquals(0, run1.getFinalCapital().compareTo(run2.getFinalCapital()),
                    "Same engine, same strategy, same data should produce identical results");
            assertEquals(run1.getTotalTrades(), run2.getTotalTrades());
            assertEquals(run1.getEquityCurve().size(), run2.getEquityCurve().size());
        }
    }

    // ======================================================================
    //  SCENARIO 11: Comprehensive Scenario Report
    // ======================================================================

    @Test
    @Order(100)
    void generateComprehensiveScenarioReport() {
        var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);

        List<TradingStrategy> strategies = List.of(
                new SMACrossoverStrategy(10, 30),
                new SMACrossoverStrategy(20, 50),
                new SMACrossoverStrategy(50, 200),
                new RSIStrategy(14, 30, 70),
                new RSIStrategy(14, 25, 75),
                new MACDStrategy(12, 26, 9)
        );

        System.out.println("\n" + "=".repeat(100));
        System.out.println("  COMPREHENSIVE SPY BACKTEST SCENARIO REPORT");
        System.out.println("  Data: " + fullData.getFirst().date() + " to " + fullData.getLast().date()
                + " (" + fullData.size() + " trading days)");
        System.out.println("  Initial Capital: $" + INITIAL_CAPITAL.setScale(2, RoundingMode.HALF_UP));
        System.out.println("=".repeat(100));

        // Full period results
        System.out.println("\n--- Full Period Performance ---");
        System.out.printf("%-25s %10s %10s %10s %10s %10s %10s%n",
                "Strategy", "Return%", "P/L", "Sharpe", "MaxDD%", "Trades", "WinRate%");
        System.out.println("-".repeat(95));

        BigDecimal buyHoldReturn = fullData.getLast().close()
                .subtract(fullData.getFirst().close())
                .divide(fullData.getFirst().close(), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        System.out.printf("%-25s %9.2f%% %10s %10s %10s %10s %10s%n",
                "Buy & Hold", buyHoldReturn.doubleValue(), "N/A", "N/A", "N/A", "1", "N/A");

        List<BacktestResult> allResults = new ArrayList<>();
        for (TradingStrategy strategy : strategies) {
            BacktestResult result = engine.run(strategy, fullData);
            allResults.add(result);

            System.out.printf("%-25s %9.2f%% $%,9.2f %10s %9.2f%% %10d %9.2f%%%n",
                    strategy.getName(),
                    result.getTotalReturn().doubleValue(),
                    result.getTotalProfitLoss().doubleValue(),
                    result.getSharpeRatio(TRADING_DAYS_PER_YEAR),
                    result.getMaxDrawdown().doubleValue(),
                    result.getTotalTrades(),
                    result.getWinRate().doubleValue());
        }

        // Find best by return and by Sharpe
        BacktestResult bestReturn = allResults.stream()
                .max(Comparator.comparing(BacktestResult::getTotalReturn))
                .orElseThrow();
        BacktestResult bestSharpe = allResults.stream()
                .max(Comparator.comparing(r -> r.getSharpeRatio(TRADING_DAYS_PER_YEAR)))
                .orElseThrow();
        BacktestResult lowestDD = allResults.stream()
                .min(Comparator.comparing(BacktestResult::getMaxDrawdown))
                .orElseThrow();

        System.out.println();
        System.out.println("  Best Return:      " + bestReturn.getStrategyName()
                + " (" + bestReturn.getTotalReturn().setScale(2, RoundingMode.HALF_UP) + "%)");
        System.out.println("  Best Sharpe:      " + bestSharpe.getStrategyName()
                + " (" + bestSharpe.getSharpeRatio(TRADING_DAYS_PER_YEAR) + ")");
        System.out.println("  Lowest Drawdown:  " + lowestDD.getStrategyName()
                + " (" + lowestDD.getMaxDrawdown().setScale(2, RoundingMode.HALF_UP) + "%)");

        // Verify all results are valid
        for (BacktestResult result : allResults) {
            assertTrue(result.getFinalCapital().compareTo(BigDecimal.ZERO) > 0);
            assertEquals(fullData.size(), result.getEquityCurve().size());
        }

        System.out.println("\n" + "=".repeat(100));
        System.out.println("  DISCLAIMER: This is for educational purposes only.");
        System.out.println("  Past performance does not guarantee future results.");
        System.out.println("=".repeat(100));
    }
}
