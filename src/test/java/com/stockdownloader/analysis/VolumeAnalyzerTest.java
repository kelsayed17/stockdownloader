package com.stockdownloader.analysis;

import com.stockdownloader.model.PriceData;
import com.stockdownloader.model.VolumeProfile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VolumeAnalyzerTest {

    @Test
    void buildProfilesMatchesDataSize() {
        List<PriceData> data = generatePrices(30, 100, 10000);
        List<VolumeProfile> profiles = VolumeAnalyzer.buildProfiles("SPY", data, null);
        assertEquals(data.size(), profiles.size());
    }

    @Test
    void buildProfilesWithSymbol() {
        List<PriceData> data = generatePrices(5, 100, 10000);
        List<VolumeProfile> profiles = VolumeAnalyzer.buildProfiles("AAPL", data, null);
        assertEquals("AAPL", profiles.getFirst().symbol());
    }

    @Test
    void findUnusualVolume() {
        List<PriceData> data = new ArrayList<>();
        // 19 normal days
        for (int i = 0; i < 19; i++) {
            data.add(makePrice("day" + i, 100, 10000));
        }
        // 1 high volume day
        data.add(makePrice("day19", 100, 30000));

        List<VolumeProfile> profiles = VolumeAnalyzer.buildProfiles("SPY", data, null);
        List<VolumeProfile> unusual = VolumeAnalyzer.findUnusualVolume(profiles, new BigDecimal("2.0"));

        assertTrue(unusual.size() >= 1, "Should detect high volume day");
    }

    @Test
    void computeOBV() {
        List<PriceData> data = new ArrayList<>();
        data.add(makePrice("d1", 100, 1000));
        data.add(makePrice("d2", 105, 2000)); // up day, +2000
        data.add(makePrice("d3", 103, 1500)); // down day, -1500
        data.add(makePrice("d4", 103, 1000)); // flat day, 0
        data.add(makePrice("d5", 110, 3000)); // up day, +3000

        List<Long> obv = VolumeAnalyzer.computeOBV(data);
        assertEquals(5, obv.size());
        assertEquals(1000, (long) obv.get(0));     // initial
        assertEquals(3000, (long) obv.get(1));     // +2000
        assertEquals(1500, (long) obv.get(2));     // -1500
        assertEquals(1500, (long) obv.get(3));     // flat
        assertEquals(4500, (long) obv.get(4));     // +3000
    }

    @Test
    void computeOBVEmpty() {
        List<Long> obv = VolumeAnalyzer.computeOBV(List.of());
        assertTrue(obv.isEmpty());
    }

    @Test
    void computeVWAP() {
        List<PriceData> data = new ArrayList<>();
        data.add(new PriceData("d1", new BigDecimal("100"), new BigDecimal("102"),
                new BigDecimal("98"), new BigDecimal("101"), new BigDecimal("101"), 5000));
        data.add(new PriceData("d2", new BigDecimal("101"), new BigDecimal("105"),
                new BigDecimal("100"), new BigDecimal("104"), new BigDecimal("104"), 3000));

        List<BigDecimal> vwap = VolumeAnalyzer.computeVWAP(data);
        assertEquals(2, vwap.size());
        assertTrue(vwap.get(0).compareTo(BigDecimal.ZERO) > 0);
        assertTrue(vwap.get(1).compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void computeMFI() {
        List<PriceData> data = generatePrices(20, 100, 10000);
        List<BigDecimal> mfi = VolumeAnalyzer.computeMFI(data, 14);
        assertEquals(20, mfi.size());

        // During warmup, MFI should be 50 (neutral)
        assertEquals(0, new BigDecimal("50").compareTo(mfi.get(0)));

        // After warmup, MFI should be between 0 and 100
        for (int i = 14; i < mfi.size(); i++) {
            assertTrue(mfi.get(i).compareTo(BigDecimal.ZERO) >= 0);
            assertTrue(mfi.get(i).compareTo(new BigDecimal("101")) < 0);
        }
    }

    @Test
    void summarizeProfiles() {
        List<PriceData> data = generatePrices(30, 100, 10000);
        List<VolumeProfile> profiles = VolumeAnalyzer.buildProfiles("SPY", data, null);
        String summary = VolumeAnalyzer.summarize(profiles);

        assertTrue(summary.contains("Volume Summary"));
        assertTrue(summary.contains("30 days"));
        assertTrue(summary.contains("Total Equity Volume"));
    }

    @Test
    void summarizeEmptyProfiles() {
        String summary = VolumeAnalyzer.summarize(List.of());
        assertEquals("No volume data available", summary);
    }

    private static List<PriceData> generatePrices(int count, double startPrice, long volume) {
        List<PriceData> data = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double price = startPrice + i * 0.5; // slight uptrend
            data.add(makePrice("day" + i, price, volume));
        }
        return data;
    }

    private static PriceData makePrice(String date, double close, long volume) {
        BigDecimal c = BigDecimal.valueOf(close);
        BigDecimal h = c.add(BigDecimal.ONE);
        BigDecimal l = c.subtract(BigDecimal.ONE);
        return new PriceData(date, c, h, l, c, c, volume);
    }
}
