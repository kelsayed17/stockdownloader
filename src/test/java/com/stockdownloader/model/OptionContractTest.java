package com.stockdownloader.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OptionContractTest {

    @Test
    void createCallContract() {
        var contract = makeContract(OptionType.CALL, "500", "5.50", "5.40", "5.60", 1000, 5000, true);
        assertEquals(OptionType.CALL, contract.getOptionType());
        assertEquals(new BigDecimal("500"), contract.getStrike());
        assertEquals(1000, contract.getVolume());
        assertEquals(5000, contract.getOpenInterest());
        assertTrue(contract.isInTheMoney());
    }

    @Test
    void createPutContract() {
        var contract = makeContract(OptionType.PUT, "490", "3.20", "3.10", "3.30", 500, 2000, false);
        assertEquals(OptionType.PUT, contract.getOptionType());
        assertFalse(contract.isInTheMoney());
    }

    @Test
    void midPriceCalculation() {
        var contract = makeContract(OptionType.CALL, "500", "5.50", "5.00", "6.00", 100, 100, true);
        assertEquals(0, new BigDecimal("5.50").compareTo(contract.getMidPrice()));
    }

    @Test
    void midPriceUsesLastWhenBidAskZero() {
        var contract = makeContract(OptionType.CALL, "500", "5.50", "0", "0", 100, 100, false);
        assertEquals(0, new BigDecimal("5.50").compareTo(contract.getMidPrice()));
    }

    @Test
    void spreadCalculation() {
        var contract = makeContract(OptionType.CALL, "500", "5.50", "5.00", "6.00", 100, 100, true);
        assertEquals(0, new BigDecimal("1.00").compareTo(contract.getSpread()));
    }

    @Test
    void callIntrinsicValueITM() {
        var contract = makeContract(OptionType.CALL, "490", "15.00", "14.50", "15.50", 100, 100, true);
        BigDecimal iv = contract.getIntrinsicValue(new BigDecimal("500"));
        assertEquals(0, new BigDecimal("10").compareTo(iv));
    }

    @Test
    void callIntrinsicValueOTM() {
        var contract = makeContract(OptionType.CALL, "510", "2.00", "1.50", "2.50", 100, 100, false);
        BigDecimal iv = contract.getIntrinsicValue(new BigDecimal("500"));
        assertEquals(0, BigDecimal.ZERO.compareTo(iv));
    }

    @Test
    void putIntrinsicValueITM() {
        var contract = makeContract(OptionType.PUT, "510", "15.00", "14.50", "15.50", 100, 100, true);
        BigDecimal iv = contract.getIntrinsicValue(new BigDecimal("500"));
        assertEquals(0, new BigDecimal("10").compareTo(iv));
    }

    @Test
    void putIntrinsicValueOTM() {
        var contract = makeContract(OptionType.PUT, "490", "2.00", "1.50", "2.50", 100, 100, false);
        BigDecimal iv = contract.getIntrinsicValue(new BigDecimal("500"));
        assertEquals(0, BigDecimal.ZERO.compareTo(iv));
    }

    @Test
    void extrinsicValueCalculation() {
        var contract = makeContract(OptionType.CALL, "490", "15.00", "14.50", "15.50", 100, 100, true);
        BigDecimal extrinsic = contract.getExtrinsicValue(new BigDecimal("500"));
        // Intrinsic = 10, Last = 15, Extrinsic = 5
        assertEquals(0, new BigDecimal("5").compareTo(extrinsic));
    }

    @Test
    void daysToExpirationCalculation() {
        var contract = makeContract(OptionType.CALL, "500", "5.00", "4.50", "5.50", 100, 100, false);
        int dte = contract.getDaysToExpiration("2024-01-01");
        assertEquals(30, dte);
    }

    @Test
    void negativeVolumeThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            new OptionContract("SYM", "SPY", OptionType.CALL,
                    BigDecimal.TEN, "2024-01-31", BigDecimal.ONE,
                    BigDecimal.ONE, BigDecimal.ONE, -1, 0,
                    Greeks.zero(), false);
        });
    }

    @Test
    void negativeOpenInterestThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            new OptionContract("SYM", "SPY", OptionType.CALL,
                    BigDecimal.TEN, "2024-01-31", BigDecimal.ONE,
                    BigDecimal.ONE, BigDecimal.ONE, 0, -1,
                    Greeks.zero(), false);
        });
    }

    @Test
    void nullOptionTypeThrows() {
        assertThrows(NullPointerException.class, () -> {
            new OptionContract("SYM", "SPY", null,
                    BigDecimal.TEN, "2024-01-31", BigDecimal.ONE,
                    BigDecimal.ONE, BigDecimal.ONE, 0, 0,
                    Greeks.zero(), false);
        });
    }

    @Test
    void toStringFormatsCorrectly() {
        var contract = makeContract(OptionType.CALL, "500", "5.50", "5.00", "6.00", 1000, 5000, true);
        String str = contract.toString();
        assertTrue(str.contains("CALL"));
        assertTrue(str.contains("500"));
        assertTrue(str.contains("ITM"));
        assertTrue(str.contains("1000"));
    }

    private static OptionContract makeContract(OptionType type, String strike, String lastPrice,
                                                String bid, String ask, long volume, long oi, boolean itm) {
        return new OptionContract("SPY240131C00500000", "SPY", type,
                new BigDecimal(strike), "2024-01-31",
                new BigDecimal(lastPrice), new BigDecimal(bid), new BigDecimal(ask),
                volume, oi, Greeks.zero(), itm);
    }
}
