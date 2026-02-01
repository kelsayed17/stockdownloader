package com.stockdownloader.integration;

import com.stockdownloader.data.CsvPriceDataLoader;
import com.stockdownloader.model.PriceData;
import com.stockdownloader.strategy.MACDStrategy;
import com.stockdownloader.strategy.RSIStrategy;
import com.stockdownloader.strategy.SMACrossoverStrategy;
import com.stockdownloader.strategy.TradingStrategy;
import com.stockdownloader.strategy.TradingStrategy.Signal;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test verifying that strategies produce correct signal sequences
 * when evaluated over real price data loaded from CSV.
 */
class StrategySignalIntegrationTest {

    private static List<PriceData> priceData;

    @BeforeAll
    static void loadTestData() {
        priceData = CsvPriceDataLoader.loadFromStream(
                StrategySignalIntegrationTest.class.getResourceAsStream("/test-price-data.csv"));
        assertFalse(priceData.isEmpty());
    }

    @Test
    void smaCrossoverRespectsWarmupPeriod() {
        var strategy = new SMACrossoverStrategy(20, 50);

        // During warmup period, all signals should be HOLD
        for (int i = 0; i < strategy.getWarmupPeriod(); i++) {
            assertEquals(Signal.HOLD, strategy.evaluate(priceData, i),
                    "Signal at index " + i + " should be HOLD during warmup");
        }
    }

    @Test
    void smaCrossoverGeneratesSignalsAfterWarmup() {
        var strategy = new SMACrossoverStrategy(20, 50);
        List<Signal> signals = collectSignals(strategy);

        // Should have HOLD signals during warmup and possibly BUY/SELL after
        long holdCount = signals.stream().filter(s -> s == Signal.HOLD).count();
        assertTrue(holdCount >= strategy.getWarmupPeriod(),
                "Should have at least warmup period number of HOLD signals");

        // Total signals should match data size
        assertEquals(priceData.size(), signals.size());
    }

    @Test
    void smaCrossoverBuyAlwaysPrecedesSell() {
        var strategy = new SMACrossoverStrategy(20, 50);
        List<Signal> signals = collectSignals(strategy);

        // Filter out HOLDs; the sequence of BUY/SELL should alternate starting with BUY
        List<Signal> actionSignals = signals.stream()
                .filter(s -> s != Signal.HOLD)
                .toList();

        if (!actionSignals.isEmpty()) {
            assertEquals(Signal.BUY, actionSignals.getFirst(),
                    "First action signal should be BUY");

            for (int i = 0; i < actionSignals.size() - 1; i++) {
                assertNotEquals(actionSignals.get(i), actionSignals.get(i + 1),
                        "BUY and SELL signals should alternate");
            }
        }
    }

    @Test
    void rsiRespectsWarmupPeriod() {
        var strategy = new RSIStrategy(14, 30, 70);

        for (int i = 0; i < strategy.getWarmupPeriod(); i++) {
            assertEquals(Signal.HOLD, strategy.evaluate(priceData, i),
                    "RSI signal at index " + i + " should be HOLD during warmup");
        }
    }

    @Test
    void rsiGeneratesValidSignals() {
        var strategy = new RSIStrategy(14, 30, 70);
        List<Signal> signals = collectSignals(strategy);

        assertEquals(priceData.size(), signals.size());

        // All signals must be one of BUY/SELL/HOLD
        for (Signal signal : signals) {
            assertNotNull(signal);
            assertTrue(signal == Signal.BUY || signal == Signal.SELL || signal == Signal.HOLD);
        }
    }

    @Test
    void rsiNarrowThresholdsGenerateMoreSignals() {
        var wideThresholds = new RSIStrategy(14, 30, 70);
        var narrowThresholds = new RSIStrategy(14, 40, 60);

        long wideSignals = collectSignals(wideThresholds).stream()
                .filter(s -> s != Signal.HOLD).count();
        long narrowSignals = collectSignals(narrowThresholds).stream()
                .filter(s -> s != Signal.HOLD).count();

        // Narrower thresholds should generally produce more or equal signals
        assertTrue(narrowSignals >= wideSignals,
                "Narrower RSI thresholds should produce more or equal trading signals");
    }

    @Test
    void macdRespectsWarmupPeriod() {
        var strategy = new MACDStrategy(12, 26, 9);
        assertEquals(35, strategy.getWarmupPeriod(), "MACD warmup should be slow + signal");

        for (int i = 0; i < strategy.getWarmupPeriod(); i++) {
            assertEquals(Signal.HOLD, strategy.evaluate(priceData, i),
                    "MACD signal at index " + i + " should be HOLD during warmup");
        }
    }

    @Test
    void macdGeneratesSignalsAfterWarmup() {
        var strategy = new MACDStrategy(12, 26, 9);
        List<Signal> signals = collectSignals(strategy);

        assertEquals(priceData.size(), signals.size());

        // After warmup, there should be some non-HOLD signals over a full year of data
        long actionSignals = signals.stream()
                .filter(s -> s != Signal.HOLD)
                .count();
        assertTrue(actionSignals > 0,
                "MACD should generate at least one trading signal over a year of data");
    }

    @Test
    void macdBuyAndSellSignalsAlternate() {
        var strategy = new MACDStrategy(12, 26, 9);
        List<Signal> signals = collectSignals(strategy);

        List<Signal> actionSignals = signals.stream()
                .filter(s -> s != Signal.HOLD)
                .toList();

        for (int i = 0; i < actionSignals.size() - 1; i++) {
            assertNotEquals(actionSignals.get(i), actionSignals.get(i + 1),
                    "MACD BUY/SELL signals should alternate at index " + i);
        }
    }

    @Test
    void differentSmaPeriodsDifferentSignals() {
        var shortPeriods = new SMACrossoverStrategy(5, 20);
        var longPeriods = new SMACrossoverStrategy(50, 200);

        List<Signal> shortSignals = collectSignals(shortPeriods);
        List<Signal> longSignals = collectSignals(longPeriods);

        // Short periods should generate more signals
        long shortActions = shortSignals.stream().filter(s -> s != Signal.HOLD).count();
        long longActions = longSignals.stream().filter(s -> s != Signal.HOLD).count();

        assertTrue(shortActions >= longActions,
                "Shorter SMA periods should generate more or equal trading signals");
    }

    private List<Signal> collectSignals(TradingStrategy strategy) {
        List<Signal> signals = new ArrayList<>(priceData.size());
        for (int i = 0; i < priceData.size(); i++) {
            signals.add(strategy.evaluate(priceData, i));
        }
        return signals;
    }
}
