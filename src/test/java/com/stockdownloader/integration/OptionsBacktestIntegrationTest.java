package com.stockdownloader.integration;

import com.stockdownloader.backtest.OptionsBacktestEngine;
import com.stockdownloader.backtest.OptionsBacktestResult;
import com.stockdownloader.data.CsvPriceDataLoader;
import com.stockdownloader.model.*;
import com.stockdownloader.strategy.CoveredCallStrategy;
import com.stockdownloader.strategy.OptionsStrategy;
import com.stockdownloader.strategy.ProtectivePutStrategy;
import com.stockdownloader.util.BlackScholesCalculator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that exercise the full options backtesting pipeline:
 * data loading → strategy evaluation → engine execution → result analysis.
 */
class OptionsBacktestIntegrationTest {

    private static List<PriceData> testData;

    @BeforeAll
    static void loadTestData() {
        var stream = OptionsBacktestIntegrationTest.class
                .getResourceAsStream("/test-price-data.csv");
        if (stream != null) {
            testData = CsvPriceDataLoader.loadFromStream(stream);
        }
        if (testData == null || testData.isEmpty()) {
            // Fall back to synthetic data if no CSV available
            testData = generateSyntheticData(200);
        }
    }

    @Test
    void coveredCallBacktestProducesValidResults() {
        var strategy = new CoveredCallStrategy(20, new BigDecimal("0.05"), 30, new BigDecimal("0.03"));
        var engine = new OptionsBacktestEngine(new BigDecimal("100000"), BigDecimal.ZERO);

        OptionsBacktestResult result = engine.run(strategy, testData);

        assertNotNull(result);
        assertNotNull(result.getFinalCapital());
        assertEquals(testData.size(), result.getEquityCurve().size());
        assertEquals(testData.getFirst().date(), result.getStartDate());
        assertEquals(testData.getLast().date(), result.getEndDate());
    }

    @Test
    void protectivePutBacktestProducesValidResults() {
        var strategy = new ProtectivePutStrategy(20, new BigDecimal("0.05"), 30, 5);
        var engine = new OptionsBacktestEngine(new BigDecimal("100000"), BigDecimal.ZERO);

        OptionsBacktestResult result = engine.run(strategy, testData);

        assertNotNull(result);
        assertNotNull(result.getFinalCapital());
    }

    @Test
    void multipleStrategiesCompare() {
        List<OptionsStrategy> strategies = List.of(
                new CoveredCallStrategy(20, new BigDecimal("0.03"), 30, new BigDecimal("0.03")),
                new CoveredCallStrategy(20, new BigDecimal("0.05"), 30, new BigDecimal("0.03")),
                new ProtectivePutStrategy(20, new BigDecimal("0.05"), 30, 5),
                new ProtectivePutStrategy(20, new BigDecimal("0.03"), 45, 10)
        );

        var engine = new OptionsBacktestEngine(new BigDecimal("100000"), new BigDecimal("0.65"));
        List<OptionsBacktestResult> results = new ArrayList<>();

        for (OptionsStrategy strategy : strategies) {
            OptionsBacktestResult result = engine.run(strategy, testData);
            results.add(result);
            assertNotNull(result.getStrategyName());
        }

        assertEquals(4, results.size());
    }

    @Test
    void volumeIsCapturedThroughPipeline() {
        var strategy = new CoveredCallStrategy(5, new BigDecimal("0.05"), 10, new BigDecimal("0.03"));
        var engine = new OptionsBacktestEngine(new BigDecimal("100000"), BigDecimal.ZERO);

        OptionsBacktestResult result = engine.run(strategy, testData);

        // Verify volume is tracked
        for (OptionsTrade trade : result.getTrades()) {
            assertTrue(trade.getEntryVolume() >= 0, "Every trade should capture volume");
        }
    }

    @Test
    void unifiedMarketDataIntegration() {
        var unified = new UnifiedMarketData("SPY");

        // Set up all data sources
        unified.setPriceHistory(testData);
        unified.setLatestPrice(testData.getLast());

        QuoteData quote = new QuoteData();
        quote.setLastTradePriceOnly(testData.getLast().close());
        quote.setVolume(BigDecimal.valueOf(testData.getLast().volume()));
        unified.setQuote(quote);

        OptionsChain chain = buildSampleChain(testData.getLast().close());
        unified.setOptionsChain(chain);

        unified.setHistorical(new HistoricalData("SPY"));
        unified.setFinancials(new FinancialData());

        // Verify unified data
        assertTrue(unified.isComplete());
        assertTrue(unified.getCurrentPrice().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(unified.hasPriceHistory());
        assertTrue(unified.hasOptionsChain());

        // Equity + options volume
        assertTrue(unified.getTotalCombinedVolume().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(unified.getOptionsVolume() > 0);
        assertTrue(unified.getAverageDailyVolume(20).compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void blackScholesIntegrationWithBacktest() {
        BigDecimal spot = testData.getLast().close();
        BigDecimal strike = spot.multiply(new BigDecimal("1.05"));
        BigDecimal timeToExpiry = new BigDecimal("0.0822"); // ~30 days
        BigDecimal rate = new BigDecimal("0.05");

        BigDecimal[] closePrices = testData.stream()
                .map(PriceData::close).toArray(BigDecimal[]::new);
        BigDecimal vol = BlackScholesCalculator.estimateVolatility(closePrices, 20);

        BigDecimal callPrice = BlackScholesCalculator.price(
                OptionType.CALL, spot, strike, timeToExpiry, rate, vol);
        BigDecimal putPrice = BlackScholesCalculator.price(
                OptionType.PUT, spot, strike, timeToExpiry, rate, vol);

        assertTrue(callPrice.doubleValue() > 0, "Call should have positive price");
        assertTrue(putPrice.doubleValue() > 0, "Put should have positive price");
        assertTrue(vol.doubleValue() > 0, "Estimated vol should be positive");

        BigDecimal delta = BlackScholesCalculator.delta(
                OptionType.CALL, spot, strike, timeToExpiry, rate, vol);
        assertTrue(delta.doubleValue() > 0 && delta.doubleValue() < 1);
    }

    @Test
    void optionsChainSearchIntegration() {
        BigDecimal price = testData.getLast().close();
        OptionsChain chain = buildSampleChain(price);

        // Find nearest strike to current price
        var nearest = chain.findNearestStrike("2024-03-15", OptionType.CALL, price);
        assertTrue(nearest.isPresent());

        // Get all contracts at a specific strike
        BigDecimal targetStrike = nearest.get().strike();
        var contracts = chain.getContractsAtStrike("2024-03-15", targetStrike);
        assertFalse(contracts.isEmpty());

        // Volume metrics
        assertTrue(chain.getTotalVolume() > 0);
        assertTrue(chain.getTotalCallOpenInterest() > 0);
    }

    // --- Helpers ---

    private static List<PriceData> generateSyntheticData(int days) {
        List<PriceData> data = new ArrayList<>();
        double price = 450;
        for (int i = 0; i < days; i++) {
            price += Math.sin(i * 0.1) * 2 + 0.1;
            BigDecimal p = BigDecimal.valueOf(price);
            BigDecimal h = p.add(new BigDecimal("2"));
            BigDecimal l = p.subtract(new BigDecimal("2"));
            data.add(new PriceData("2024-%02d-%02d".formatted((i / 28) + 1, (i % 28) + 1),
                    p, h, l, p, p, 500000 + (long)(Math.random() * 100000)));
        }
        return data;
    }

    private OptionsChain buildSampleChain(BigDecimal currentPrice) {
        OptionsChain chain = new OptionsChain("SPY");
        chain.setUnderlyingPrice(currentPrice);
        chain.addExpirationDate("2024-03-15");

        BigDecimal[] strikes = {
                currentPrice.subtract(new BigDecimal("20")),
                currentPrice.subtract(new BigDecimal("10")),
                currentPrice,
                currentPrice.add(new BigDecimal("10")),
                currentPrice.add(new BigDecimal("20"))
        };

        for (BigDecimal strike : strikes) {
            chain.addCall("2024-03-15", new OptionContract(
                    "SPY-C-" + strike, OptionType.CALL, strike, "2024-03-15",
                    new BigDecimal("5.00"), new BigDecimal("4.90"), new BigDecimal("5.10"),
                    1000, 5000,
                    new BigDecimal("0.20"),
                    new BigDecimal("0.50"), new BigDecimal("0.02"),
                    new BigDecimal("-0.10"), new BigDecimal("0.15"),
                    strike.compareTo(currentPrice) < 0));

            chain.addPut("2024-03-15", new OptionContract(
                    "SPY-P-" + strike, OptionType.PUT, strike, "2024-03-15",
                    new BigDecimal("4.00"), new BigDecimal("3.90"), new BigDecimal("4.10"),
                    800, 4000,
                    new BigDecimal("0.22"),
                    new BigDecimal("-0.40"), new BigDecimal("0.02"),
                    new BigDecimal("-0.08"), new BigDecimal("0.12"),
                    strike.compareTo(currentPrice) > 0));
        }

        return chain;
    }
}
