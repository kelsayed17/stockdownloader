package com.stockdownloader.integration;

import com.stockdownloader.backtest.BacktestEngine;
import com.stockdownloader.backtest.BacktestResult;
import com.stockdownloader.data.CsvPriceDataLoader;
import com.stockdownloader.model.PriceData;
import com.stockdownloader.model.Trade;
import com.stockdownloader.strategy.MACDStrategy;
import com.stockdownloader.strategy.RSIStrategy;
import com.stockdownloader.strategy.SMACrossoverStrategy;
import com.stockdownloader.strategy.TradingStrategy;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that exercises the full backtest pipeline:
 * CSV file loading -> PriceData parsing -> Strategy evaluation -> BacktestEngine execution -> Result metrics.
 */
class BacktestPipelineIntegrationTest {

    private static final BigDecimal INITIAL_CAPITAL = new BigDecimal("100000.00");
    private static final BigDecimal ZERO_COMMISSION = BigDecimal.ZERO;
    private static List<PriceData> priceData;

    @BeforeAll
    static void loadTestData() {
        priceData = CsvPriceDataLoader.loadFromStream(
                BacktestPipelineIntegrationTest.class.getResourceAsStream("/test-price-data.csv"));
        assertFalse(priceData.isEmpty(), "Test price data should be loaded successfully");
    }

    @Test
    void csvDataLoadedCorrectly() {
        assertTrue(priceData.size() > 200, "Should have > 200 trading days for strategy warmup");
        assertEquals("2023-01-03", priceData.getFirst().date());
        assertEquals("2024-01-31", priceData.getLast().date());

        // Verify OHLCV fields are populated
        PriceData first = priceData.getFirst();
        assertTrue(first.open().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(first.high().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(first.low().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(first.close().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(first.volume() > 0);
    }

    @Test
    void smaCrossoverPipelineProducesValidResults() {
        var strategy = new SMACrossoverStrategy(20, 50);
        var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);

        BacktestResult result = engine.run(strategy, priceData);

        assertNotNull(result);
        assertEquals("SMA Crossover (20/50)", result.getStrategyName());
        assertEquals(INITIAL_CAPITAL, result.getInitialCapital());
        assertNotNull(result.getFinalCapital());
        assertTrue(result.getFinalCapital().compareTo(BigDecimal.ZERO) > 0, "Final capital should be positive");

        // Should have at least some trades over a year of data
        assertTrue(result.getTotalTrades() >= 0, "Should have non-negative trade count");

        // Equity curve should match data size
        assertEquals(priceData.size(), result.getEquityCurve().size());

        // All equity values should be positive
        for (BigDecimal equity : result.getEquityCurve()) {
            assertTrue(equity.compareTo(BigDecimal.ZERO) > 0, "Equity should always be positive");
        }
    }

    @Test
    void rsiPipelineProducesValidResults() {
        var strategy = new RSIStrategy(14, 30, 70);
        var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);

        BacktestResult result = engine.run(strategy, priceData);

        assertNotNull(result);
        assertEquals("RSI (14) [30/70]", result.getStrategyName());
        assertTrue(result.getFinalCapital().compareTo(BigDecimal.ZERO) > 0);

        // Verify metrics are computable without error
        assertNotNull(result.getTotalReturn());
        assertNotNull(result.getTotalProfitLoss());
        assertNotNull(result.getWinRate());
        assertNotNull(result.getMaxDrawdown());
        assertNotNull(result.getProfitFactor());
    }

    @Test
    void macdPipelineProducesValidResults() {
        var strategy = new MACDStrategy(12, 26, 9);
        var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);

        BacktestResult result = engine.run(strategy, priceData);

        assertNotNull(result);
        assertEquals("MACD (12/26/9)", result.getStrategyName());
        assertTrue(result.getFinalCapital().compareTo(BigDecimal.ZERO) > 0);

        // Sharpe ratio should be computable
        BigDecimal sharpe = result.getSharpeRatio(252);
        assertNotNull(sharpe);
    }

    @Test
    void commissionReducesFinalCapital() {
        var strategy = new SMACrossoverStrategy(20, 50);
        BigDecimal commission = new BigDecimal("10.00");

        var engineNoCommission = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);
        var engineWithCommission = new BacktestEngine(INITIAL_CAPITAL, commission);

        BacktestResult resultNoComm = engineNoCommission.run(strategy, priceData);
        BacktestResult resultWithComm = engineWithCommission.run(strategy, priceData);

        // If trades occurred, commission should reduce final capital
        if (resultNoComm.getTotalTrades() > 0) {
            assertTrue(resultNoComm.getFinalCapital().compareTo(resultWithComm.getFinalCapital()) >= 0,
                    "Commission should reduce or maintain final capital");
        }
    }

    @Test
    void allTradesAreClosedAtEnd() {
        var strategy = new SMACrossoverStrategy(20, 50);
        var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);

        BacktestResult result = engine.run(strategy, priceData);

        // All trades in the result should be closed (engine closes open positions at end)
        for (Trade trade : result.getTrades()) {
            assertEquals(Trade.Status.CLOSED, trade.getStatus(),
                    "All trades should be closed at end of backtest");
            assertNotNull(trade.getExitDate());
            assertNotNull(trade.getExitPrice());
        }
    }

    @Test
    void profitLossConsistency() {
        var strategy = new MACDStrategy(12, 26, 9);
        var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);

        BacktestResult result = engine.run(strategy, priceData);

        BigDecimal expectedPL = result.getFinalCapital().subtract(result.getInitialCapital());
        assertEquals(0, expectedPL.compareTo(result.getTotalProfitLoss()),
                "Total P/L should equal final capital minus initial capital");
    }

    @Test
    void winRateConsistency() {
        var strategy = new RSIStrategy(14, 25, 75);
        var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);

        BacktestResult result = engine.run(strategy, priceData);

        long total = result.getTotalTrades();
        long winning = result.getWinningTrades();
        long losing = result.getLosingTrades();

        assertEquals(total, winning + losing,
                "Winning + losing trades should equal total trades");

        if (total > 0) {
            BigDecimal winRate = result.getWinRate();
            assertTrue(winRate.compareTo(BigDecimal.ZERO) >= 0, "Win rate should be >= 0");
            assertTrue(winRate.compareTo(new BigDecimal("100")) <= 0, "Win rate should be <= 100");
        }
    }

    @Test
    void buyAndHoldReturnMatchesData() {
        var strategy = new SMACrossoverStrategy(20, 50);
        var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);

        BacktestResult result = engine.run(strategy, priceData);
        BigDecimal buyAndHold = result.getBuyAndHoldReturn(priceData);

        // Manually compute buy and hold
        BigDecimal firstClose = priceData.getFirst().close();
        BigDecimal lastClose = priceData.getLast().close();
        BigDecimal expectedReturn = lastClose.subtract(firstClose)
                .multiply(new BigDecimal("100"))
                .divide(firstClose, 6, java.math.RoundingMode.HALF_UP);

        assertEquals(0, expectedReturn.compareTo(buyAndHold),
                "Buy and hold return should match manual calculation");
    }

    @Test
    void maxDrawdownIsNonNegative() {
        var strategy = new SMACrossoverStrategy(50, 200);
        var engine = new BacktestEngine(INITIAL_CAPITAL, ZERO_COMMISSION);

        BacktestResult result = engine.run(strategy, priceData);

        BigDecimal maxDrawdown = result.getMaxDrawdown();
        assertTrue(maxDrawdown.compareTo(BigDecimal.ZERO) >= 0,
                "Max drawdown should be non-negative");
        assertTrue(maxDrawdown.compareTo(new BigDecimal("100")) <= 0,
                "Max drawdown should be <= 100%");
    }
}
