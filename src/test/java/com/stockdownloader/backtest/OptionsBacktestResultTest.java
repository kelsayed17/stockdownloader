package com.stockdownloader.backtest;

import com.stockdownloader.model.OptionTrade;
import com.stockdownloader.model.OptionType;
import com.stockdownloader.model.Trade;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OptionsBacktestResultTest {

    @Test
    void initialState() {
        var result = new OptionsBacktestResult("Test", new BigDecimal("100000"));
        assertEquals("Test", result.getStrategyName());
        assertEquals(0, new BigDecimal("100000").compareTo(result.getInitialCapital()));
        assertEquals(0, new BigDecimal("100000").compareTo(result.getFinalCapital()));
        assertEquals(0, result.getTotalTrades());
    }

    @Test
    void totalReturn() {
        var result = new OptionsBacktestResult("Test", new BigDecimal("100000"));
        result.setFinalCapital(new BigDecimal("110000"));
        assertEquals(0, new BigDecimal("10").compareTo(result.getTotalReturn().setScale(0, java.math.RoundingMode.HALF_UP)));
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

        var winTrade = makeClosedTrade(OptionType.CALL, Trade.Direction.LONG,
                new BigDecimal("5"), new BigDecimal("8"));
        var loseTrade = makeClosedTrade(OptionType.CALL, Trade.Direction.LONG,
                new BigDecimal("5"), new BigDecimal("2"));

        result.addTrade(winTrade);
        result.addTrade(loseTrade);

        assertEquals(2, result.getTotalTrades());
        assertEquals(1, result.getWinningTrades());
        assertEquals(1, result.getLosingTrades());
        // 50% win rate
        assertTrue(result.getWinRate().compareTo(new BigDecimal("49")) > 0);
        assertTrue(result.getWinRate().compareTo(new BigDecimal("51")) < 0);
    }

    @Test
    void premiumCollectedAndPaid() {
        var result = new OptionsBacktestResult("Test", new BigDecimal("100000"));

        // Short option: premium collected
        var shortTrade = new OptionTrade(OptionType.CALL, Trade.Direction.SHORT,
                new BigDecimal("510"), "2024-02-16", "2024-01-15",
                new BigDecimal("3.00"), 2, 1000);
        shortTrade.closeAtExpiration(new BigDecimal("500")); // expired worthless

        // Long option: premium paid
        var longTrade = new OptionTrade(OptionType.PUT, Trade.Direction.LONG,
                new BigDecimal("490"), "2024-02-16", "2024-01-15",
                new BigDecimal("5.00"), 1, 1000);
        longTrade.closeAtExpiration(new BigDecimal("500")); // expired worthless

        result.addTrade(shortTrade);
        result.addTrade(longTrade);

        // Premium collected: 3 * 2 * 100 = 600
        assertEquals(0, new BigDecimal("600").compareTo(result.getTotalPremiumCollected()));
        // Premium paid: 5 * 1 * 100 = 500
        assertEquals(0, new BigDecimal("500").compareTo(result.getTotalPremiumPaid()));
    }

    @Test
    void assignmentAndExpirationCounts() {
        var result = new OptionsBacktestResult("Test", new BigDecimal("100000"));

        // ITM at expiration = assigned
        var assignedTrade = new OptionTrade(OptionType.CALL, Trade.Direction.LONG,
                new BigDecimal("490"), "2024-02-16", "2024-01-15",
                new BigDecimal("15.00"), 1, 1000);
        assignedTrade.closeAtExpiration(new BigDecimal("510"));

        // OTM at expiration = expired worthless
        var expiredTrade = new OptionTrade(OptionType.CALL, Trade.Direction.LONG,
                new BigDecimal("520"), "2024-02-16", "2024-01-15",
                new BigDecimal("2.00"), 1, 1000);
        expiredTrade.closeAtExpiration(new BigDecimal("510"));

        result.addTrade(assignedTrade);
        result.addTrade(expiredTrade);

        assertEquals(1, result.getAssignedCount());
        assertEquals(1, result.getExpiredWorthlessCount());
        // 50% assignment rate
        assertTrue(result.getAssignmentRate().compareTo(new BigDecimal("49")) > 0);
    }

    @Test
    void callAndPutTradeCounts() {
        var result = new OptionsBacktestResult("Test", new BigDecimal("100000"));

        result.addTrade(makeClosedTrade(OptionType.CALL, Trade.Direction.LONG,
                new BigDecimal("5"), new BigDecimal("8")));
        result.addTrade(makeClosedTrade(OptionType.CALL, Trade.Direction.LONG,
                new BigDecimal("5"), new BigDecimal("2")));
        result.addTrade(makeClosedTrade(OptionType.PUT, Trade.Direction.LONG,
                new BigDecimal("4"), new BigDecimal("6")));

        assertEquals(2, result.getTotalCallTrades());
        assertEquals(1, result.getTotalPutTrades());
    }

    @Test
    void profitFactor() {
        var result = new OptionsBacktestResult("Test", new BigDecimal("100000"));
        result.addTrade(makeClosedTrade(OptionType.CALL, Trade.Direction.LONG,
                new BigDecimal("5"), new BigDecimal("10")));  // +500
        result.addTrade(makeClosedTrade(OptionType.CALL, Trade.Direction.LONG,
                new BigDecimal("5"), new BigDecimal("3")));   // -200
        assertTrue(result.getProfitFactor().compareTo(BigDecimal.ONE) > 0);
    }

    @Test
    void maxDrawdown() {
        var result = new OptionsBacktestResult("Test", new BigDecimal("100000"));
        result.setEquityCurve(List.of(
                new BigDecimal("100000"),
                new BigDecimal("110000"),
                new BigDecimal("95000"),  // drawdown from 110000
                new BigDecimal("105000")
        ));
        assertTrue(result.getMaxDrawdown().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void sharpeRatio() {
        var result = new OptionsBacktestResult("Test", new BigDecimal("100000"));
        result.setEquityCurve(List.of(
                new BigDecimal("100000"),
                new BigDecimal("101000"),
                new BigDecimal("102000"),
                new BigDecimal("103000"),
                new BigDecimal("104000")
        ));
        // Consistent positive returns should give positive Sharpe
        assertTrue(result.getSharpeRatio(252).compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void averageEntryVolume() {
        var result = new OptionsBacktestResult("Test", new BigDecimal("100000"));
        var trade1 = new OptionTrade(OptionType.CALL, Trade.Direction.LONG,
                new BigDecimal("500"), "2024-02-16", "2024-01-15",
                new BigDecimal("5"), 1, 1000);
        trade1.closeAtPremium("2024-02-01", new BigDecimal("6"), 500);

        var trade2 = new OptionTrade(OptionType.CALL, Trade.Direction.LONG,
                new BigDecimal("500"), "2024-02-16", "2024-01-16",
                new BigDecimal("5"), 1, 3000);
        trade2.closeAtPremium("2024-02-02", new BigDecimal("6"), 500);

        result.addTrade(trade1);
        result.addTrade(trade2);

        assertEquals(2000, result.getAverageEntryVolume());
    }

    private OptionTrade makeClosedTrade(OptionType type, Trade.Direction dir,
                                         BigDecimal entryPremium, BigDecimal exitPremium) {
        var trade = new OptionTrade(type, dir,
                new BigDecimal("500"), "2024-02-16", "2024-01-15",
                entryPremium, 1, 1000);
        trade.closeAtPremium("2024-02-01", exitPremium, 500);
        return trade;
    }
}
