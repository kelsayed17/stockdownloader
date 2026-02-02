package com.stockdownloader.e2e;

import com.stockdownloader.backtest.BacktestEngine;
import com.stockdownloader.backtest.BacktestResult;
import com.stockdownloader.data.CsvPriceDataLoader;
import com.stockdownloader.model.PriceData;
import com.stockdownloader.model.Trade;
import com.stockdownloader.strategy.MACDStrategy;
import com.stockdownloader.strategy.RSIStrategy;
import com.stockdownloader.strategy.SMACrossoverStrategy;
import com.stockdownloader.strategy.TradingStrategy;
import com.stockdownloader.util.CsvParser;
import com.stockdownloader.util.MovingAverageCalculator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end test for the complete data loading pipeline and trade lifecycle.
 *
 * Pipeline 1 (Data Loading):
 *   Raw CSV string -> CsvParser -> CsvPriceDataLoader -> List<PriceData>
 *   -> MovingAverageCalculator (SMA/EMA) -> strategy signals
 *
 * Pipeline 2 (Trade Lifecycle):
 *   Strategy signal -> Trade creation (OPEN) -> position tracking
 *   -> Trade close (CLOSED) -> P/L calculation -> BacktestResult aggregation
 *   -> metric computation (return, win rate, Sharpe, drawdown, profit factor)
 *
 * Exercises: CsvParser, CsvPriceDataLoader, PriceData, Trade, BacktestEngine,
 * BacktestResult, all strategy implementations, MovingAverageCalculator.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DataPipelineAndTradeLifecycleE2ETest {

    private static List<PriceData> realData;

    @BeforeAll
    static void loadRealData() {
        realData = CsvPriceDataLoader.loadFromStream(
                DataPipelineAndTradeLifecycleE2ETest.class.getResourceAsStream("/test-price-data.csv"));
        assertFalse(realData.isEmpty());
    }

    // ========== CsvParser -> CsvPriceDataLoader Pipeline ==========

    @Test
    @Order(1)
    void csvParserToLoaderPipeline() throws Exception {
        // Manually construct CSV and verify it flows through the full parsing pipeline
        String csv = """
                Date,Open,High,Low,Close,Adj Close,Volume
                2024-01-02,100.00,105.00,99.00,103.50,103.50,5000000
                2024-01-03,103.50,108.00,102.00,107.25,107.25,6000000
                2024-01-04,107.25,110.00,106.00,108.75,108.75,4500000
                """;

        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        List<PriceData> data = CsvPriceDataLoader.loadFromStream(stream);

        assertEquals(3, data.size());
        assertEquals("2024-01-02", data.get(0).date());
        assertEquals(new BigDecimal("100.00"), data.get(0).open());
        assertEquals(new BigDecimal("105.00"), data.get(0).high());
        assertEquals(new BigDecimal("99.00"), data.get(0).low());
        assertEquals(new BigDecimal("103.50"), data.get(0).close());
        assertEquals(new BigDecimal("103.50"), data.get(0).adjClose());
        assertEquals(5000000L, data.get(0).volume());
    }

    @Test
    @Order(2)
    void csvParserHandlesRealDataFormat() throws Exception {
        // Verify CsvParser correctly parses actual Yahoo Finance CSV format
        InputStream stream = getClass().getResourceAsStream("/test-price-data.csv");
        assertNotNull(stream);

        try (var parser = new CsvParser(stream)) {
            String[] header = parser.readNext();
            assertNotNull(header);
            assertEquals("Date", header[0]);
            assertEquals("Open", header[1]);
            assertEquals("High", header[2]);
            assertEquals("Low", header[3]);
            assertEquals("Close", header[4]);
            assertEquals("Adj Close", header[5]);
            assertEquals("Volume", header[6]);

            // Read first data row
            String[] firstRow = parser.readNext();
            assertNotNull(firstRow);
            assertEquals("2023-01-03", firstRow[0]);
            // Verify values are parseable as numbers
            assertDoesNotThrow(() -> new BigDecimal(firstRow[1]));
            assertDoesNotThrow(() -> new BigDecimal(firstRow[4]));
            assertDoesNotThrow(() -> Long.parseLong(firstRow[6]));
        }
    }

    @Test
    @Order(3)
    void csvParserWithTabSeparator() throws Exception {
        String tsv = "Col1\tCol2\tCol3\nA\tB\tC\nD\tE\tF";
        try (var parser = new CsvParser(new StringReader(tsv), '\t')) {
            String[] header = parser.readNext();
            assertEquals(3, header.length);
            assertEquals("Col1", header[0]);
            assertEquals("Col3", header[2]);

            String[] row1 = parser.readNext();
            assertEquals("A", row1[0]);
            assertEquals("C", row1[2]);
        }
    }

    @Test
    @Order(4)
    void csvParserHandlesQuotedFields() throws Exception {
        String csv = "Name,Value\n\"Smith, John\",100\n\"O'Brien\",200";
        try (var parser = new CsvParser(new StringReader(csv))) {
            parser.readNext(); // skip header
            String[] row1 = parser.readNext();
            assertEquals("Smith, John", row1[0]);
            assertEquals("100", row1[1]);
        }
    }

    @Test
    @Order(5)
    void csvParserReadAllReturnsAllRows() throws Exception {
        String csv = "A,B\n1,2\n3,4\n5,6";
        try (var parser = new CsvParser(new StringReader(csv))) {
            List<String[]> rows = parser.readAll();
            assertEquals(4, rows.size()); // header + 3 data rows
        }
    }

    @Test
    @Order(6)
    void csvParserSkipLines() throws Exception {
        String csv = "Header1\nHeader2\nData1,A\nData2,B";
        try (var parser = new CsvParser(new StringReader(csv))) {
            parser.skipLines(2);
            String[] dataRow = parser.readNext();
            assertEquals("Data1", dataRow[0]);
        }
    }

    @Test
    @Order(7)
    void loaderSkipsInvalidNumericRows() {
        String csv = "Date,Open,High,Low,Close,Adj Close,Volume\n"
                + "2024-01-02,100.00,105.00,99.00,103.50,103.50,5000000\n"
                + "2024-01-03,null,null,null,null,null,0\n"
                + "2024-01-04,107.25,110.00,106.00,108.75,108.75,4500000\n";

        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        List<PriceData> data = CsvPriceDataLoader.loadFromStream(stream);

        assertEquals(2, data.size(), "Should skip the row with null values");
        assertEquals("2024-01-02", data.get(0).date());
        assertEquals("2024-01-04", data.get(1).date());
    }

    @Test
    @Order(8)
    void loaderHandlesMissingAdjCloseAndVolume() {
        String csv = "Date,Open,High,Low,Close\n"
                + "2024-01-02,100.00,105.00,99.00,103.50\n";

        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        List<PriceData> data = CsvPriceDataLoader.loadFromStream(stream);

        assertEquals(1, data.size());
        // adjClose should default to close, volume should default to 0
        assertEquals(data.get(0).close(), data.get(0).adjClose());
        assertEquals(0L, data.get(0).volume());
    }

    // ========== Data -> Strategy Signal Pipeline ==========

    @Test
    @Order(9)
    void dataToSignalPipelineSMA() {
        var strategy = new SMACrossoverStrategy(20, 50);

        // Before warmup, all signals should be HOLD
        for (int i = 0; i < strategy.getWarmupPeriod(); i++) {
            assertEquals(TradingStrategy.Signal.HOLD, strategy.evaluate(realData, i),
                    "Before warmup, signal should be HOLD at index " + i);
        }

        // After warmup, signals depend on SMA crossover
        int buyCount = 0, sellCount = 0, holdCount = 0;
        for (int i = strategy.getWarmupPeriod(); i < realData.size(); i++) {
            TradingStrategy.Signal signal = strategy.evaluate(realData, i);
            switch (signal) {
                case BUY -> buyCount++;
                case SELL -> sellCount++;
                case HOLD -> holdCount++;
            }
        }

        // Should have at least some signals with real data
        assertTrue(buyCount + sellCount + holdCount > 0, "Should evaluate signals after warmup");
        assertTrue(holdCount > buyCount, "HOLD should be more common than BUY");
        assertTrue(holdCount > sellCount, "HOLD should be more common than SELL");
    }

    @Test
    @Order(10)
    void dataToSignalPipelineRSI() {
        var strategy = new RSIStrategy(14, 30, 70);

        // Warmup period check
        assertEquals(15, strategy.getWarmupPeriod()); // period + 1

        int buyCount = 0, sellCount = 0;
        for (int i = strategy.getWarmupPeriod(); i < realData.size(); i++) {
            TradingStrategy.Signal signal = strategy.evaluate(realData, i);
            if (signal == TradingStrategy.Signal.BUY) buyCount++;
            if (signal == TradingStrategy.Signal.SELL) sellCount++;
        }

        // RSI signals should be relatively rare (extreme readings)
        int totalBars = realData.size() - strategy.getWarmupPeriod();
        assertTrue(buyCount < totalBars / 2, "RSI BUY signals should be infrequent");
        assertTrue(sellCount < totalBars / 2, "RSI SELL signals should be infrequent");
    }

    @Test
    @Order(11)
    void dataToSignalPipelineMACD() {
        var strategy = new MACDStrategy(12, 26, 9);

        assertEquals(35, strategy.getWarmupPeriod()); // slowPeriod + signalPeriod

        int buyCount = 0, sellCount = 0;
        for (int i = strategy.getWarmupPeriod(); i < realData.size(); i++) {
            TradingStrategy.Signal signal = strategy.evaluate(realData, i);
            if (signal == TradingStrategy.Signal.BUY) buyCount++;
            if (signal == TradingStrategy.Signal.SELL) sellCount++;
        }

        // MACD should generate both buy and sell signals with real data
        assertTrue(buyCount > 0 || sellCount > 0,
                "MACD should generate at least one signal with real SPY data");
    }

    // ========== Trade Lifecycle E2E ==========

    @Test
    @Order(12)
    void tradeCreationAndClosure() {
        Trade trade = new Trade(Trade.Direction.LONG, "2024-01-02",
                new BigDecimal("100.00"), 50);

        assertEquals(Trade.Status.OPEN, trade.getStatus());
        assertEquals(Trade.Direction.LONG, trade.getDirection());
        assertEquals(50, trade.getShares());
        assertNull(trade.getExitDate());
        assertNull(trade.getExitPrice());

        // Close with profit
        trade.close("2024-01-10", new BigDecimal("110.00"));

        assertEquals(Trade.Status.CLOSED, trade.getStatus());
        assertEquals("2024-01-10", trade.getExitDate());
        assertEquals(new BigDecimal("110.00"), trade.getExitPrice());

        // P/L = (110 - 100) * 50 = 500
        assertEquals(0, new BigDecimal("500.00").compareTo(trade.getProfitLoss()));
        assertTrue(trade.isWin());

        // Return % = (110 - 100) / 100 * 100 = 10%
        assertEquals(0, new BigDecimal("10.000000").compareTo(trade.getReturnPct()));
    }

    @Test
    @Order(13)
    void tradeLosingPosition() {
        Trade trade = new Trade(Trade.Direction.LONG, "2024-01-02",
                new BigDecimal("100.00"), 100);

        trade.close("2024-01-10", new BigDecimal("95.00"));

        // P/L = (95 - 100) * 100 = -500
        assertTrue(trade.getProfitLoss().compareTo(BigDecimal.ZERO) < 0);
        assertFalse(trade.isWin());
    }

    @Test
    @Order(14)
    void tradeCannotBeClosedTwice() {
        Trade trade = new Trade(Trade.Direction.LONG, "2024-01-02",
                new BigDecimal("100.00"), 10);
        trade.close("2024-01-10", new BigDecimal("110.00"));

        assertThrows(IllegalStateException.class,
                () -> trade.close("2024-01-15", new BigDecimal("120.00")),
                "Should not allow closing an already closed trade");
    }

    @Test
    @Order(15)
    void tradeShortDirection() {
        Trade trade = new Trade(Trade.Direction.SHORT, "2024-01-02",
                new BigDecimal("100.00"), 50);

        trade.close("2024-01-10", new BigDecimal("90.00"));

        // Short P/L = (entry - exit) * shares = (100 - 90) * 50 = 500
        assertEquals(0, new BigDecimal("500.00").compareTo(trade.getProfitLoss()));
        assertTrue(trade.isWin());
    }

    @Test
    @Order(16)
    void tradeToStringFormat() {
        Trade trade = new Trade(Trade.Direction.LONG, "2024-01-02",
                new BigDecimal("100.00"), 10);
        trade.close("2024-01-10", new BigDecimal("110.00"));

        String str = trade.toString();
        assertTrue(str.contains("LONG"));
        assertTrue(str.contains("CLOSED"));
        assertTrue(str.contains("2024-01-02"));
        assertTrue(str.contains("2024-01-10"));
        assertTrue(str.contains("100.00"));
        assertTrue(str.contains("110.00"));
    }

    // ========== Engine -> Trade -> Result Full Lifecycle ==========

    @Test
    @Order(17)
    void engineProducesClosedTradesWithValidLifecycle() {
        var engine = new BacktestEngine(new BigDecimal("100000"), BigDecimal.ZERO);
        var strategy = new SMACrossoverStrategy(20, 50);

        BacktestResult result = engine.run(strategy, realData);

        for (Trade trade : result.getClosedTrades()) {
            // Every trade should go through complete lifecycle
            assertEquals(Trade.Status.CLOSED, trade.getStatus());
            assertEquals(Trade.Direction.LONG, trade.getDirection());
            assertNotNull(trade.getEntryDate());
            assertNotNull(trade.getExitDate());
            assertTrue(trade.getEntryPrice().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(trade.getExitPrice().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(trade.getShares() > 0);

            // Entry date should come before exit date
            assertTrue(trade.getEntryDate().compareTo(trade.getExitDate()) < 0,
                    "Entry date should precede exit date");

            // Verify P/L calculation
            BigDecimal expectedPL = trade.getExitPrice().subtract(trade.getEntryPrice())
                    .multiply(BigDecimal.valueOf(trade.getShares()));
            assertEquals(0, expectedPL.compareTo(trade.getProfitLoss()),
                    "P/L should match (exit - entry) * shares");

            // Verify return percentage
            BigDecimal expectedReturn = trade.getExitPrice().subtract(trade.getEntryPrice())
                    .divide(trade.getEntryPrice(), 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            assertEquals(0, expectedReturn.compareTo(trade.getReturnPct()),
                    "Return % should match price change percentage");
        }
    }

    @Test
    @Order(18)
    void engineTradeEntryDatesMatchDataDates() {
        var engine = new BacktestEngine(new BigDecimal("100000"), BigDecimal.ZERO);
        var strategy = new MACDStrategy(12, 26, 9);

        BacktestResult result = engine.run(strategy, realData);

        // Collect all valid dates from the data
        var validDates = realData.stream().map(PriceData::date).toList();

        for (Trade trade : result.getClosedTrades()) {
            assertTrue(validDates.contains(trade.getEntryDate()),
                    "Entry date " + trade.getEntryDate() + " should be a valid trading date");
            assertTrue(validDates.contains(trade.getExitDate()),
                    "Exit date " + trade.getExitDate() + " should be a valid trading date");
        }
    }

    @Test
    @Order(19)
    void engineTradeEntryPricesMatchClosePrices() {
        var engine = new BacktestEngine(new BigDecimal("100000"), BigDecimal.ZERO);
        var strategy = new SMACrossoverStrategy(20, 50);

        BacktestResult result = engine.run(strategy, realData);

        for (Trade trade : result.getClosedTrades()) {
            // Find the entry bar in real data
            for (PriceData bar : realData) {
                if (bar.date().equals(trade.getEntryDate())) {
                    assertEquals(0, bar.close().compareTo(trade.getEntryPrice()),
                            "Entry price should match the closing price of the entry date");
                    break;
                }
            }
            // Find the exit bar
            for (PriceData bar : realData) {
                if (bar.date().equals(trade.getExitDate())) {
                    assertEquals(0, bar.close().compareTo(trade.getExitPrice()),
                            "Exit price should match the closing price of the exit date");
                    break;
                }
            }
        }
    }

    @Test
    @Order(20)
    void engineSharesCalculatedFromCapital() {
        BigDecimal capital = new BigDecimal("50000.00");
        var engine = new BacktestEngine(capital, BigDecimal.ZERO);
        var strategy = new SMACrossoverStrategy(20, 50);

        BacktestResult result = engine.run(strategy, realData);

        if (!result.getClosedTrades().isEmpty()) {
            Trade firstTrade = result.getClosedTrades().getFirst();
            // shares = floor(capital / price)
            int expectedShares = capital.divide(firstTrade.getEntryPrice(), 0, RoundingMode.DOWN).intValue();
            assertEquals(expectedShares, firstTrade.getShares(),
                    "Shares should be floor(capital / entry price)");
        }
    }

    // ========== Result Metrics from Trade Lifecycle ==========

    @Test
    @Order(21)
    void resultMetricsReflectTradeOutcomes() {
        var engine = new BacktestEngine(new BigDecimal("100000"), BigDecimal.ZERO);
        var strategy = new MACDStrategy(12, 26, 9);
        BacktestResult result = engine.run(strategy, realData);

        if (result.getTotalTrades() > 0) {
            // Average win should be positive
            if (result.getWinningTrades() > 0) {
                assertTrue(result.getAverageWin().compareTo(BigDecimal.ZERO) > 0,
                        "Average win should be positive");
            }

            // Average loss should be negative or zero
            if (result.getLosingTrades() > 0) {
                assertTrue(result.getAverageLoss().compareTo(BigDecimal.ZERO) <= 0,
                        "Average loss should be negative or zero");
            }

            // Sharpe ratio should be computable
            BigDecimal sharpe = result.getSharpeRatio(252);
            assertNotNull(sharpe);
        }
    }

    @Test
    @Order(22)
    void resultSumOfTradePLMatchesTotalPL() {
        var engine = new BacktestEngine(new BigDecimal("100000"), BigDecimal.ZERO);
        var strategy = new RSIStrategy(14, 30, 70);
        BacktestResult result = engine.run(strategy, realData);

        // The total P/L from result should be close to sum of individual trade P/L
        // Note: there may be small differences due to fractional cash leftover from share rounding
        BigDecimal tradePLSum = BigDecimal.ZERO;
        for (Trade t : result.getClosedTrades()) {
            tradePLSum = tradePLSum.add(t.getProfitLoss());
        }

        // Total P/L from result
        BigDecimal totalPL = result.getTotalProfitLoss();

        // The difference should be small (just the cash that couldn't buy full shares)
        BigDecimal diff = totalPL.subtract(tradePLSum).abs();
        // Difference should be less than one share price (max rounding error)
        if (!result.getClosedTrades().isEmpty()) {
            BigDecimal maxSharePrice = realData.stream()
                    .map(PriceData::close)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            assertTrue(diff.compareTo(maxSharePrice) < 0,
                    "P/L difference should be within one share price rounding error");
        }
    }

    // ========== Edge Case: Very Small Capital ==========

    @Test
    @Order(23)
    void verySmallCapitalStillWorks() {
        // Capital too small to buy even one share
        BigDecimal tinyCap = new BigDecimal("1.00");
        var engine = new BacktestEngine(tinyCap, BigDecimal.ZERO);
        var strategy = new SMACrossoverStrategy(20, 50);

        BacktestResult result = engine.run(strategy, realData);

        // Should complete without errors
        assertEquals(tinyCap, result.getFinalCapital(),
                "Capital should remain unchanged when unable to buy shares");
        assertEquals(0, result.getTotalTrades(),
                "No trades should occur with insufficient capital");
    }

    // ========== PriceData Record Validation ==========

    @Test
    @Order(24)
    void priceDataRecordEquality() {
        PriceData p1 = new PriceData("2024-01-02",
                new BigDecimal("100"), new BigDecimal("105"),
                new BigDecimal("99"), new BigDecimal("103"),
                new BigDecimal("103"), 5000000);

        PriceData p2 = new PriceData("2024-01-02",
                new BigDecimal("100"), new BigDecimal("105"),
                new BigDecimal("99"), new BigDecimal("103"),
                new BigDecimal("103"), 5000000);

        assertEquals(p1, p2, "PriceData records with same values should be equal");
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    @Order(25)
    void priceDataNullValidation() {
        assertThrows(NullPointerException.class,
                () -> new PriceData(null, BigDecimal.ONE, BigDecimal.ONE,
                        BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new PriceData("2024-01-02", BigDecimal.ONE, BigDecimal.ONE,
                        BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, -1));
    }

    @Test
    @Order(26)
    void priceDataToStringFormat() {
        PriceData pd = realData.getFirst();
        String str = pd.toString();
        assertTrue(str.contains(pd.date()));
        assertTrue(str.contains("O:"));
        assertTrue(str.contains("H:"));
        assertTrue(str.contains("L:"));
        assertTrue(str.contains("C:"));
        assertTrue(str.contains("V:"));
    }
}
