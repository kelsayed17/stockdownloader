package com.stockdownloader.backtest;

import com.stockdownloader.model.OptionType;
import com.stockdownloader.model.OptionsTrade;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OptionsBacktestResultTest {

    @Test
    void initialState() {
        var result = new OptionsBacktestResult("TestStrategy", new BigDecimal("100000"));
        assertEquals("TestStrategy", result.getStrategyName());
        assertEquals(new BigDecimal("100000"), result.getInitialCapital());
        assertEquals(new BigDecimal("100000"), result.getFinalCapital());
        assertEquals(0, result.getTotalTrades());
        assertEquals(0, result.getTotalVolumeTraded());
    }

    @Test
    void totalReturnCalculation() {
        var result = new OptionsBacktestResult("Test", new BigDecimal("100000"));
        result.setFinalCapital(new BigDecimal("110000"));
        // (110000 - 100000) / 100000 * 100 = 10%
        assertEquals(0, new BigDecimal("10.000000").compareTo(result.getTotalReturn()));
    }

    @Test
    void totalProfitLoss() {
        var result = new OptionsBacktestResult("Test", new BigDecimal("100000"));
        result.setFinalCapital(new BigDecimal("95000"));
        assertEquals(0, new BigDecimal("-5000").compareTo(result.getTotalProfitLoss()));
    }

    @Test
    void winRateCalculation() {
        var result = new OptionsBacktestResult("Test", new BigDecimal("100000"));

        // Two winning trades, one losing
        OptionsTrade win1 = makeTrade(new BigDecimal("5.00"), new BigDecimal("8.00"));
        OptionsTrade win2 = makeTrade(new BigDecimal("3.00"), new BigDecimal("5.00"));
        OptionsTrade lose = makeTrade(new BigDecimal("5.00"), new BigDecimal("2.00"));

        result.addTrade(win1);
        result.addTrade(win2);
        result.addTrade(lose);

        assertEquals(3, result.getTotalTrades());
        assertEquals(2, result.getWinningTrades());
        assertEquals(1, result.getLosingTrades());
        // Win rate: 2/3 * 100 = 66.67%
        assertTrue(result.getWinRate().doubleValue() > 66);
        assertTrue(result.getWinRate().doubleValue() < 67);
    }

    @Test
    void profitFactor() {
        var result = new OptionsBacktestResult("Test", new BigDecimal("100000"));

        OptionsTrade win = makeTrade(new BigDecimal("5.00"), new BigDecimal("8.00"));
        OptionsTrade lose = makeTrade(new BigDecimal("5.00"), new BigDecimal("3.00"));
        result.addTrade(win);
        result.addTrade(lose);

        BigDecimal pf = result.getProfitFactor();
        assertTrue(pf.doubleValue() > 0);
    }

    @Test
    void profitFactorNoLosses() {
        var result = new OptionsBacktestResult("Test", new BigDecimal("100000"));
        result.addTrade(makeTrade(new BigDecimal("5.00"), new BigDecimal("8.00")));

        assertEquals(0, new BigDecimal("999.99").compareTo(result.getProfitFactor()));
    }

    @Test
    void maxDrawdown() {
        var result = new OptionsBacktestResult("Test", new BigDecimal("100000"));
        result.setEquityCurve(List.of(
                new BigDecimal("100000"),
                new BigDecimal("105000"),
                new BigDecimal("95000"),  // drawdown from 105000
                new BigDecimal("98000"),
                new BigDecimal("110000")
        ));

        BigDecimal dd = result.getMaxDrawdown();
        // Peak 105000, trough 95000 → (105000-95000)/105000 = 9.52%
        assertTrue(dd.doubleValue() > 9);
        assertTrue(dd.doubleValue() < 10);
    }

    @Test
    void sharpeRatio() {
        var result = new OptionsBacktestResult("Test", new BigDecimal("100000"));
        result.setEquityCurve(List.of(
                new BigDecimal("100000"),
                new BigDecimal("100500"),
                new BigDecimal("101000"),
                new BigDecimal("101500"),
                new BigDecimal("102000")
        ));

        BigDecimal sharpe = result.getSharpeRatio(252);
        // Consistent positive returns should give high Sharpe
        assertTrue(sharpe.doubleValue() > 0, "Sharpe should be positive for consistent gains");
    }

    @Test
    void sharpeRatioInsufficientData() {
        var result = new OptionsBacktestResult("Test", new BigDecimal("100000"));
        result.setEquityCurve(List.of(new BigDecimal("100000")));
        assertEquals(BigDecimal.ZERO, result.getSharpeRatio(252));
    }

    @Test
    void averagePremiumCollected() {
        var result = new OptionsBacktestResult("Test", new BigDecimal("100000"));
        // Entry premium 5.00, 1 contract * 100 = $500
        result.addTrade(makeTrade(new BigDecimal("5.00"), new BigDecimal("3.00")));
        result.addTrade(makeTrade(new BigDecimal("3.00"), new BigDecimal("1.00")));

        BigDecimal avg = result.getAveragePremiumCollected();
        // (500 + 300) / 2 = 400
        assertEquals(0, new BigDecimal("400.00").compareTo(avg));
    }

    @Test
    void volumeTracking() {
        var result = new OptionsBacktestResult("Test", new BigDecimal("100000"));

        OptionsTrade trade1 = new OptionsTrade(OptionType.CALL, OptionsTrade.Direction.BUY,
                new BigDecimal("100"), "2024-02-16", "2024-01-15",
                new BigDecimal("5.00"), 1, 5000);
        trade1.close("2024-02-10", new BigDecimal("7.00"));

        OptionsTrade trade2 = new OptionsTrade(OptionType.CALL, OptionsTrade.Direction.BUY,
                new BigDecimal("100"), "2024-03-15", "2024-02-15",
                new BigDecimal("4.00"), 1, 3000);
        trade2.close("2024-03-10", new BigDecimal("6.00"));

        result.addTrade(trade1);
        result.addTrade(trade2);

        assertEquals(8000, result.getTotalVolumeTraded());
    }

    @Test
    void closedTradesExcludesOpenTrades() {
        var result = new OptionsBacktestResult("Test", new BigDecimal("100000"));

        OptionsTrade closedTrade = makeTrade(new BigDecimal("5.00"), new BigDecimal("8.00"));
        OptionsTrade openTrade = new OptionsTrade(OptionType.CALL, OptionsTrade.Direction.BUY,
                new BigDecimal("100"), "2024-02-16", "2024-01-15",
                new BigDecimal("5.00"), 1, 1000);

        result.addTrade(closedTrade);
        result.addTrade(openTrade);

        assertEquals(1, result.getClosedTrades().size());
        assertEquals(2, result.getTrades().size());
    }

    private OptionsTrade makeTrade(BigDecimal entryPremium, BigDecimal exitPremium) {
        OptionsTrade trade = new OptionsTrade(OptionType.CALL, OptionsTrade.Direction.BUY,
                new BigDecimal("100"), "2024-02-16", "2024-01-15",
                entryPremium, 1, 1000);
        trade.close("2024-02-10", exitPremium);
        return trade;
    }
}
