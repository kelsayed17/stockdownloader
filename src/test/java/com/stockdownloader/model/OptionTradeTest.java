package com.stockdownloader.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OptionTradeTest {

    @Test
    void createLongCallTrade() {
        var trade = new OptionTrade(OptionType.CALL, Trade.Direction.LONG,
                new BigDecimal("500"), "2024-02-16", "2024-01-15",
                new BigDecimal("5.00"), 1, 1000);

        assertEquals(OptionType.CALL, trade.getOptionType());
        assertEquals(Trade.Direction.LONG, trade.getDirection());
        assertEquals(OptionTrade.Status.OPEN, trade.getStatus());
        assertEquals(0, new BigDecimal("500").compareTo(trade.getStrike()));
        assertEquals("2024-02-16", trade.getExpirationDate());
        assertEquals(1, trade.getContracts());
        assertEquals(1000, trade.getEntryVolume());
    }

    @Test
    void closeAtPremiumWithProfit() {
        var trade = new OptionTrade(OptionType.CALL, Trade.Direction.LONG,
                new BigDecimal("500"), "2024-02-16", "2024-01-15",
                new BigDecimal("5.00"), 1, 1000);
        trade.closeAtPremium("2024-02-01", new BigDecimal("8.00"), 500);

        assertEquals(OptionTrade.Status.CLOSED_EXIT, trade.getStatus());
        // P/L = (8 - 5) * 1 * 100 = 300
        assertEquals(0, new BigDecimal("300").compareTo(trade.getProfitLoss()));
        assertTrue(trade.isWin());
        assertEquals(500, trade.getExitVolume());
    }

    @Test
    void closeAtPremiumWithLoss() {
        var trade = new OptionTrade(OptionType.CALL, Trade.Direction.LONG,
                new BigDecimal("500"), "2024-02-16", "2024-01-15",
                new BigDecimal("5.00"), 2, 1000);
        trade.closeAtPremium("2024-02-01", new BigDecimal("2.00"), 300);

        // P/L = (2 - 5) * 2 * 100 = -600
        assertEquals(0, new BigDecimal("-600").compareTo(trade.getProfitLoss()));
        assertFalse(trade.isWin());
    }

    @Test
    void shortCallCloseWithProfit() {
        var trade = new OptionTrade(OptionType.CALL, Trade.Direction.SHORT,
                new BigDecimal("510"), "2024-02-16", "2024-01-15",
                new BigDecimal("3.00"), 1, 1000);
        trade.closeAtPremium("2024-02-01", new BigDecimal("1.00"), 200);

        // P/L = (3 - 1) * 1 * 100 = 200 (premium decay profit)
        assertEquals(0, new BigDecimal("200").compareTo(trade.getProfitLoss()));
        assertTrue(trade.isWin());
    }

    @Test
    void closeAtExpirationITMCall() {
        var trade = new OptionTrade(OptionType.CALL, Trade.Direction.LONG,
                new BigDecimal("490"), "2024-02-16", "2024-01-15",
                new BigDecimal("15.00"), 1, 1000);
        trade.closeAtExpiration(new BigDecimal("510"));

        assertEquals(OptionTrade.Status.CLOSED_ASSIGNED, trade.getStatus());
        // Intrinsic = 510 - 490 = 20, P/L = (20 - 15) * 100 = 500
        assertEquals(0, new BigDecimal("500").compareTo(trade.getProfitLoss()));
    }

    @Test
    void closeAtExpirationOTMCall() {
        var trade = new OptionTrade(OptionType.CALL, Trade.Direction.LONG,
                new BigDecimal("510"), "2024-02-16", "2024-01-15",
                new BigDecimal("5.00"), 1, 1000);
        trade.closeAtExpiration(new BigDecimal("500"));

        assertEquals(OptionTrade.Status.CLOSED_EXPIRED, trade.getStatus());
        // P/L = (0 - 5) * 100 = -500
        assertEquals(0, new BigDecimal("-500").compareTo(trade.getProfitLoss()));
    }

    @Test
    void closeAtExpirationITMPut() {
        var trade = new OptionTrade(OptionType.PUT, Trade.Direction.LONG,
                new BigDecimal("510"), "2024-02-16", "2024-01-15",
                new BigDecimal("15.00"), 1, 1000);
        trade.closeAtExpiration(new BigDecimal("490"));

        assertEquals(OptionTrade.Status.CLOSED_ASSIGNED, trade.getStatus());
        // Intrinsic = 510 - 490 = 20, P/L = (20 - 15) * 100 = 500
        assertEquals(0, new BigDecimal("500").compareTo(trade.getProfitLoss()));
    }

    @Test
    void closeAtExpirationShortPutOTM() {
        var trade = new OptionTrade(OptionType.PUT, Trade.Direction.SHORT,
                new BigDecimal("490"), "2024-02-16", "2024-01-15",
                new BigDecimal("3.00"), 1, 1000);
        trade.closeAtExpiration(new BigDecimal("500"));

        assertEquals(OptionTrade.Status.CLOSED_EXPIRED, trade.getStatus());
        // P/L = (3 - 0) * 100 = 300 (keep entire premium)
        assertEquals(0, new BigDecimal("300").compareTo(trade.getProfitLoss()));
    }

    @Test
    void multipleContractsMultipliesPL() {
        var trade = new OptionTrade(OptionType.CALL, Trade.Direction.LONG,
                new BigDecimal("500"), "2024-02-16", "2024-01-15",
                new BigDecimal("5.00"), 5, 1000);
        trade.closeAtPremium("2024-02-01", new BigDecimal("8.00"), 500);

        // P/L = (8 - 5) * 5 * 100 = 1500
        assertEquals(0, new BigDecimal("1500").compareTo(trade.getProfitLoss()));
    }

    @Test
    void closingAlreadyClosedTradeThrows() {
        var trade = new OptionTrade(OptionType.CALL, Trade.Direction.LONG,
                new BigDecimal("500"), "2024-02-16", "2024-01-15",
                new BigDecimal("5.00"), 1, 1000);
        trade.closeAtPremium("2024-02-01", new BigDecimal("8.00"), 500);

        assertThrows(IllegalStateException.class, () -> {
            trade.closeAtPremium("2024-02-10", new BigDecimal("9.00"), 100);
        });
    }

    @Test
    void zeroContractsThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            new OptionTrade(OptionType.CALL, Trade.Direction.LONG,
                    new BigDecimal("500"), "2024-02-16", "2024-01-15",
                    new BigDecimal("5.00"), 0, 1000);
        });
    }

    @Test
    void maxRiskLongOption() {
        var trade = new OptionTrade(OptionType.CALL, Trade.Direction.LONG,
                new BigDecimal("500"), "2024-02-16", "2024-01-15",
                new BigDecimal("5.00"), 2, 1000);
        // Max risk for long = premium * contracts * 100 = 5 * 2 * 100 = 1000
        assertEquals(0, new BigDecimal("1000").compareTo(trade.getMaxRisk()));
    }

    @Test
    void returnPercentageCalculation() {
        var trade = new OptionTrade(OptionType.CALL, Trade.Direction.LONG,
                new BigDecimal("500"), "2024-02-16", "2024-01-15",
                new BigDecimal("5.00"), 1, 1000);
        trade.closeAtPremium("2024-02-01", new BigDecimal("10.00"), 500);

        // Return = ((10-5) * 100) / (5 * 100) * 100 = 100%
        assertTrue(trade.getReturnPct().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void toStringFormatsCorrectly() {
        var trade = new OptionTrade(OptionType.CALL, Trade.Direction.LONG,
                new BigDecimal("500"), "2024-02-16", "2024-01-15",
                new BigDecimal("5.00"), 1, 1000);
        String str = trade.toString();
        assertTrue(str.contains("LONG"));
        assertTrue(str.contains("CALL"));
        assertTrue(str.contains("500"));
        assertTrue(str.contains("2024-02-16"));
    }
}
