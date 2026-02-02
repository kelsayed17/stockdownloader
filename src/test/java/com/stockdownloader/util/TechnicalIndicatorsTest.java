package com.stockdownloader.util;

import com.stockdownloader.model.PriceData;
import com.stockdownloader.util.TechnicalIndicators.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for all technical indicators in TechnicalIndicators.
 */
class TechnicalIndicatorsTest {

    private List<PriceData> data;

    @BeforeEach
    void setUp() {
        data = generateTestData(300);
    }

    // =========================================================================
    // Bollinger Bands
    // =========================================================================

    @Test
    void bollingerBands_returnsValidBands() {
        BollingerBands bb = TechnicalIndicators.bollingerBands(data, 50, 20, 2.0);
        assertTrue(bb.upper().compareTo(bb.middle()) > 0, "Upper band should be above middle");
        assertTrue(bb.middle().compareTo(bb.lower()) > 0, "Middle band should be above lower");
        assertTrue(bb.width().compareTo(BigDecimal.ZERO) > 0, "Width should be positive");
    }

    @Test
    void bollingerBands_returnsZeroForInsufficientData() {
        BollingerBands bb = TechnicalIndicators.bollingerBands(data, 5, 20, 2.0);
        assertEquals(0, bb.upper().compareTo(BigDecimal.ZERO));
    }

    @Test
    void bollingerPercentB_withinExpectedRange() {
        BigDecimal pctB = TechnicalIndicators.bollingerPercentB(data, 50, 20);
        // %B should be between -1 and 2 for most cases
        assertTrue(pctB.doubleValue() >= -2 && pctB.doubleValue() <= 3);
    }

    // =========================================================================
    // Stochastic
    // =========================================================================

    @Test
    void stochastic_returnsValidValues() {
        Stochastic stoch = TechnicalIndicators.stochastic(data, 30, 14, 3);
        assertTrue(stoch.percentK().doubleValue() >= 0 && stoch.percentK().doubleValue() <= 100,
                "%%K should be between 0 and 100, got " + stoch.percentK());
        assertTrue(stoch.percentD().doubleValue() >= 0 && stoch.percentD().doubleValue() <= 100,
                "%%D should be between 0 and 100, got " + stoch.percentD());
    }

    @Test
    void stochastic_returnsZeroForInsufficientData() {
        Stochastic stoch = TechnicalIndicators.stochastic(data, 5, 14, 3);
        assertEquals(0, stoch.percentK().compareTo(BigDecimal.ZERO));
    }

    // =========================================================================
    // ATR
    // =========================================================================

    @Test
    void atr_returnsPositiveValue() {
        BigDecimal atr = TechnicalIndicators.atr(data, 30, 14);
        assertTrue(atr.compareTo(BigDecimal.ZERO) > 0, "ATR should be positive");
    }

    @Test
    void trueRange_computesCorrectly() {
        BigDecimal tr = TechnicalIndicators.trueRange(data, 10);
        assertTrue(tr.compareTo(BigDecimal.ZERO) >= 0, "True range should be non-negative");
    }

    // =========================================================================
    // OBV
    // =========================================================================

    @Test
    void obv_computesWithoutError() {
        BigDecimal obvValue = TechnicalIndicators.obv(data, 50);
        assertNotNull(obvValue);
    }

    @Test
    void isOBVRising_returnsBoolean() {
        // Just ensure it doesn't throw
        boolean result = TechnicalIndicators.isOBVRising(data, 50, 5);
        assertNotNull(result);
    }

    // =========================================================================
    // ADX
    // =========================================================================

    @Test
    void adx_returnsValidResult() {
        ADXResult result = TechnicalIndicators.adx(data, 60, 14);
        assertTrue(result.adx().compareTo(BigDecimal.ZERO) >= 0, "ADX should be non-negative");
        assertTrue(result.plusDI().compareTo(BigDecimal.ZERO) >= 0, "+DI should be non-negative");
        assertTrue(result.minusDI().compareTo(BigDecimal.ZERO) >= 0, "-DI should be non-negative");
    }

    // =========================================================================
    // Parabolic SAR
    // =========================================================================

    @Test
    void parabolicSAR_returnsPositiveValue() {
        BigDecimal sar = TechnicalIndicators.parabolicSAR(data, 50);
        assertTrue(sar.compareTo(BigDecimal.ZERO) > 0, "SAR should be positive");
    }

    @Test
    void isSARBullish_returnsBoolean() {
        boolean result = TechnicalIndicators.isSARBullish(data, 50);
        assertNotNull(result);
    }

    // =========================================================================
    // Williams %R
    // =========================================================================

    @Test
    void williamsR_withinExpectedRange() {
        BigDecimal willR = TechnicalIndicators.williamsR(data, 30, 14);
        assertTrue(willR.doubleValue() >= -100 && willR.doubleValue() <= 0,
                "Williams %%R should be between -100 and 0, got " + willR);
    }

    // =========================================================================
    // CCI
    // =========================================================================

    @Test
    void cci_computesWithoutError() {
        BigDecimal cci = TechnicalIndicators.cci(data, 40, 20);
        assertNotNull(cci);
    }

    // =========================================================================
    // VWAP
    // =========================================================================

    @Test
    void vwap_returnsPositiveValue() {
        BigDecimal vwap = TechnicalIndicators.vwap(data, 30, 20);
        assertTrue(vwap.compareTo(BigDecimal.ZERO) > 0, "VWAP should be positive");
    }

    // =========================================================================
    // Fibonacci
    // =========================================================================

    @Test
    void fibonacci_returnsOrderedLevels() {
        FibonacciLevels fib = TechnicalIndicators.fibonacciRetracement(data, 100, 50);
        assertTrue(fib.high().compareTo(fib.level236()) >= 0, "High should be >= 23.6% level");
        assertTrue(fib.level236().compareTo(fib.level382()) >= 0, "23.6% should be >= 38.2%");
        assertTrue(fib.level382().compareTo(fib.level500()) >= 0, "38.2% should be >= 50%");
        assertTrue(fib.level500().compareTo(fib.level618()) >= 0, "50% should be >= 61.8%");
        assertTrue(fib.level618().compareTo(fib.level786()) >= 0, "61.8% should be >= 78.6%");
        assertTrue(fib.level786().compareTo(fib.low()) >= 0, "78.6% should be >= low");
    }

    // =========================================================================
    // ROC
    // =========================================================================

    @Test
    void roc_computesWithoutError() {
        BigDecimal roc = TechnicalIndicators.roc(data, 30, 12);
        assertNotNull(roc);
    }

    @Test
    void roc_returnsZeroForInsufficientData() {
        BigDecimal roc = TechnicalIndicators.roc(data, 5, 12);
        assertEquals(0, roc.compareTo(BigDecimal.ZERO));
    }

    // =========================================================================
    // MFI
    // =========================================================================

    @Test
    void mfi_withinExpectedRange() {
        BigDecimal mfi = TechnicalIndicators.mfi(data, 30, 14);
        assertTrue(mfi.doubleValue() >= 0 && mfi.doubleValue() <= 100,
                "MFI should be between 0 and 100, got " + mfi);
    }

    // =========================================================================
    // Ichimoku
    // =========================================================================

    @Test
    void ichimoku_returnsValidResult() {
        IchimokuCloud ichi = TechnicalIndicators.ichimoku(data, 100);
        assertTrue(ichi.tenkanSen().compareTo(BigDecimal.ZERO) > 0, "Tenkan should be positive");
        assertTrue(ichi.kijunSen().compareTo(BigDecimal.ZERO) > 0, "Kijun should be positive");
        assertTrue(ichi.senkouSpanA().compareTo(BigDecimal.ZERO) > 0, "Senkou A should be positive");
        assertTrue(ichi.senkouSpanB().compareTo(BigDecimal.ZERO) > 0, "Senkou B should be positive");
    }

    @Test
    void ichimoku_returnsZeroForInsufficientData() {
        IchimokuCloud ichi = TechnicalIndicators.ichimoku(data, 30);
        assertEquals(0, ichi.tenkanSen().compareTo(BigDecimal.ZERO));
    }

    // =========================================================================
    // RSI
    // =========================================================================

    @Test
    void rsi_withinExpectedRange() {
        BigDecimal rsi = TechnicalIndicators.rsi(data, 30, 14);
        assertTrue(rsi.doubleValue() >= 0 && rsi.doubleValue() <= 100,
                "RSI should be between 0 and 100, got " + rsi);
    }

    // =========================================================================
    // MACD
    // =========================================================================

    @Test
    void macd_computesWithoutError() {
        BigDecimal macdLine = TechnicalIndicators.macdLine(data, 50, 12, 26);
        BigDecimal macdSignal = TechnicalIndicators.macdSignal(data, 50, 12, 26, 9);
        BigDecimal histogram = TechnicalIndicators.macdHistogram(data, 50, 12, 26, 9);
        assertNotNull(macdLine);
        assertNotNull(macdSignal);
        assertNotNull(histogram);
        assertEquals(macdLine.subtract(macdSignal).setScale(6, java.math.RoundingMode.HALF_UP),
                histogram.setScale(6, java.math.RoundingMode.HALF_UP));
    }

    // =========================================================================
    // Support & Resistance
    // =========================================================================

    @Test
    void supportResistance_returnsValidLevels() {
        SupportResistance sr = TechnicalIndicators.supportResistance(data, 200, 100, 5);
        assertNotNull(sr.supportLevels());
        assertNotNull(sr.resistanceLevels());
    }

    // =========================================================================
    // Average Volume
    // =========================================================================

    @Test
    void averageVolume_returnsPositiveValue() {
        BigDecimal avgVol = TechnicalIndicators.averageVolume(data, 30, 20);
        assertTrue(avgVol.compareTo(BigDecimal.ZERO) > 0, "Average volume should be positive");
    }

    // =========================================================================
    // Standard Deviation
    // =========================================================================

    @Test
    void standardDeviation_returnsPositiveValue() {
        BigDecimal stdDev = TechnicalIndicators.standardDeviation(data, 30, 20);
        assertTrue(stdDev.compareTo(BigDecimal.ZERO) > 0, "Std dev should be positive");
    }

    // =========================================================================
    // Test Data Generator
    // =========================================================================

    private static List<PriceData> generateTestData(int days) {
        List<PriceData> data = new ArrayList<>();
        double price = 100.0;

        for (int i = 0; i < days; i++) {
            // Simulate a mild uptrend with noise
            double change = (Math.random() - 0.48) * 3;
            price = Math.max(50, price + change);

            double open = price + (Math.random() - 0.5) * 2;
            double high = Math.max(open, price) + Math.random() * 2;
            double low = Math.min(open, price) - Math.random() * 2;
            double close = price;
            long volume = (long) (1_000_000 + Math.random() * 5_000_000);

            data.add(new PriceData(
                    "2020-01-%02d".formatted(Math.min(i + 1, 28)),
                    BigDecimal.valueOf(open),
                    BigDecimal.valueOf(high),
                    BigDecimal.valueOf(low),
                    BigDecimal.valueOf(close),
                    BigDecimal.valueOf(close),
                    volume));
        }
        return data;
    }
}
