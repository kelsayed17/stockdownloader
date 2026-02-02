package com.stockdownloader.e2e;

import com.stockdownloader.data.CsvPriceDataLoader;
import com.stockdownloader.model.PriceData;
import com.stockdownloader.util.BigDecimalMath;
import com.stockdownloader.util.CsvParser;
import com.stockdownloader.util.DateHelper;
import com.stockdownloader.util.FileHelper;
import com.stockdownloader.util.MovingAverageCalculator;
import com.stockdownloader.util.RetryExecutor;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.io.TempDir;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end test that exercises all utility classes working in concert:
 *   DateHelper + FileHelper + RetryExecutor + BigDecimalMath + CsvParser
 *   + MovingAverageCalculator
 *
 * This test validates that the utility layer correctly supports the
 * application's data processing, file I/O, date handling, mathematical
 * computations, and retry patterns that are used across the entire system.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UtilityConcertE2ETest {

    private static final Logger LOGGER = Logger.getLogger(UtilityConcertE2ETest.class.getName());
    private static List<PriceData> spyData;

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void loadData() {
        spyData = CsvPriceDataLoader.loadFromStream(
                UtilityConcertE2ETest.class.getResourceAsStream("/test-price-data.csv"));
        assertFalse(spyData.isEmpty());
    }

    // ========== DateHelper ==========

    @Test
    @Order(1)
    void dateHelperDefaultConstructorUsesToday() {
        var helper = new DateHelper();
        LocalDate today = LocalDate.now();

        // Today market day should be today or a recent weekday
        LocalDate todayMarket = helper.getTodayMarket();
        assertNotNull(todayMarket);
        assertTrue(todayMarket.getDayOfWeek() != DayOfWeek.SATURDAY);
        assertTrue(todayMarket.getDayOfWeek() != DayOfWeek.SUNDAY);
    }

    @Test
    @Order(2)
    void dateHelperMarketDayAdjustment() {
        // Saturday -> Friday
        LocalDate saturday = LocalDate.of(2024, 1, 6); // Saturday
        assertEquals(LocalDate.of(2024, 1, 5), DateHelper.adjustToMarketDay(saturday));

        // Sunday -> Friday
        LocalDate sunday = LocalDate.of(2024, 1, 7);
        assertEquals(LocalDate.of(2024, 1, 5), DateHelper.adjustToMarketDay(sunday));

        // Monday stays Monday
        LocalDate monday = LocalDate.of(2024, 1, 8);
        assertEquals(monday, DateHelper.adjustToMarketDay(monday));
    }

    @Test
    @Order(3)
    void dateHelperTomorrowIsAfterToday() {
        var helper = new DateHelper(LocalDate.of(2024, 1, 10)); // Wednesday
        LocalDate today = helper.getTodayMarket();
        LocalDate tomorrow = helper.getTomorrowMarket();
        LocalDate yesterday = helper.getYesterdayMarket();

        assertTrue(tomorrow.isAfter(today), "Tomorrow should be after today");
        assertTrue(yesterday.isBefore(today), "Yesterday should be before today");
    }

    @Test
    @Order(4)
    void dateHelperFormats() {
        var helper = new DateHelper(LocalDate.of(2024, 6, 15)); // Saturday
        // Should adjust to Friday June 14
        assertEquals("06/14/2024", helper.getToday());

        // Verify multiple format accessors
        assertNotNull(helper.getCurrentMonth());
        assertNotNull(helper.getCurrentDay());
        assertNotNull(helper.getCurrentYear());
        assertNotNull(helper.getSixMonthsAgo());
        assertNotNull(helper.getFromMonth());
        assertNotNull(helper.getFromDay());
        assertNotNull(helper.getFromYear());
    }

    @Test
    @Order(5)
    void dateHelperSixMonthsAgo() {
        var helper = new DateHelper(LocalDate.of(2024, 7, 15));
        String sixMonthsAgo = helper.getSixMonthsAgo();
        // Should be around January 2024
        assertTrue(sixMonthsAgo.contains("01/15/2024"));
    }

    @Test
    @Order(6)
    void dateHelperWeekdayStressTest() {
        // Test every day in a year to ensure no crashes
        LocalDate start = LocalDate.of(2024, 1, 1);
        for (int i = 0; i < 366; i++) {
            LocalDate date = start.plusDays(i);
            var helper = new DateHelper(date);

            // These should never throw
            assertNotNull(helper.getTodayMarket());
            assertNotNull(helper.getTomorrowMarket());
            assertNotNull(helper.getYesterdayMarket());

            // Market days should never be on weekends
            assertTrue(helper.getTodayMarket().getDayOfWeek() != DayOfWeek.SATURDAY);
            assertTrue(helper.getTodayMarket().getDayOfWeek() != DayOfWeek.SUNDAY);
            assertTrue(helper.getTomorrowMarket().getDayOfWeek() != DayOfWeek.SATURDAY);
            assertTrue(helper.getTomorrowMarket().getDayOfWeek() != DayOfWeek.SUNDAY);
            assertTrue(helper.getYesterdayMarket().getDayOfWeek() != DayOfWeek.SATURDAY);
            assertTrue(helper.getYesterdayMarket().getDayOfWeek() != DayOfWeek.SUNDAY);
        }
    }

    // ========== FileHelper ==========

    @Test
    @Order(7)
    void fileHelperWriteAndReadLines() {
        String filepath = tempDir.resolve("test-lines.txt").toString();
        Set<String> lines = new TreeSet<>(Set.of("AAPL", "MSFT", "GOOGL", "AMZN"));

        FileHelper.writeLines(lines, filepath);
        TreeSet<String> readBack = FileHelper.readLines(filepath);

        assertEquals(4, readBack.size());
        assertTrue(readBack.contains("AAPL"));
        assertTrue(readBack.contains("MSFT"));
        assertTrue(readBack.contains("GOOGL"));
        assertTrue(readBack.contains("AMZN"));
    }

    @Test
    @Order(8)
    void fileHelperAppendLine() {
        String filepath = tempDir.resolve("test-append.txt").toString();

        FileHelper.appendLine("TICK1", filepath);
        FileHelper.appendLine("TICK2", filepath);
        FileHelper.appendLine("TICK3", filepath);

        TreeSet<String> lines = FileHelper.readLines(filepath);
        assertEquals(3, lines.size());
        assertTrue(lines.contains("TICK1"));
        assertTrue(lines.contains("TICK2"));
        assertTrue(lines.contains("TICK3"));
    }

    @Test
    @Order(9)
    void fileHelperWriteContent() {
        String filepath = tempDir.resolve("test-content.txt").toString();
        String content = "[AAPL, MSFT, GOOGL]";

        FileHelper.writeContent(content, filepath);
        TreeSet<String> lines = FileHelper.readLines(filepath);

        // writeContent removes [ and ] characters
        assertFalse(lines.isEmpty());
        String joined = String.join("", lines);
        assertFalse(joined.contains("["));
        assertFalse(joined.contains("]"));
    }

    @Test
    @Order(10)
    void fileHelperReadCsvLines() {
        String filepath = tempDir.resolve("test-csv.txt").toString();
        FileHelper.writeLines(Set.of("AAPL,MSFT,GOOGL"), filepath);

        TreeSet<String> tickers = FileHelper.readCsvLines(filepath);
        assertTrue(tickers.contains("AAPL"));
        assertTrue(tickers.contains("MSFT"));
        assertTrue(tickers.contains("GOOGL"));
    }

    @Test
    @Order(11)
    void fileHelperDeleteFile() {
        String filepath = tempDir.resolve("test-delete.txt").toString();
        FileHelper.writeLines(Set.of("data"), filepath);

        assertTrue(FileHelper.deleteFile(filepath));
        // Reading deleted file should return empty
        TreeSet<String> lines = FileHelper.readLines(filepath);
        assertTrue(lines.isEmpty());
    }

    @Test
    @Order(12)
    void fileHelperReadNonexistentFileReturnsEmpty() {
        TreeSet<String> lines = FileHelper.readLines(tempDir.resolve("nonexistent.txt").toString());
        assertTrue(lines.isEmpty());
    }

    @Test
    @Order(13)
    void fileHelperDeleteNonexistentReturnsFalse() {
        assertFalse(FileHelper.deleteFile(tempDir.resolve("nonexistent.txt").toString()));
    }

    // ========== FileHelper + DateHelper Integration ==========

    @Test
    @Order(14)
    void fileHelperAndDateHelperIntegration() {
        // Simulate the incomplete file workflow from TrendAnalysisApp
        String incompleteFile = tempDir.resolve("incomplete.txt").toString();

        var dateHelper = new DateHelper(LocalDate.of(2024, 3, 15));

        // Write some tickers with dates
        FileHelper.appendLine("AAPL_" + dateHelper.getToday(), incompleteFile);
        FileHelper.appendLine("MSFT_" + dateHelper.getYesterday(), incompleteFile);

        TreeSet<String> incomplete = FileHelper.readLines(incompleteFile);
        assertEquals(2, incomplete.size());

        // Clean up
        FileHelper.deleteFile(incompleteFile);
    }

    // ========== RetryExecutor ==========

    @Test
    @Order(15)
    void retryExecutorSucceedsOnFirstTry() {
        AtomicInteger attempts = new AtomicInteger(0);

        RetryExecutor.execute(() -> {
            attempts.incrementAndGet();
            // Success on first try
        }, LOGGER, "successful operation");

        assertEquals(1, attempts.get(), "Should only attempt once on success");
    }

    @Test
    @Order(16)
    void retryExecutorRetriesOnFailure() {
        AtomicInteger attempts = new AtomicInteger(0);

        RetryExecutor.execute(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new RuntimeException("Simulated failure");
            }
        }, 3, LOGGER, "retrying operation");

        assertEquals(3, attempts.get(), "Should retry until success");
    }

    @Test
    @Order(17)
    void retryExecutorExhaustsRetries() {
        AtomicInteger attempts = new AtomicInteger(0);

        RetryExecutor.execute(() -> {
            attempts.incrementAndGet();
            throw new RuntimeException("Always fails");
        }, 3, LOGGER, "always failing operation");

        assertEquals(4, attempts.get(), "Should attempt maxRetries + 1 times (0..3)");
    }

    @Test
    @Order(18)
    void retryExecutorSupplierReturnsValue() {
        String result = RetryExecutor.execute(
                () -> "success",
                3, LOGGER, "supplier operation");

        assertEquals("success", result);
    }

    @Test
    @Order(19)
    void retryExecutorSupplierRetriesAndReturns() {
        AtomicInteger attempts = new AtomicInteger(0);

        String result = RetryExecutor.execute(() -> {
            if (attempts.incrementAndGet() < 2) {
                throw new RuntimeException("Temporary failure");
            }
            return "recovered";
        }, 3, LOGGER, "supplier retry");

        assertEquals("recovered", result);
        assertEquals(2, attempts.get());
    }

    @Test
    @Order(20)
    void retryExecutorSupplierReturnsNullOnExhaustion() {
        String result = RetryExecutor.execute(() -> {
            throw new RuntimeException("Always fails");
        }, 2, LOGGER, "supplier exhaust");

        assertNull(result, "Should return null when all retries exhausted");
    }

    // ========== BigDecimalMath ==========

    @Test
    @Order(21)
    void bigDecimalMathDivisionSafeOnZero() {
        BigDecimal result = BigDecimalMath.divide(BigDecimal.TEN, BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO, result, "Division by zero should return ZERO");
    }

    @Test
    @Order(22)
    void bigDecimalMathDivisionWithCustomScale() {
        BigDecimal result = BigDecimalMath.divide(BigDecimal.ONE, BigDecimal.valueOf(3), 2);
        assertEquals(new BigDecimal("0.33"), result);
    }

    @Test
    @Order(23)
    void bigDecimalMathPercentChange() {
        BigDecimal from = new BigDecimal("100");
        BigDecimal to = new BigDecimal("110");
        BigDecimal pctChange = BigDecimalMath.percentChange(from, to);
        assertEquals(0, new BigDecimal("10.000000").compareTo(pctChange));

        // Negative change
        BigDecimal negChange = BigDecimalMath.percentChange(new BigDecimal("200"), new BigDecimal("180"));
        assertTrue(negChange.compareTo(BigDecimal.ZERO) < 0);

        // Zero from
        assertEquals(BigDecimal.ZERO, BigDecimalMath.percentChange(BigDecimal.ZERO, BigDecimal.TEN));
    }

    @Test
    @Order(24)
    void bigDecimalMathAverage() {
        BigDecimal avg = BigDecimalMath.average(
                new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("30"));
        // (10 + 20 + 30) / 3 = 20
        assertEquals(0, new BigDecimal("20").compareTo(avg.setScale(0, RoundingMode.HALF_UP)));

        // Average skips zeros
        BigDecimal avgWithZero = BigDecimalMath.average(
                new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("20"));
        // (10 + 20) / 2 = 15
        assertEquals(0, new BigDecimal("15").compareTo(avgWithZero.setScale(0, RoundingMode.HALF_UP)));

        // All zeros
        assertEquals(BigDecimal.ZERO, BigDecimalMath.average(BigDecimal.ZERO, BigDecimal.ZERO));

        // Null/empty
        assertEquals(BigDecimal.ZERO, BigDecimalMath.average());
    }

    @Test
    @Order(25)
    void bigDecimalMathScale2() {
        BigDecimal val = new BigDecimal("3.14159265");
        BigDecimal scaled = BigDecimalMath.scale2(val);
        assertEquals(new BigDecimal("3.14"), scaled);
    }

    // ========== BigDecimalMath + Real Data ==========

    @Test
    @Order(26)
    void bigDecimalMathWithRealSpyReturns() {
        // Calculate daily returns using BigDecimalMath on real data
        for (int i = 1; i < Math.min(50, spyData.size()); i++) {
            BigDecimal prevClose = spyData.get(i - 1).close();
            BigDecimal currClose = spyData.get(i).close();

            BigDecimal dailyReturn = BigDecimalMath.percentChange(prevClose, currClose);
            assertNotNull(dailyReturn);

            // Daily SPY returns should be within reasonable bounds (-10% to +10%)
            assertTrue(dailyReturn.compareTo(new BigDecimal("-10")) > 0,
                    "Daily return should be > -10% on " + spyData.get(i).date());
            assertTrue(dailyReturn.compareTo(new BigDecimal("10")) < 0,
                    "Daily return should be < 10% on " + spyData.get(i).date());
        }
    }

    // ========== MovingAverageCalculator + Real Data ==========

    @Test
    @Order(27)
    void smaComputationOnRealData() {
        int period = 20;
        for (int i = period; i < spyData.size(); i++) {
            BigDecimal sma = MovingAverageCalculator.sma(spyData, i, period);

            // SMA should be between the min and max close in the window
            BigDecimal min = BigDecimal.valueOf(Double.MAX_VALUE);
            BigDecimal max = BigDecimal.ZERO;
            for (int j = i - period + 1; j <= i; j++) {
                BigDecimal close = spyData.get(j).close();
                if (close.compareTo(min) < 0) min = close;
                if (close.compareTo(max) > 0) max = close;
            }

            assertTrue(sma.compareTo(min) >= 0,
                    "SMA should be >= min close in window at index " + i);
            assertTrue(sma.compareTo(max) <= 0,
                    "SMA should be <= max close in window at index " + i);
        }
    }

    @Test
    @Order(28)
    void emaComputationSmoothness() {
        int period = 12;
        BigDecimal prevEma = null;
        int directionChanges = 0;
        boolean prevUp = false;

        for (int i = period; i < spyData.size(); i++) {
            BigDecimal ema = MovingAverageCalculator.ema(spyData, i, period);
            assertTrue(ema.compareTo(BigDecimal.ZERO) > 0,
                    "EMA should always be positive");

            if (prevEma != null) {
                boolean isUp = ema.compareTo(prevEma) > 0;
                if (i > period + 1 && isUp != prevUp) {
                    directionChanges++;
                }
                prevUp = isUp;
            }
            prevEma = ema;
        }

        // EMA should be smoother than raw price data
        int priceDirectionChanges = 0;
        for (int i = 1; i < spyData.size(); i++) {
            boolean priceUp = spyData.get(i).close().compareTo(spyData.get(i - 1).close()) > 0;
            boolean prevPriceUp = i > 1 &&
                    spyData.get(i - 1).close().compareTo(spyData.get(i - 2).close()) > 0;
            if (i > 1 && priceUp != prevPriceUp) {
                priceDirectionChanges++;
            }
        }

        assertTrue(directionChanges < priceDirectionChanges,
                "EMA should have fewer direction changes than raw price");
    }

    @Test
    @Order(29)
    void smaAndEmaConvergeOnFlatData() {
        // For flat data, SMA and EMA should be the same
        var flatData = new java.util.ArrayList<PriceData>();
        BigDecimal flatPrice = new BigDecimal("100.00");
        for (int i = 0; i < 50; i++) {
            flatData.add(new PriceData("2024-01-" + String.format("%02d", (i % 28) + 1),
                    flatPrice, flatPrice, flatPrice, flatPrice, flatPrice, 1000));
        }

        BigDecimal sma = MovingAverageCalculator.sma(flatData, 49, 20);
        BigDecimal ema = MovingAverageCalculator.ema(flatData, 49, 20);

        assertEquals(0, flatPrice.compareTo(sma.setScale(2, RoundingMode.HALF_UP)),
                "SMA should equal the flat price");
        assertEquals(0, flatPrice.compareTo(ema.setScale(2, RoundingMode.HALF_UP)),
                "EMA should equal the flat price for flat data");
    }

    // ========== Full Utility Chain Integration ==========

    @Test
    @Order(30)
    void fullUtilityChainFromCsvToAnalysis() {
        // This test chains: CsvParser -> PriceData -> MovingAverageCalculator -> BigDecimalMath

        // Step 1: Parse CSV data
        String csv = "Date,Open,High,Low,Close,Adj Close,Volume\n";
        for (int i = 0; i < 60; i++) {
            double price = 100 + Math.sin(i * 0.1) * 10;
            csv += "2024-01-%02d,%f,%f,%f,%f,%f,%d\n".formatted(
                    (i % 28) + 1, price, price + 1, price - 1, price, price, 1000000L);
        }

        try (var parser = new CsvParser(new StringReader(csv))) {
            List<String[]> rows = parser.readAll();
            assertEquals(61, rows.size()); // header + 60 data rows
        } catch (Exception e) {
            fail("CSV parsing should not fail: " + e.getMessage());
        }

        // Step 2: Load via CsvPriceDataLoader
        var stream = new java.io.ByteArrayInputStream(csv.getBytes());
        List<PriceData> data = CsvPriceDataLoader.loadFromStream(stream);
        assertEquals(60, data.size());

        // Step 3: Compute moving averages
        BigDecimal sma20 = MovingAverageCalculator.sma(data, 30, 20);
        BigDecimal ema12 = MovingAverageCalculator.ema(data, 30, 12);
        assertNotNull(sma20);
        assertNotNull(ema12);

        // Step 4: Use BigDecimalMath to compute percent change
        BigDecimal pctChange = BigDecimalMath.percentChange(
                data.getFirst().close(), data.getLast().close());
        assertNotNull(pctChange);

        // Step 5: Store and retrieve results via FileHelper
        String resultFile = tempDir.resolve("analysis-result.txt").toString();
        FileHelper.appendLine("SMA20: " + sma20, resultFile);
        FileHelper.appendLine("EMA12: " + ema12, resultFile);
        FileHelper.appendLine("PctChange: " + pctChange, resultFile);

        TreeSet<String> results = FileHelper.readLines(resultFile);
        assertEquals(3, results.size());

        FileHelper.deleteFile(resultFile);
    }

    @Test
    @Order(31)
    void retryExecutorWithFileHelper() {
        // Simulate retry pattern used by StockListDownloader
        String filepath = tempDir.resolve("retry-test.txt").toString();
        AtomicInteger attempts = new AtomicInteger(0);

        RetryExecutor.execute(() -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 2) {
                throw new RuntimeException("Simulated download failure");
            }
            // On success, write data
            FileHelper.writeLines(Set.of("AAPL", "MSFT"), filepath);
        }, 3, LOGGER, "file write with retry");

        TreeSet<String> lines = FileHelper.readLines(filepath);
        assertEquals(2, lines.size());
        assertTrue(lines.contains("AAPL"));
        assertTrue(lines.contains("MSFT"));

        FileHelper.deleteFile(filepath);
    }

    @Test
    @Order(32)
    void dateHelperFormatsUsedInDataPipeline() {
        // Verify DateHelper formats are compatible with PriceData date format
        var helper = new DateHelper(LocalDate.of(2024, 3, 15));
        String yahooDate = helper.getTodayMarket().format(DateHelper.YAHOO_FORMAT);

        // Should be in yyyy-MM-dd format, matching CSV data
        assertTrue(yahooDate.matches("\\d{4}-\\d{2}-\\d{2}"),
                "Yahoo format should be yyyy-MM-dd");

        // Standard format for display
        String stdDate = helper.getToday();
        assertTrue(stdDate.matches("\\d{2}/\\d{2}/\\d{4}"),
                "Standard format should be MM/dd/yyyy");

        // Morningstar format for quarterly data
        String msDate = helper.getTodayMarket().format(DateHelper.MORNINGSTAR_FORMAT);
        assertTrue(msDate.matches("\\d{4}-\\d{2}"),
                "Morningstar format should be yyyy-MM");

        // Yahoo earnings format
        String earningsDate = helper.getTodayMarket().format(DateHelper.YAHOO_EARNINGS_FORMAT);
        assertTrue(earningsDate.matches("\\d{8}"),
                "Yahoo earnings format should be yyyyMMdd");
    }
}
