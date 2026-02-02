package com.stockdownloader.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UnifiedMarketDataTest {

    private UnifiedMarketData unified;

    @BeforeEach
    void setUp() {
        unified = new UnifiedMarketData("SPY");
    }

    @Test
    void symbolIsRequired() {
        assertThrows(NullPointerException.class, () -> new UnifiedMarketData(null));
    }

    @Test
    void defaultValuesAreZeroOrEmpty() {
        assertEquals("SPY", unified.getSymbol());
        assertEquals(BigDecimal.ZERO, unified.getEquityVolume());
        assertEquals(0, unified.getOptionsVolume());
        assertEquals(BigDecimal.ZERO, unified.getCurrentPrice());
        assertFalse(unified.isComplete());
    }

    @Test
    void equityVolumeFromQuoteData() {
        QuoteData quote = new QuoteData();
        quote.setVolume(new BigDecimal("5000000"));
        unified.setQuote(quote);
        assertEquals(new BigDecimal("5000000"), unified.getEquityVolume());
    }

    @Test
    void optionsVolumeFromChain() {
        OptionsChain chain = new OptionsChain("SPY");
        chain.addCall("2024-01-19", makeContract(OptionType.CALL, 300));
        chain.addPut("2024-01-19", makeContract(OptionType.PUT, 200));
        chain.addExpirationDate("2024-01-19");
        unified.setOptionsChain(chain);

        assertEquals(500, unified.getOptionsVolume());
        assertEquals(300, unified.getCallVolume());
        assertEquals(200, unified.getPutVolume());
    }

    @Test
    void totalCombinedVolume() {
        QuoteData quote = new QuoteData();
        quote.setVolume(new BigDecimal("1000"));
        unified.setQuote(quote);

        OptionsChain chain = new OptionsChain("SPY");
        chain.addCall("2024-01-19", makeContract(OptionType.CALL, 500));
        chain.addExpirationDate("2024-01-19");
        unified.setOptionsChain(chain);

        assertEquals(0, new BigDecimal("1500").compareTo(unified.getTotalCombinedVolume()));
    }

    @Test
    void averageDailyVolume() {
        List<PriceData> history = List.of(
                makePrice("2024-01-01", 100_000),
                makePrice("2024-01-02", 200_000),
                makePrice("2024-01-03", 150_000),
                makePrice("2024-01-04", 250_000),
                makePrice("2024-01-05", 300_000)
        );
        unified.setPriceHistory(history);

        // Last 3 days: 150000 + 250000 + 300000 = 700000 / 3 = 233333
        BigDecimal avg = unified.getAverageDailyVolume(3);
        assertTrue(avg.longValue() > 230000);
        assertTrue(avg.longValue() < 240000);
    }

    @Test
    void averageDailyVolumeEmptyHistory() {
        assertEquals(BigDecimal.ZERO, unified.getAverageDailyVolume(5));
    }

    @Test
    void currentPriceFromQuote() {
        QuoteData quote = new QuoteData();
        quote.setLastTradePriceOnly(new BigDecimal("475.50"));
        unified.setQuote(quote);
        assertEquals(new BigDecimal("475.50"), unified.getCurrentPrice());
    }

    @Test
    void currentPriceFallsBackToLatestPrice() {
        PriceData price = new PriceData("2024-01-05",
                new BigDecimal("470"), new BigDecimal("476"),
                new BigDecimal("469"), new BigDecimal("474"),
                new BigDecimal("474"), 100_000);
        unified.setLatestPrice(price);
        assertEquals(new BigDecimal("474"), unified.getCurrentPrice());
    }

    @Test
    void putCallRatioDelegation() {
        OptionsChain chain = new OptionsChain("SPY");
        chain.addCall("2024-01-19", makeContract(OptionType.CALL, 100));
        chain.addPut("2024-01-19", makeContract(OptionType.PUT, 200));
        chain.addExpirationDate("2024-01-19");
        unified.setOptionsChain(chain);

        BigDecimal ratio = unified.getPutCallRatio();
        assertEquals(0, new BigDecimal("2.0000").compareTo(ratio));
    }

    @Test
    void completenessChecks() {
        assertFalse(unified.hasQuoteData());
        assertFalse(unified.hasPriceHistory());
        assertFalse(unified.hasFinancialData());
        assertFalse(unified.hasOptionsChain());
        assertFalse(unified.hasHistoricalData());

        unified.setQuote(new QuoteData());
        assertTrue(unified.hasQuoteData());

        unified.setPriceHistory(List.of(makePrice("2024-01-01", 1000)));
        assertTrue(unified.hasPriceHistory());

        unified.setFinancials(new FinancialData());
        assertTrue(unified.hasFinancialData());

        OptionsChain chain = new OptionsChain("SPY");
        chain.addExpirationDate("2024-01-19");
        unified.setOptionsChain(chain);
        assertTrue(unified.hasOptionsChain());

        unified.setHistorical(new HistoricalData("SPY"));
        assertTrue(unified.hasHistoricalData());

        assertTrue(unified.isComplete());
    }

    @Test
    void priceHistoryIsDefensivelyCopied() {
        List<PriceData> original = new java.util.ArrayList<>();
        original.add(makePrice("2024-01-01", 1000));
        unified.setPriceHistory(original);
        assertThrows(UnsupportedOperationException.class, () ->
                unified.getPriceHistory().clear());
    }

    @Test
    void toStringContainsKeyInfo() {
        String s = unified.toString();
        assertTrue(s.contains("SPY"));
    }

    private static OptionContract makeContract(OptionType type, long volume) {
        return new OptionContract(
                "SYM", type, new BigDecimal("475"), "2024-01-19",
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                volume, 0,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                false);
    }

    private static PriceData makePrice(String date, long volume) {
        return new PriceData(date,
                new BigDecimal("475"), new BigDecimal("476"),
                new BigDecimal("474"), new BigDecimal("475"),
                new BigDecimal("475"), volume);
    }
}
