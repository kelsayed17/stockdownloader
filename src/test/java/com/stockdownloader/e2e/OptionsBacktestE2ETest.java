package com.stockdownloader.e2e;

import com.stockdownloader.analysis.VolumeAnalyzer;
import com.stockdownloader.backtest.OptionsBacktestEngine;
import com.stockdownloader.backtest.OptionsBacktestResult;
import com.stockdownloader.backtest.OptionsReportFormatter;
import com.stockdownloader.model.*;
import com.stockdownloader.strategy.*;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests for the full options backtesting pipeline including
 * unified market data, volume analysis, and all four options strategies
 * across different market regimes.
 */
class OptionsBacktestE2ETest {

    private static final BigDecimal INITIAL_CAPITAL = new BigDecimal("100000");
    private static final BigDecimal COMMISSION = new BigDecimal("0.65");

    // ======================== BULL MARKET SCENARIO ========================

    @Test
    void bullMarketAllStrategies() {
        List<PriceData> data = generateBullMarket(250);
        var engine = new OptionsBacktestEngine(INITIAL_CAPITAL, COMMISSION);

        List<OptionsStrategy> strategies = List.of(
                new CoveredCallStrategy(10, new BigDecimal("5"), 30, 0),
                new ProtectivePutStrategy(10, new BigDecimal("3"), 30, 0, new BigDecimal("2")),
                new StraddleStrategy(10, 30, 0, new BigDecimal("50"), new BigDecimal("0.10")),
                new IronCondorStrategy(10, new BigDecimal("5"), new BigDecimal("3"), 30, 0)
        );

        List<OptionsBacktestResult> results = new ArrayList<>();
        for (OptionsStrategy strategy : strategies) {
            OptionsBacktestResult result = engine.run(strategy, data);
            assertNotNull(result);
            assertEquals(data.size(), result.getEquityCurve().size());
            results.add(result);
        }

        // Verify comparison works - just call directly (no assertDoesNotThrow needed)
        OptionsReportFormatter.printComparison(results, data);
    }

    // ======================== BEAR MARKET SCENARIO ========================

    @Test
    void bearMarketProtectivePut() {
        List<PriceData> data = generateBearMarket(250);
        var engine = new OptionsBacktestEngine(INITIAL_CAPITAL, COMMISSION);

        var strategy = new ProtectivePutStrategy(10, new BigDecimal("3"), 30, 0, new BigDecimal("1.5"));
        OptionsBacktestResult result = engine.run(strategy, data);

        assertNotNull(result);
        assertTrue(result.getTotalTrades() > 0, "Should open trades in bear market");
    }

    // ======================== SIDEWAYS MARKET SCENARIO ========================

    @Test
    void sidewaysMarketIronCondor() {
        List<PriceData> data = generateSidewaysMarket(250);
        var engine = new OptionsBacktestEngine(INITIAL_CAPITAL, COMMISSION);

        var strategy = new IronCondorStrategy(10, new BigDecimal("5"), new BigDecimal("3"), 30, 0);
        OptionsBacktestResult result = engine.run(strategy, data);

        assertNotNull(result);
        assertTrue(result.getTotalTrades() > 0, "Iron condor should trade in sideways market");
    }

    // ======================== VOLUME ANALYSIS INTEGRATION ========================

    @Test
    void volumeAnalysisIntegration() {
        List<PriceData> data = generateBullMarket(100);

        // Build volume profiles
        List<VolumeProfile> profiles = VolumeAnalyzer.buildProfiles("SPY", data, null);
        assertEquals(data.size(), profiles.size());

        // Compute volume indicators
        List<Long> obv = VolumeAnalyzer.computeOBV(data);
        assertEquals(data.size(), obv.size());

        List<BigDecimal> vwap = VolumeAnalyzer.computeVWAP(data);
        assertEquals(data.size(), vwap.size());

        List<BigDecimal> mfi = VolumeAnalyzer.computeMFI(data, 14);
        assertEquals(data.size(), mfi.size());

        // Summary should work
        String summary = VolumeAnalyzer.summarize(profiles);
        assertNotNull(summary);
        assertTrue(summary.contains("Volume Summary"));

        // Unusual volume detection
        List<VolumeProfile> unusual = VolumeAnalyzer.findUnusualVolume(profiles, new BigDecimal("1.5"));
        assertNotNull(unusual);
    }

    // ======================== UNIFIED MARKET DATA ========================

    @Test
    void unifiedMarketDataPipeline() {
        List<PriceData> data = generateBullMarket(30);

        // Build unified data for each bar
        for (int i = 0; i < data.size(); i++) {
            var unified = new UnifiedMarketData.Builder()
                    .symbol("SPY")
                    .date(data.get(i).date())
                    .priceData(data.get(i))
                    .historicalPrices(data, i)
                    .build();

            assertNotNull(unified);
            assertEquals("SPY", unified.getSymbol());
            assertEquals(data.get(i).close(), unified.getUnderlyingPrice());
            assertTrue(unified.hasVolumeProfile());
            assertFalse(unified.hasOptionsData());
        }
    }

    @Test
    void unifiedMarketDataWithOptionsChain() {
        List<PriceData> data = generateBullMarket(5);
        PriceData lastBar = data.getLast();

        // Create a mock options chain
        List<OptionContract> calls = List.of(
                makeOption(OptionType.CALL, "490", "15.00", 1000, 5000, true),
                makeOption(OptionType.CALL, "500", "8.00", 2000, 8000, false),
                makeOption(OptionType.CALL, "510", "3.00", 500, 3000, false)
        );
        List<OptionContract> puts = List.of(
                makeOption(OptionType.PUT, "490", "2.00", 800, 4000, false),
                makeOption(OptionType.PUT, "500", "6.00", 1500, 6000, true),
                makeOption(OptionType.PUT, "510", "12.00", 600, 3500, true)
        );

        var chain = new OptionsChain("SPY", lastBar.close(), lastBar.date(),
                List.of("2024-02-16"), calls, puts);

        var unified = new UnifiedMarketData.Builder()
                .symbol("SPY")
                .date(lastBar.date())
                .priceData(lastBar)
                .optionsChain(chain)
                .historicalPrices(data, data.size() - 1)
                .build();

        assertTrue(unified.hasOptionsData());
        assertTrue(unified.hasVolumeProfile());

        VolumeProfile vp = unified.getVolumeProfile();
        assertTrue(vp.callVolume() > 0);
        assertTrue(vp.putVolume() > 0);
        assertTrue(vp.callOpenInterest() > 0);
        assertTrue(vp.putOpenInterest() > 0);
    }

    // ======================== OPTIONS CHAIN FUNCTIONALITY ========================

    @Test
    void optionsChainAnalytics() {
        var chain = makeTestOptionsChain();

        // Volume ratios
        assertTrue(chain.getTotalCallVolume() > 0);
        assertTrue(chain.getTotalPutVolume() > 0);
        assertTrue(chain.getPutCallVolumeRatio().compareTo(BigDecimal.ZERO) > 0);

        // Open interest
        assertTrue(chain.getTotalCallOpenInterest() > 0);
        assertTrue(chain.getTotalPutOpenInterest() > 0);

        // Strike operations
        List<BigDecimal> strikes = chain.getStrikesForExpiration("2024-02-16");
        assertFalse(strikes.isEmpty());

        // ATM option finding
        var atm = chain.findATMOption(OptionType.CALL, "2024-02-16");
        assertTrue(atm.isPresent());

        // Max pain
        BigDecimal maxPain = chain.getMaxPainStrike("2024-02-16");
        assertTrue(maxPain.compareTo(BigDecimal.ZERO) > 0);
    }

    // ======================== OPTIONS BACKTEST REPORT ========================

    @Test
    void reportFormatterDoesNotThrow() {
        List<PriceData> data = generateBullMarket(100);
        var engine = new OptionsBacktestEngine(INITIAL_CAPITAL, COMMISSION);

        var strategy = new CoveredCallStrategy(10, new BigDecimal("5"), 30, 0);
        OptionsBacktestResult result = engine.run(strategy, data);

        // Just call directly (no assertDoesNotThrow needed)
        OptionsReportFormatter.printReport(result, data);
    }

    // ======================== MULTI-LEG STRATEGIES ========================

    @Test
    void straddleMultiLegTracking() {
        List<PriceData> data = generateSidewaysMarket(100);
        var engine = new OptionsBacktestEngine(INITIAL_CAPITAL, COMMISSION);
        var strategy = new StraddleStrategy(10, 30, 0, new BigDecimal("50"), new BigDecimal("0.10"));

        OptionsBacktestResult result = engine.run(strategy, data);
        assertNotNull(result);

        // Straddles should produce both call and put trades
        if (result.getTotalTrades() > 0) {
            assertTrue(result.getTotalCallTrades() > 0 || result.getTotalPutTrades() > 0);
        }
    }

    @Test
    void ironCondorFourLegTracking() {
        List<PriceData> data = generateSidewaysMarket(100);
        var engine = new OptionsBacktestEngine(INITIAL_CAPITAL, COMMISSION);
        var strategy = new IronCondorStrategy(10, new BigDecimal("5"), new BigDecimal("3"), 30, 0);

        OptionsBacktestResult result = engine.run(strategy, data);
        assertNotNull(result);

        // Iron condor should produce both call and put trades
        if (result.getTotalTrades() > 0) {
            assertTrue(result.getTotalCallTrades() > 0, "Should have call legs");
            assertTrue(result.getTotalPutTrades() > 0, "Should have put legs");
        }
    }

    // ======================== OPTION TRADE LIFECYCLE ========================

    @Test
    void optionTradeFullLifecycle() {
        // Long call that expires ITM
        var longCall = new OptionTrade(OptionType.CALL, Trade.Direction.LONG,
                new BigDecimal("490"), "2024-02-16", "2024-01-15",
                new BigDecimal("15.00"), 2, 5000);

        assertEquals(OptionTrade.Status.OPEN, longCall.getStatus());
        assertEquals(5000, longCall.getEntryVolume());

        longCall.closeAtExpiration(new BigDecimal("510"));
        assertEquals(OptionTrade.Status.CLOSED_ASSIGNED, longCall.getStatus());
        assertTrue(longCall.isWin());

        // Short put that expires OTM (keep premium)
        var shortPut = new OptionTrade(OptionType.PUT, Trade.Direction.SHORT,
                new BigDecimal("480"), "2024-02-16", "2024-01-15",
                new BigDecimal("3.50"), 1, 3000);

        shortPut.closeAtExpiration(new BigDecimal("500"));
        assertEquals(OptionTrade.Status.CLOSED_EXPIRED, shortPut.getStatus());
        assertTrue(shortPut.isWin()); // short option expired worthless = win
    }

    // ======================== GREEKS ========================

    @Test
    void greeksCreation() {
        var greeks = new Greeks(
                new BigDecimal("0.55"), new BigDecimal("0.03"),
                new BigDecimal("-0.05"), new BigDecimal("0.15"),
                new BigDecimal("0.02"), new BigDecimal("0.25"));

        assertEquals(0, new BigDecimal("0.55").compareTo(greeks.delta()));
        assertEquals(0, new BigDecimal("-0.05").compareTo(greeks.theta()));
        assertEquals(0, new BigDecimal("0.25").compareTo(greeks.impliedVolatility()));
    }

    // ======================== HELPER METHODS ========================

    private static List<PriceData> generateBullMarket(int days) {
        List<PriceData> data = new ArrayList<>();
        double price = 450;
        for (int i = 0; i < days; i++) {
            price *= (1 + 0.0004 + Math.sin(i * 0.1) * 0.002);
            BigDecimal p = BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP);
            BigDecimal h = p.add(new BigDecimal("2"));
            BigDecimal l = p.subtract(new BigDecimal("2"));
            long vol = 50000000 + (long) (Math.random() * 20000000);
            data.add(new PriceData("2024-%02d-%02d".formatted(
                    1 + (i / 28), 1 + (i % 28)), p, h, l, p, p, vol));
        }
        return data;
    }

    private static List<PriceData> generateBearMarket(int days) {
        List<PriceData> data = new ArrayList<>();
        double price = 500;
        for (int i = 0; i < days; i++) {
            price *= (1 - 0.0005 + Math.sin(i * 0.1) * 0.002);
            BigDecimal p = BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP);
            BigDecimal h = p.add(new BigDecimal("2"));
            BigDecimal l = p.subtract(new BigDecimal("2"));
            long vol = 60000000 + (long) (Math.random() * 30000000);
            data.add(new PriceData("2024-%02d-%02d".formatted(
                    1 + (i / 28), 1 + (i % 28)), p, h, l, p, p, vol));
        }
        return data;
    }

    private static List<PriceData> generateSidewaysMarket(int days) {
        List<PriceData> data = new ArrayList<>();
        double price = 500;
        for (int i = 0; i < days; i++) {
            price = 500 + Math.sin(i * 0.15) * 5;
            BigDecimal p = BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP);
            BigDecimal h = p.add(new BigDecimal("1"));
            BigDecimal l = p.subtract(new BigDecimal("1"));
            long vol = 50000000 + (long) (Math.random() * 10000000);
            data.add(new PriceData("2024-%02d-%02d".formatted(
                    1 + (i / 28), 1 + (i % 28)), p, h, l, p, p, vol));
        }
        return data;
    }

    private static OptionsChain makeTestOptionsChain() {
        List<OptionContract> calls = List.of(
                makeOption(OptionType.CALL, "490", "15.00", 1000, 5000, true),
                makeOption(OptionType.CALL, "500", "8.00", 2000, 8000, false),
                makeOption(OptionType.CALL, "510", "3.00", 500, 3000, false)
        );
        List<OptionContract> puts = List.of(
                makeOption(OptionType.PUT, "490", "2.00", 800, 4000, false),
                makeOption(OptionType.PUT, "500", "6.00", 1500, 6000, true),
                makeOption(OptionType.PUT, "510", "12.00", 600, 3500, true)
        );
        return new OptionsChain("SPY", new BigDecimal("500"), "2024-01-15",
                List.of("2024-02-16"), calls, puts);
    }

    private static OptionContract makeOption(OptionType type, String strike, String lastPrice,
                                              long volume, long oi, boolean itm) {
        return new OptionContract(
                "SPY240216" + (type == OptionType.CALL ? "C" : "P") + strike,
                "SPY", type,
                new BigDecimal(strike), "2024-02-16",
                new BigDecimal(lastPrice),
                new BigDecimal(lastPrice).subtract(new BigDecimal("0.50")),
                new BigDecimal(lastPrice).add(new BigDecimal("0.50")),
                volume, oi, Greeks.zero(), itm);
    }
}
