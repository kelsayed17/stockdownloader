package com.stockdownloader.e2e;

import com.stockdownloader.analysis.PatternAnalyzer;
import com.stockdownloader.data.CsvPriceDataLoader;
import com.stockdownloader.model.HistoricalData;
import com.stockdownloader.model.PatternResult;
import com.stockdownloader.model.PriceData;
import com.stockdownloader.util.BigDecimalMath;

import com.google.common.collect.HashMultimap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end test for the pattern analysis pipeline that mirrors TrendAnalysisApp.
 * Uses real SPY price data to generate price movement patterns for multiple
 * simulated tickers, then runs the full PatternAnalyzer pipeline.
 *
 * Data flow: PriceData -> pattern generation (like YahooHistoricalClient)
 *   -> HashMultimap -> PatternAnalyzer.analyze() -> SortedSet<PatternResult>
 *   -> PatternAnalyzer.printResults()
 *
 * This exercises: PriceData, HistoricalData, PatternResult, PatternAnalyzer,
 * BigDecimalMath, and the Guava HashMultimap pattern storage.
 */
class PatternAnalysisE2ETest {

    private static final int PATTERN_DAYS = 7;

    private static List<PriceData> spyData;
    private static HashMultimap<String, String> allPatterns;
    private static SortedSet<PatternResult> analysisResults;

    @BeforeAll
    static void generatePatternsFromRealData() {
        spyData = CsvPriceDataLoader.loadFromStream(
                PatternAnalysisE2ETest.class.getResourceAsStream("/test-price-data.csv"));
        assertFalse(spyData.isEmpty());

        allPatterns = HashMultimap.create();

        // Simulate what TrendAnalysisApp does: for each ticker, generate 7-day patterns
        // We use different segments of real SPY data as if they were different tickers
        String[] simulatedTickers = {"AAPL", "MSFT", "GOOGL", "AMZN", "META",
                "NVDA", "TSLA", "JPM", "V", "UNH"};

        for (int t = 0; t < simulatedTickers.length; t++) {
            int startIdx = t * 20; // Offset each ticker's data window
            if (startIdx + PATTERN_DAYS + 1 >= spyData.size()) break;

            HistoricalData hd = generatePatterns(simulatedTickers[t], startIdx);
            allPatterns.putAll(hd.getPatterns());
        }

        // Also generate overlapping patterns to create pattern frequency
        for (int startIdx = 0; startIdx + PATTERN_DAYS + 1 < spyData.size(); startIdx += 5) {
            String syntheticTicker = "SYN" + startIdx;
            HistoricalData hd = generatePatterns(syntheticTicker, startIdx);
            allPatterns.putAll(hd.getPatterns());
        }

        assertTrue(allPatterns.size() > 0, "Should have generated patterns");

        // Run the full analysis pipeline
        analysisResults = PatternAnalyzer.analyze(allPatterns);
    }

    /**
     * Generates patterns for a ticker starting at the given index.
     * Mirrors YahooHistoricalClient.parsePatterns logic.
     */
    private static HistoricalData generatePatterns(String ticker, int startIdx) {
        var data = new HistoricalData(ticker);
        var mc = new MathContext(2);
        List<Integer> upDownList = new ArrayList<>();

        for (int i = startIdx + 1; i <= startIdx + PATTERN_DAYS && i < spyData.size(); i++) {
            BigDecimal closePrice = spyData.get(i).close();
            BigDecimal prevClose = spyData.get(i - 1).close();

            BigDecimal closeChange = closePrice.subtract(prevClose)
                    .divide(prevClose, 10, RoundingMode.CEILING)
                    .multiply(new BigDecimal(100), mc);

            upDownList.add(closeChange.signum());
            data.getPatterns().put(upDownList.toString(), ticker);
        }

        return data;
    }

    // ========== Pattern Generation ==========

    @Test
    void patternsGeneratedFromRealData() {
        assertTrue(allPatterns.keySet().size() > 0,
                "Should have multiple distinct pattern keys");
        assertTrue(allPatterns.size() > 10,
                "Should have many pattern-ticker entries");
    }

    @Test
    void patternKeysHaveExpectedFormat() {
        for (String key : allPatterns.keySet()) {
            assertTrue(key.startsWith("["), "Pattern key should start with [");
            assertTrue(key.endsWith("]"), "Pattern key should end with ]");
            // Pattern values should be -1, 0, or 1
            String inner = key.substring(1, key.length() - 1);
            for (String val : inner.split(", ")) {
                int v = Integer.parseInt(val.trim());
                assertTrue(v >= -1 && v <= 1,
                        "Pattern value should be -1, 0, or 1, got " + v);
            }
        }
    }

    @Test
    void patternsAssociatedWithCorrectTickers() {
        // Verify that synthetic tickers are associated with patterns
        boolean foundSynthetic = false;
        for (String key : allPatterns.keySet()) {
            Set<String> tickers = allPatterns.get(key);
            for (String ticker : tickers) {
                if (ticker.startsWith("SYN")) {
                    foundSynthetic = true;
                    break;
                }
            }
            if (foundSynthetic) break;
        }
        assertTrue(foundSynthetic, "Should find synthetic tickers in patterns");
    }

    // ========== PatternAnalyzer Results ==========

    @Test
    void analyzerProducesResults() {
        assertNotNull(analysisResults);
        assertFalse(analysisResults.isEmpty(), "Analysis should produce results");
    }

    @Test
    void resultsAreSortedByFrequencyDescending() {
        BigDecimal prevFreq = null;
        for (PatternResult result : analysisResults) {
            if (prevFreq != null) {
                assertTrue(result.getPatternFreq().compareTo(prevFreq) <= 0,
                        "Results should be sorted by frequency descending");
            }
            prevFreq = result.getPatternFreq();
        }
    }

    @Test
    void patternResultFieldsPopulated() {
        for (PatternResult result : analysisResults) {
            assertNotNull(result.getPattern(), "Pattern should not be null");
            assertNotNull(result.getSimilar(), "Similar pattern should not be null");
            assertNotNull(result.getOffset(), "Offset pattern should not be null");
            assertNotNull(result.getPatternFreq(), "Pattern frequency should not be null");
            assertNotNull(result.getSimilarFreq(), "Similar frequency should not be null");
            assertNotNull(result.getOffsetFreq(), "Offset frequency should not be null");
            assertNotNull(result.getAccuracy(), "Accuracy should not be null");
            assertNotNull(result.getPatternSymbols(), "Pattern symbols should not be null");
        }
    }

    @Test
    void similarPatternIsInverse() {
        for (PatternResult result : analysisResults) {
            String pattern = result.getPattern();
            String similar = result.getSimilar();

            // Parse the first element and check it's inverted
            String patternInner = pattern.substring(1, pattern.length() - 1);
            String similarInner = similar.substring(1, similar.length() - 1);

            String[] patternVals = patternInner.split(", ");
            String[] similarVals = similarInner.split(", ");

            assertEquals(patternVals.length, similarVals.length,
                    "Pattern and similar should have same length");

            // First element should be inverted
            int first = Integer.parseInt(patternVals[0].trim());
            int similarFirst = Integer.parseInt(similarVals[0].trim());
            if (first == 1) assertEquals(-1, similarFirst);
            else if (first == -1) assertEquals(1, similarFirst);
            else assertEquals(0, similarFirst);

            // Remaining elements should be the same
            for (int i = 1; i < patternVals.length; i++) {
                assertEquals(patternVals[i].trim(), similarVals[i].trim(),
                        "Non-first elements should be identical between pattern and similar");
            }
        }
    }

    @Test
    void offsetPatternRemovesFirstElement() {
        for (PatternResult result : analysisResults) {
            String pattern = result.getPattern();
            String offset = result.getOffset();

            String patternInner = pattern.substring(1, pattern.length() - 1);
            String[] patternVals = patternInner.split(", ");

            // The offset is derived by the PatternAnalyzer via ArrayList.removeFirst()
            // and then toString(), so it should contain all elements after the first.
            // For a pattern like [1, -1, 0], the offset should be [-1, 0]
            if (patternVals.length > 1) {
                String offsetInner = offset.substring(1, offset.length() - 1);
                String[] offsetVals = offsetInner.split(", ");

                assertEquals(patternVals.length - 1, offsetVals.length,
                        "Offset pattern should have one fewer element for pattern " + pattern);

                // Offset values should match pattern[1..n]
                for (int i = 0; i < offsetVals.length; i++) {
                    assertEquals(patternVals[i + 1].trim(), offsetVals[i].trim(),
                            "Offset should match pattern shifted by one");
                }
            }
        }
    }

    @Test
    void accuracyPercentageBounds() {
        for (PatternResult result : analysisResults) {
            BigDecimal accuracy = result.getAccuracy();
            assertTrue(accuracy.compareTo(BigDecimal.ZERO) >= 0,
                    "Accuracy should be >= 0%");
            assertTrue(accuracy.compareTo(new BigDecimal("100")) <= 0,
                    "Accuracy should be <= 100%");
        }
    }

    @Test
    void accuracyFormulaVerification() {
        for (PatternResult result : analysisResults) {
            BigDecimal pf = result.getPatternFreq();
            BigDecimal sf = result.getSimilarFreq();
            BigDecimal total = BigDecimalMath.add(pf, sf);

            if (total.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal expectedAccuracy = BigDecimalMath.scale2(
                        BigDecimalMath.divide(
                                BigDecimalMath.multiply(pf, BigDecimal.valueOf(100)),
                                total));
                assertEquals(0, expectedAccuracy.compareTo(result.getAccuracy()),
                        "Accuracy should be (patternFreq * 100) / (patternFreq + similarFreq)");
            }
        }
    }

    @Test
    void patternSymbolsAreNonEmpty() {
        for (PatternResult result : analysisResults) {
            assertFalse(result.getPatternSymbols().isEmpty(),
                    "Pattern should have at least one associated symbol");
            // Frequency should match number of symbols
            assertEquals(result.getPatternSymbols().size(),
                    result.getPatternFreq().intValue(),
                    "Pattern frequency should match number of symbols");
        }
    }

    // ========== Report Generation ==========

    @Test
    void printResultsGeneratesOutput() {
        PrintStream original = System.out;
        var capture = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capture));

        try {
            PatternAnalyzer.printResults(analysisResults);
        } finally {
            System.setOut(original);
        }

        String output = capture.toString();
        assertTrue(output.contains("Top 10 patterns"), "Should contain header");
        assertTrue(output.contains("Top 10 patterns offset by one day"),
                "Should contain offset header");
        assertTrue(output.contains("Frequency:"), "Should contain frequency column");
        assertTrue(output.contains("Percentage:"), "Should contain percentage column");
        assertTrue(output.contains("Pattern:"), "Should contain pattern column");
        assertTrue(output.contains("Stocks:"), "Should contain stocks column");
    }

    // ========== HistoricalData Integration ==========

    @Test
    void historicalDataPatternStorage() {
        HistoricalData hd = new HistoricalData("TEST");
        hd.setHighestPriceThisQtr(new BigDecimal("200.00"));
        hd.setLowestPriceThisQtr(new BigDecimal("150.00"));
        hd.setHighestPriceLastQtr(new BigDecimal("195.00"));
        hd.setLowestPriceLastQtr(new BigDecimal("145.00"));

        // Generate patterns
        HistoricalData generated = generatePatterns("TEST", 0);

        assertFalse(generated.getPatterns().isEmpty(),
                "Generated patterns should not be empty");
        assertEquals("TEST", generated.getTicker());
    }

    @Test
    void multipleTickersGenerateDistinctPatterns() {
        HistoricalData hd1 = generatePatterns("TICK1", 0);
        HistoricalData hd2 = generatePatterns("TICK2", 50);

        // Different starting points may generate different patterns
        HashMultimap<String, String> combined = HashMultimap.create();
        combined.putAll(hd1.getPatterns());
        combined.putAll(hd2.getPatterns());

        // Combined should have entries from both tickers
        boolean foundTick1 = false;
        boolean foundTick2 = false;
        for (String key : combined.keySet()) {
            if (combined.get(key).contains("TICK1")) foundTick1 = true;
            if (combined.get(key).contains("TICK2")) foundTick2 = true;
        }
        assertTrue(foundTick1, "Should find TICK1 in combined patterns");
        assertTrue(foundTick2, "Should find TICK2 in combined patterns");
    }

    // ========== PatternResult Record Behavior ==========

    @Test
    void patternResultComparableOrdering() {
        // PatternResult's compareTo sorts by frequency descending
        var result1 = new PatternResult("[1, 0]", "[-1, 0]", "[0]",
                BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE,
                new BigDecimal("90.91"), Set.of("A"), Set.of("B"), Set.of("C"));
        var result2 = new PatternResult("[0, 1]", "[0, -1]", "[1]",
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                new BigDecimal("50.00"), Set.of("D"), Set.of("E"), Set.of("F"));

        assertTrue(result1.compareTo(result2) < 0,
                "Higher frequency should sort first (descending)");
    }

    // ========== Full Pipeline with All Pattern Lengths ==========

    @Test
    void patternsGrowIncrementallyPerBar() {
        // The YahooHistoricalClient adds to the upDownList incrementally:
        // After 1 bar: [1]
        // After 2 bars: [1, -1]
        // After 3 bars: [1, -1, 0]
        // etc.
        // This means patterns of length 1 through PATTERN_DAYS should exist
        HistoricalData hd = generatePatterns("GROW", 10);

        boolean foundShort = false;
        boolean foundLong = false;
        for (String key : hd.getPatterns().keySet()) {
            String inner = key.substring(1, key.length() - 1);
            int elements = inner.split(", ").length;
            if (elements == 1) foundShort = true;
            if (elements >= 5) foundLong = true;
        }
        assertTrue(foundShort, "Should have short (1-element) patterns");
        assertTrue(foundLong, "Should have longer patterns");
    }
}
