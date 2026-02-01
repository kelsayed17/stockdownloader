package com.stockdownloader.backtest;

import com.stockdownloader.model.PriceData;
import com.stockdownloader.model.Trade;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BacktestResultTest {

    @Test
    void totalReturnCalculation() {
        var result = new BacktestResult("Test", new BigDecimal("10000"));
        result.setFinalCapital(new BigDecimal("12000"));

        assertEquals(0, new BigDecimal("20").compareTo(
                result.getTotalReturn().setScale(0, RoundingMode.HALF_UP)));
    }

    @Test
    void totalProfitLoss() {
        var result = new BacktestResult("Test", new BigDecimal("10000"));
        result.setFinalCapital(new BigDecimal("12000"));

        assertEquals(0, new BigDecimal("2000").compareTo(result.getTotalProfitLoss()));
    }

    @Test
    void zeroTradesReturnZeroWinRate() {
        var result = new BacktestResult("Test", new BigDecimal("10000"));
        assertEquals(BigDecimal.ZERO, result.getWinRate());
    }

    @Test
    void winRateCalculation() {
        var result = new BacktestResult("Test", new BigDecimal("10000"));

        var winTrade = new Trade(Trade.Direction.LONG, "d1", new BigDecimal("100"), 10);
        winTrade.close("d2", new BigDecimal("120"));

        var lossTrade = new Trade(Trade.Direction.LONG, "d3", new BigDecimal("100"), 10);
        lossTrade.close("d4", new BigDecimal("80"));

        result.addTrade(winTrade);
        result.addTrade(lossTrade);

        assertEquals(0, new BigDecimal("50").compareTo(
                result.getWinRate().setScale(0, RoundingMode.HALF_UP)));
    }

    @Test
    void winningAndLosingTradeCount() {
        var result = new BacktestResult("Test", new BigDecimal("10000"));

        var win1 = new Trade(Trade.Direction.LONG, "d1", new BigDecimal("100"), 10);
        win1.close("d2", new BigDecimal("120"));
        var win2 = new Trade(Trade.Direction.LONG, "d3", new BigDecimal("100"), 10);
        win2.close("d4", new BigDecimal("115"));
        var loss = new Trade(Trade.Direction.LONG, "d5", new BigDecimal("100"), 10);
        loss.close("d6", new BigDecimal("90"));

        result.addTrade(win1);
        result.addTrade(win2);
        result.addTrade(loss);

        assertEquals(3, result.getTotalTrades());
        assertEquals(2, result.getWinningTrades());
        assertEquals(1, result.getLosingTrades());
    }

    @Test
    void averageWinAndLoss() {
        var result = new BacktestResult("Test", new BigDecimal("10000"));

        var win = new Trade(Trade.Direction.LONG, "d1", new BigDecimal("100"), 10);
        win.close("d2", new BigDecimal("120")); // P/L = 200
        var loss = new Trade(Trade.Direction.LONG, "d3", new BigDecimal("100"), 10);
        loss.close("d4", new BigDecimal("80")); // P/L = -200

        result.addTrade(win);
        result.addTrade(loss);

        assertEquals(0, new BigDecimal("200").compareTo(result.getAverageWin()));
        assertEquals(0, new BigDecimal("-200").compareTo(result.getAverageLoss()));
    }

    @Test
    void profitFactorWithNoLosses() {
        var result = new BacktestResult("Test", new BigDecimal("10000"));

        var win = new Trade(Trade.Direction.LONG, "d1", new BigDecimal("100"), 10);
        win.close("d2", new BigDecimal("120"));
        result.addTrade(win);

        assertEquals(0, new BigDecimal("999.99").compareTo(result.getProfitFactor()));
    }

    @Test
    void profitFactorCalculation() {
        var result = new BacktestResult("Test", new BigDecimal("10000"));

        var win = new Trade(Trade.Direction.LONG, "d1", new BigDecimal("100"), 10);
        win.close("d2", new BigDecimal("150")); // P/L = 500
        var loss = new Trade(Trade.Direction.LONG, "d3", new BigDecimal("100"), 10);
        loss.close("d4", new BigDecimal("50")); // P/L = -500

        result.addTrade(win);
        result.addTrade(loss);

        assertEquals(0, BigDecimal.ONE.compareTo(result.getProfitFactor()));
    }

    @Test
    void maxDrawdownOnEmptyCurve() {
        var result = new BacktestResult("Test", new BigDecimal("10000"));
        assertEquals(BigDecimal.ZERO, result.getMaxDrawdown());
    }

    @Test
    void maxDrawdownCalculation() {
        var result = new BacktestResult("Test", new BigDecimal("10000"));
        result.setEquityCurve(List.of(
                new BigDecimal("10000"),
                new BigDecimal("11000"),
                new BigDecimal("9000"),  // 18.18% drawdown from 11000
                new BigDecimal("10500")
        ));

        BigDecimal maxDD = result.getMaxDrawdown();
        assertTrue(maxDD.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(maxDD.compareTo(new BigDecimal("20")) < 0);
    }

    @Test
    void sharpeRatioWithInsufficientData() {
        var result = new BacktestResult("Test", new BigDecimal("10000"));
        assertEquals(BigDecimal.ZERO, result.getSharpeRatio(252));
    }

    @Test
    void sharpeRatioCalculation() {
        var result = new BacktestResult("Test", new BigDecimal("10000"));
        result.setEquityCurve(List.of(
                new BigDecimal("10000"),
                new BigDecimal("10100"),
                new BigDecimal("10200"),
                new BigDecimal("10300"),
                new BigDecimal("10400")
        ));

        BigDecimal sharpe = result.getSharpeRatio(252);
        assertTrue(sharpe.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void buyAndHoldReturn() {
        var result = new BacktestResult("Test", new BigDecimal("10000"));

        BigDecimal p = new BigDecimal("100");
        List<PriceData> data = List.of(
                new PriceData("d1", p, p, p, new BigDecimal("100"), p, 1000),
                new PriceData("d2", p, p, p, new BigDecimal("150"), p, 1000)
        );

        assertEquals(0, new BigDecimal("50").compareTo(
                result.getBuyAndHoldReturn(data).setScale(0, RoundingMode.HALF_UP)));
    }

    @Test
    void buyAndHoldReturnEmptyData() {
        var result = new BacktestResult("Test", new BigDecimal("10000"));
        assertEquals(BigDecimal.ZERO, result.getBuyAndHoldReturn(List.of()));
    }
}
