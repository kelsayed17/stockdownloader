package com.stockdownloader.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VolumeProfileTest {

    @Test
    void createVolumeProfile() {
        var profile = makeProfile(1000000, 5000, 3000, 20000, 15000, "1000000");
        assertEquals("2024-01-15", profile.date());
        assertEquals("SPY", profile.symbol());
        assertEquals(1000000, profile.equityVolume());
    }

    @Test
    void totalOptionsVolume() {
        var profile = makeProfile(1000000, 5000, 3000, 20000, 15000, "1000000");
        assertEquals(8000, profile.getTotalOptionsVolume());
    }

    @Test
    void totalOpenInterest() {
        var profile = makeProfile(1000000, 5000, 3000, 20000, 15000, "1000000");
        assertEquals(35000, profile.getTotalOpenInterest());
    }

    @Test
    void putCallVolumeRatio() {
        var profile = makeProfile(1000000, 5000, 3000, 20000, 15000, "1000000");
        // 3000 / 5000 = 0.6
        BigDecimal ratio = profile.getPutCallVolumeRatio();
        assertEquals(0, new BigDecimal("0.6000").compareTo(ratio));
    }

    @Test
    void putCallVolumeRatioZeroCalls() {
        var profile = makeProfile(1000000, 0, 3000, 0, 15000, "1000000");
        assertEquals(0, BigDecimal.ZERO.compareTo(profile.getPutCallVolumeRatio()));
    }

    @Test
    void putCallOIRatio() {
        var profile = makeProfile(1000000, 5000, 3000, 20000, 15000, "1000000");
        // 15000 / 20000 = 0.75
        BigDecimal ratio = profile.getPutCallOIRatio();
        assertEquals(0, new BigDecimal("0.7500").compareTo(ratio));
    }

    @Test
    void relativeVolume() {
        var profile = makeProfile(1500000, 5000, 3000, 20000, 15000, "1000000");
        // 1500000 / 1000000 = 1.5
        BigDecimal rvol = profile.getRelativeVolume();
        assertEquals(0, new BigDecimal("1.5000").compareTo(rvol));
    }

    @Test
    void isHighVolumeTrue() {
        var profile = makeProfile(2000000, 5000, 3000, 20000, 15000, "1000000");
        assertTrue(profile.isHighVolume());
    }

    @Test
    void isHighVolumeFalse() {
        var profile = makeProfile(800000, 5000, 3000, 20000, 15000, "1000000");
        assertFalse(profile.isHighVolume());
    }

    @Test
    void computeAvgVolumeFromPriceData() {
        List<PriceData> data = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            BigDecimal p = BigDecimal.valueOf(100);
            data.add(new PriceData("day" + i, p, p, p, p, p, 1000));
        }
        BigDecimal avg = VolumeProfile.computeAvgVolume(data, 19, 20);
        assertEquals(0, new BigDecimal("1000").compareTo(avg));
    }

    @Test
    void computeAvgVolumeInsufficientData() {
        List<PriceData> data = new ArrayList<>();
        BigDecimal p = BigDecimal.valueOf(100);
        data.add(new PriceData("day0", p, p, p, p, p, 1000));
        BigDecimal avg = VolumeProfile.computeAvgVolume(data, 0, 20);
        assertEquals(0, BigDecimal.ZERO.compareTo(avg));
    }

    @Test
    void toStringFormatsCorrectly() {
        var profile = makeProfile(1000000, 5000, 3000, 20000, 15000, "1000000");
        String str = profile.toString();
        assertTrue(str.contains("SPY"));
        assertTrue(str.contains("2024-01-15"));
    }

    private static VolumeProfile makeProfile(long eqVol, long callVol, long putVol,
                                              long callOI, long putOI, String avgVol) {
        return new VolumeProfile("2024-01-15", "SPY", eqVol,
                callVol, putVol, callOI, putOI, new BigDecimal(avgVol));
    }
}
