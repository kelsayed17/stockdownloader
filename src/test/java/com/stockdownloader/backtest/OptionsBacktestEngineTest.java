package com.stockdownloader.backtest;

import com.stockdownloader.model.OptionType;
import com.stockdownloader.model.PriceData;
import com.stockdownloader.strategy.CoveredCallStrategy;
import com.stockdownloader.strategy.OptionsStrategy;
import com.stockdownloader.strategy.ProtectivePutStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OptionsBacktestEngineTest {

    private static final BigDecimal CAPITAL = new BigDecimal("100000");
    private static final BigDecimal NO_COMMISSION = BigDecimal.ZERO;

    @Test
    void rejectsNullStrategy() {
        var engine = new OptionsBacktestEngine(CAPITAL, NO_COMMISSION);
        assertThrows(NullPointerException.class, () -> engine.run(null, List.of()));
    }

    @Test
    void rejectsNullData() {
        var engine = new OptionsBacktestEngine(CAPITAL, NO_COMMISSION);
        assertThrows(IllegalArgumentException.class, () ->
                engine.run(new CoveredCallStrategy(), null));
    }

    @Test
    void rejectsEmptyData() {
        var engine = new OptionsBacktestEngine(CAPITAL, NO_COMMISSION);
        assertThrows(IllegalArgumentException.class, () ->
                engine.run(new CoveredCallStrategy(), List.of()));
    }

    @Test
    void runsWithUptrend() {
        var engine = new OptionsBacktestEngine(CAPITAL, NO_COMMISSION);
        var strategy = new CoveredCallStrategy(5, new BigDecimal("0.05"), 10, new BigDecimal("0.03"));
        List<PriceData> data = generateUptrend(50);

        OptionsBacktestResult result = engine.run(strategy, data);

        assertNotNull(result);
        assertEquals(strategy.getName(), result.getStrategyName());
        assertEquals(CAPITAL, result.getInitialCapital());
        assertNotNull(result.getEquityCurve());
        assertEquals(data.size(), result.getEquityCurve().size());
    }

    @Test
    void runsWithDowntrend() {
        var engine = new OptionsBacktestEngine(CAPITAL, NO_COMMISSION);
        var strategy = new ProtectivePutStrategy(5, new BigDecimal("0.05"), 10, 3);
        List<PriceData> data = generateDowntrend(50);

        OptionsBacktestResult result = engine.run(strategy, data);

        assertNotNull(result);
        assertEquals(data.getFirst().date(), result.getStartDate());
        assertEquals(data.getLast().date(), result.getEndDate());
    }

    @Test
    void coveredCallInFlatMarket() {
        var engine = new OptionsBacktestEngine(CAPITAL, NO_COMMISSION);
        var strategy = new CoveredCallStrategy(5, new BigDecimal("0.05"), 10, new BigDecimal("0.03"));
        List<PriceData> data = generateFlatWithCross(60);

        OptionsBacktestResult result = engine.run(strategy, data);
        assertNotNull(result);
        // In flat market, covered calls should collect premium
        assertNotNull(result.getFinalCapital());
    }

    @Test
    void commissionIsDeducted() {
        BigDecimal commission = new BigDecimal("5.00");
        var engine = new OptionsBacktestEngine(CAPITAL, commission);
        var strategy = new CoveredCallStrategy(5, new BigDecimal("0.05"), 10, new BigDecimal("0.03"));
        List<PriceData> data = generateFlatWithCross(60);

        OptionsBacktestResult resultWithComm = engine.run(strategy, data);

        var engineNoComm = new OptionsBacktestEngine(CAPITAL, NO_COMMISSION);
        OptionsBacktestResult resultNoComm = engineNoComm.run(strategy, data);

        // With commission, final capital should be less than or equal
        assertTrue(resultWithComm.getFinalCapital().compareTo(resultNoComm.getFinalCapital()) <= 0,
                "Commission should reduce final capital");
    }

    @Test
    void equityCurveSizeMatchesDataSize() {
        var engine = new OptionsBacktestEngine(CAPITAL, NO_COMMISSION);
        var strategy = new CoveredCallStrategy(5, new BigDecimal("0.05"), 10, new BigDecimal("0.03"));
        List<PriceData> data = generateUptrend(30);

        OptionsBacktestResult result = engine.run(strategy, data);
        assertEquals(data.size(), result.getEquityCurve().size());
    }

    @Test
    void volumeIsCapturedInTrades() {
        var engine = new OptionsBacktestEngine(CAPITAL, NO_COMMISSION);
        var strategy = new CoveredCallStrategy(5, new BigDecimal("0.05"), 10, new BigDecimal("0.03"));
        List<PriceData> data = generateFlatWithCross(60);

        OptionsBacktestResult result = engine.run(strategy, data);

        if (!result.getTrades().isEmpty()) {
            // Volume should be captured from the bar where the trade was entered
            assertTrue(result.getTotalVolumeTraded() > 0,
                    "Volume should be tracked for all trades");
            for (var trade : result.getTrades()) {
                assertTrue(trade.getEntryVolume() > 0,
                        "Each trade should capture entry volume");
            }
        }
    }

    @Test
    void handlesExpirationCorrectly() {
        var engine = new OptionsBacktestEngine(CAPITAL, NO_COMMISSION);
        // Use very short DTE to trigger expiration within the data window
        var strategy = new CoveredCallStrategy(3, new BigDecimal("0.05"), 5, new BigDecimal("0.10"));
        List<PriceData> data = generateFlatWithCross(40);

        OptionsBacktestResult result = engine.run(strategy, data);
        // Should complete without error
        assertNotNull(result.getFinalCapital());
    }

    @Test
    void protectivePutInDowntrend() {
        var engine = new OptionsBacktestEngine(CAPITAL, NO_COMMISSION);
        var strategy = new ProtectivePutStrategy(5, new BigDecimal("0.05"), 10, 3);
        List<PriceData> data = generateDowntrend(50);

        OptionsBacktestResult result = engine.run(strategy, data);
        assertNotNull(result);
        // Protective puts should provide some hedge value in downtrend
        assertNotNull(result.getFinalCapital());
    }

    @Test
    void customRiskFreeRate() {
        var engine1 = new OptionsBacktestEngine(CAPITAL, NO_COMMISSION, new BigDecimal("0.01"));
        var engine2 = new OptionsBacktestEngine(CAPITAL, NO_COMMISSION, new BigDecimal("0.10"));
        var strategy = new CoveredCallStrategy(5, new BigDecimal("0.05"), 10, new BigDecimal("0.03"));
        List<PriceData> data = generateUptrend(30);

        // Both should run without error
        assertNotNull(engine1.run(strategy, data));
        assertNotNull(engine2.run(strategy, data));
    }

    // --- Helper methods ---

    private List<PriceData> generateUptrend(int days) {
        List<PriceData> data = new ArrayList<>();
        double price = 100;
        for (int i = 0; i < days; i++) {
            price += 0.5 + Math.sin(i * 0.5) * 0.3;
            data.add(makePrice("2024-01-%02d".formatted((i % 28) + 1), price));
        }
        return data;
    }

    private List<PriceData> generateDowntrend(int days) {
        List<PriceData> data = new ArrayList<>();
        double price = 100;
        for (int i = 0; i < days; i++) {
            price -= 0.3 + Math.sin(i * 0.5) * 0.2;
            price = Math.max(price, 50); // floor
            data.add(makePrice("2024-01-%02d".formatted((i % 28) + 1), price));
        }
        return data;
    }

    private List<PriceData> generateFlatWithCross(int days) {
        List<PriceData> data = new ArrayList<>();
        double price = 100;
        for (int i = 0; i < days; i++) {
            // Oscillate around 100 to trigger MA crossovers
            price = 100 + Math.sin(i * 0.3) * 5;
            data.add(makePrice("2024-01-%02d".formatted((i % 28) + 1), price));
        }
        return data;
    }

    private PriceData makePrice(String date, double price) {
        BigDecimal p = BigDecimal.valueOf(price);
        BigDecimal high = p.add(new BigDecimal("1.50"));
        BigDecimal low = p.subtract(new BigDecimal("1.50"));
        return new PriceData(date, p, high, low, p, p, 500000);
    }
}
