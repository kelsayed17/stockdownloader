package com.stockdownloader.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TradeTest {

    @Test
    void createLongTrade() {
        var trade = new Trade(Trade.Direction.LONG, "2024-01-01", new BigDecimal("100"), 10);
        assertEquals(Trade.Direction.LONG, trade.getDirection());
        assertEquals(Trade.Status.OPEN, trade.getStatus());
        assertEquals("2024-01-01", trade.getEntryDate());
        assertEquals(new BigDecimal("100"), trade.getEntryPrice());
        assertEquals(10, trade.getShares());
        assertEquals(BigDecimal.ZERO, trade.getProfitLoss());
    }

    @Test
    void closeLongTradeWithProfit() {
        var trade = new Trade(Trade.Direction.LONG, "2024-01-01", new BigDecimal("100"), 10);
        trade.close("2024-02-01", new BigDecimal("120"));

        assertEquals(Trade.Status.CLOSED, trade.getStatus());
        assertEquals("2024-02-01", trade.getExitDate());
        assertEquals(new BigDecimal("120"), trade.getExitPrice());
        assertEquals(0, new BigDecimal("200").compareTo(trade.getProfitLoss()));
        assertTrue(trade.isWin());
    }

    @Test
    void closeLongTradeWithLoss() {
        var trade = new Trade(Trade.Direction.LONG, "2024-01-01", new BigDecimal("100"), 10);
        trade.close("2024-02-01", new BigDecimal("80"));

        assertEquals(Trade.Status.CLOSED, trade.getStatus());
        assertEquals(0, new BigDecimal("-200").compareTo(trade.getProfitLoss()));
        assertFalse(trade.isWin());
    }

    @Test
    void closeShortTradeWithProfit() {
        var trade = new Trade(Trade.Direction.SHORT, "2024-01-01", new BigDecimal("100"), 10);
        trade.close("2024-02-01", new BigDecimal("80"));

        assertEquals(0, new BigDecimal("200").compareTo(trade.getProfitLoss()));
        assertTrue(trade.isWin());
    }

    @Test
    void closeShortTradeWithLoss() {
        var trade = new Trade(Trade.Direction.SHORT, "2024-01-01", new BigDecimal("100"), 10);
        trade.close("2024-02-01", new BigDecimal("120"));

        assertEquals(0, new BigDecimal("-200").compareTo(trade.getProfitLoss()));
        assertFalse(trade.isWin());
    }

    @Test
    void closingAlreadyClosedTradeThrows() {
        var trade = new Trade(Trade.Direction.LONG, "2024-01-01", new BigDecimal("100"), 10);
        trade.close("2024-02-01", new BigDecimal("110"));

        assertThrows(IllegalStateException.class, () ->
                trade.close("2024-03-01", new BigDecimal("120")));
    }

    @Test
    void zeroSharesThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new Trade(Trade.Direction.LONG, "2024-01-01", new BigDecimal("100"), 0));
    }

    @Test
    void negativeSharesThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new Trade(Trade.Direction.LONG, "2024-01-01", new BigDecimal("100"), -5));
    }

    @Test
    void nullDirectionThrows() {
        assertThrows(NullPointerException.class, () ->
                new Trade(null, "2024-01-01", new BigDecimal("100"), 10));
    }

    @Test
    void returnPercentageIsCalculated() {
        var trade = new Trade(Trade.Direction.LONG, "2024-01-01", new BigDecimal("100"), 10);
        trade.close("2024-02-01", new BigDecimal("110"));

        // 10% return
        assertTrue(trade.getReturnPct().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(0, new BigDecimal("10").compareTo(trade.getReturnPct().setScale(0, java.math.RoundingMode.HALF_UP)));
    }

    @Test
    void toStringFormatsCorrectly() {
        var trade = new Trade(Trade.Direction.LONG, "2024-01-01", new BigDecimal("100"), 10);
        trade.close("2024-02-01", new BigDecimal("110"));

        String str = trade.toString();
        assertTrue(str.contains("LONG"));
        assertTrue(str.contains("CLOSED"));
        assertTrue(str.contains("2024-01-01"));
        assertTrue(str.contains("2024-02-01"));
    }
}
