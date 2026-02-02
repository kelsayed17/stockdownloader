package com.stockdownloader.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UnifiedMarketDataTest {

    @Test
    void buildWithPriceDataOnly() {
        PriceData price = makePrice("2024-01-15", 500, 10000);
        var unified = new UnifiedMarketData.Builder()
                .symbol("SPY")
                .date("2024-01-15")
                .priceData(price)
                .build();

        assertEquals("SPY", unified.getSymbol());
        assertEquals("2024-01-15", unified.getDate());
        assertEquals(0, new BigDecimal("500").compareTo(unified.getUnderlyingPrice()));
        assertFalse(unified.hasOptionsData());
        assertTrue(unified.hasVolumeProfile());
    }

    @Test
    void buildWithAllData() {
        PriceData price = makePrice("2024-01-15", 500, 10000);
        var chain = new OptionsChain("SPY", new BigDecimal("500"), "2024-01-15",
                List.of("2024-02-16"),
                List.of(makeContract(OptionType.CALL, "510", 100, 500)),
                List.of(makeContract(OptionType.PUT, "490", 80, 400)));

        List<PriceData> history = makePriceHistory(25, 500, 10000);

        var unified = new UnifiedMarketData.Builder()
                .symbol("SPY")
                .date("2024-01-15")
                .priceData(price)
                .optionsChain(chain)
                .historicalPrices(history, 24)
                .build();

        assertTrue(unified.hasOptionsData());
        assertTrue(unified.hasVolumeProfile());

        VolumeProfile vp = unified.getVolumeProfile();
        assertEquals(10000, vp.equityVolume());
        assertEquals(100, vp.callVolume());
        assertEquals(80, vp.putVolume());
        assertEquals(500, vp.callOpenInterest());
        assertEquals(400, vp.putOpenInterest());
    }

    @Test
    void volumeProfileComputesAverage() {
        List<PriceData> history = makePriceHistory(25, 500, 10000);
        PriceData price = makePrice("2024-01-15", 500, 10000);

        var unified = new UnifiedMarketData.Builder()
                .symbol("SPY")
                .date("2024-01-15")
                .priceData(price)
                .historicalPrices(history, 24)
                .build();

        VolumeProfile vp = unified.getVolumeProfile();
        assertEquals(0, new BigDecimal("10000").compareTo(vp.averageEquityVolume20d()));
    }

    @Test
    void nullSymbolThrows() {
        PriceData price = makePrice("2024-01-15", 500, 10000);
        assertThrows(NullPointerException.class, () -> {
            new UnifiedMarketData.Builder()
                    .date("2024-01-15")
                    .priceData(price)
                    .build();
        });
    }

    @Test
    void toStringFormatsCorrectly() {
        PriceData price = makePrice("2024-01-15", 500, 10000);
        var unified = new UnifiedMarketData.Builder()
                .symbol("SPY")
                .date("2024-01-15")
                .priceData(price)
                .build();
        String str = unified.toString();
        assertTrue(str.contains("SPY"));
        assertTrue(str.contains("500"));
    }

    private static PriceData makePrice(String date, double close, long volume) {
        BigDecimal p = BigDecimal.valueOf(close);
        return new PriceData(date, p, p, p, p, p, volume);
    }

    private static List<PriceData> makePriceHistory(int count, double close, long volume) {
        List<PriceData> data = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            data.add(makePrice("day" + i, close, volume));
        }
        return data;
    }

    private static OptionContract makeContract(OptionType type, String strike, long volume, long oi) {
        return new OptionContract("SYM", "SPY", type,
                new BigDecimal(strike), "2024-02-16",
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                volume, oi, Greeks.zero(), false);
    }
}
