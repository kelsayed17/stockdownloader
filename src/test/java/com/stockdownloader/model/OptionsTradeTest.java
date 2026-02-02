package com.stockdownloader.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OptionsTradeTest {

    @Test
    void constructorValidatesNullFields() {
        assertThrows(NullPointerException.class, () ->
                new OptionsTrade(null, OptionsTrade.Direction.BUY,
                        BigDecimal.ONE, "2024-01-19", "2024-01-01", BigDecimal.ONE, 1, 100));
    }

    @Test
    void constructorRejectsNonPositiveContracts() {
        assertThrows(IllegalArgumentException.class, () ->
                new OptionsTrade(OptionType.CALL, OptionsTrade.Direction.BUY,
                        BigDecimal.ONE, "2024-01-19", "2024-01-01", BigDecimal.ONE, 0, 100));
    }

    @Test
    void newTradeIsOpen() {
        OptionsTrade trade = makeLongCall();
        assertEquals(OptionsTrade.Status.OPEN, trade.getStatus());
        assertEquals(BigDecimal.ZERO, trade.getProfitLoss());
    }

    @Test
    void closeLongCallWithProfit() {
        OptionsTrade trade = makeLongCall();
        // Bought at 5.00, close at 8.00 → profit = (8-5) * 2 * 100 = 600
        trade.close("2024-01-15", new BigDecimal("8.00"));

        assertEquals(OptionsTrade.Status.CLOSED, trade.getStatus());
        assertEquals(0, new BigDecimal("600.00").compareTo(trade.getProfitLoss().setScale(2)));
        assertTrue(trade.isWin());
    }

    @Test
    void closeLongCallWithLoss() {
        OptionsTrade trade = makeLongCall();
        // Bought at 5.00, close at 2.00 → loss = (2-5) * 2 * 100 = -600
        trade.close("2024-01-15", new BigDecimal("2.00"));

        assertEquals(0, new BigDecimal("-600.00").compareTo(trade.getProfitLoss().setScale(2)));
        assertFalse(trade.isWin());
    }

    @Test
    void closeShortPutWithProfit() {
        OptionsTrade trade = new OptionsTrade(OptionType.PUT, OptionsTrade.Direction.SELL,
                new BigDecimal("460"), "2024-01-19", "2024-01-01",
                new BigDecimal("4.00"), 1, 500);
        // Sold at 4.00, close at 1.00 → profit = (4-1) * 1 * 100 = 300
        trade.close("2024-01-15", new BigDecimal("1.00"));

        assertEquals(0, new BigDecimal("300.00").compareTo(trade.getProfitLoss().setScale(2)));
        assertTrue(trade.isWin());
    }

    @Test
    void expireWorthless() {
        OptionsTrade trade = new OptionsTrade(OptionType.CALL, OptionsTrade.Direction.SELL,
                new BigDecimal("500"), "2024-01-19", "2024-01-01",
                new BigDecimal("3.00"), 1, 200);
        // Sold call expires worthless → full premium profit = 3 * 1 * 100 = 300
        trade.expire("2024-01-19", BigDecimal.ZERO);

        assertEquals(OptionsTrade.Status.EXPIRED, trade.getStatus());
        assertEquals(0, new BigDecimal("300.00").compareTo(trade.getProfitLoss().setScale(2)));
    }

    @Test
    void cannotCloseAlreadyClosedTrade() {
        OptionsTrade trade = makeLongCall();
        trade.close("2024-01-15", new BigDecimal("6.00"));
        assertThrows(IllegalStateException.class, () ->
                trade.close("2024-01-16", new BigDecimal("7.00")));
    }

    @Test
    void cannotExpireAlreadyExpiredTrade() {
        OptionsTrade trade = makeLongCall();
        trade.expire("2024-01-19", BigDecimal.ZERO);
        assertThrows(IllegalStateException.class, () ->
                trade.expire("2024-01-19", BigDecimal.ZERO));
    }

    @Test
    void totalEntryCost() {
        OptionsTrade trade = makeLongCall();
        // 5.00 * 2 * 100 = 1000
        assertEquals(0, new BigDecimal("1000").compareTo(trade.totalEntryCost()));
    }

    @Test
    void returnPercentage() {
        OptionsTrade trade = makeLongCall();
        trade.close("2024-01-15", new BigDecimal("7.50"));
        // P/L = (7.50-5.00)*2*100 = 500, Cost = 5*2*100 = 1000, Return = 50%
        assertTrue(trade.getReturnPct().doubleValue() > 49.0);
        assertTrue(trade.getReturnPct().doubleValue() < 51.0);
    }

    @Test
    void volumeIsCaptured() {
        OptionsTrade trade = makeLongCall();
        assertEquals(1000, trade.getEntryVolume());
    }

    @Test
    void toStringContainsKeyFields() {
        OptionsTrade trade = makeLongCall();
        String s = trade.toString();
        assertTrue(s.contains("BUY"));
        assertTrue(s.contains("CALL"));
        assertTrue(s.contains("480.00"));
        assertTrue(s.contains("vol:1000"));
    }

    private OptionsTrade makeLongCall() {
        return new OptionsTrade(OptionType.CALL, OptionsTrade.Direction.BUY,
                new BigDecimal("480"), "2024-01-19", "2024-01-01",
                new BigDecimal("5.00"), 2, 1000);
    }
}
