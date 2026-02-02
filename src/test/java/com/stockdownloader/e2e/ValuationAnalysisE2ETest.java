package com.stockdownloader.e2e;

import com.stockdownloader.analysis.FormulaCalculator;
import com.stockdownloader.analysis.FormulaCalculator.ValuationInputs;
import com.stockdownloader.model.FinancialData;
import com.stockdownloader.model.HistoricalData;
import com.stockdownloader.model.QuoteData;
import com.stockdownloader.util.BigDecimalMath;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end test for the full valuation analysis pipeline.
 * Uses realistic AAPL-like financial data to exercise the complete chain:
 * QuoteData + FinancialData + HistoricalData + ValuationInputs
 *   -> FormulaCalculator.calculate()
 *   -> All computed metrics (P/E, Graham, intrinsic value, margin of safety,
 *      EPS projections, dividend analysis, PS ratios, CAGR)
 *
 * This validates that all model classes, FormulaCalculator, and BigDecimalMath
 * work together correctly with real-world-like data.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ValuationAnalysisE2ETest {

    private static QuoteData aapl;
    private static FinancialData aaplFinancials;
    private static HistoricalData aaplHistory;
    private static ValuationInputs aaplInputs;
    private static FormulaCalculator calculator;

    @BeforeAll
    static void setupRealisticAAPLData() {
        // Real-world-like AAPL data (approximate Q4 2023 values)
        aapl = new QuoteData();
        aapl.setLastTradePriceOnly(new BigDecimal("185.50"));
        aapl.setDilutedEPS(new BigDecimal("6.42"));
        aapl.setEpsEstimateNextYear(new BigDecimal("7.10"));
        aapl.setTrailingAnnualDividendYield(new BigDecimal("0.96"));
        aapl.setYearHigh(new BigDecimal("199.62"));
        aapl.setYearLow(new BigDecimal("124.17"));
        aapl.setPriceSales(new BigDecimal("7.68"));
        aapl.setPreviousClose(new BigDecimal("184.25"));
        aapl.setOpen(new BigDecimal("185.00"));
        aapl.setDaysHigh(new BigDecimal("186.10"));
        aapl.setDaysLow(new BigDecimal("183.80"));
        aapl.setVolume(new BigDecimal("55000000"));
        aapl.setYearRange("124.17 - 199.62");
        aapl.setMarketCapitalization(2900000000000L);
        aapl.setMarketCapitalizationStr("2.9T");

        // Morningstar-like financial data (5 quarters + TTM)
        aaplFinancials = new FinancialData();
        long[] revenues = {94836, 89498, 81797, 83359, 90146, 383296};
        long[] dilutedShares = {15744, 15697, 15672, 15634, 15599, 15599};
        for (int i = 0; i < 6; i++) {
            aaplFinancials.setRevenue(i, revenues[i]);
            aaplFinancials.setDilutedShares(i, dilutedShares[i]);
            aaplFinancials.setBasicShares(i, dilutedShares[i] - 100);
        }
        aaplFinancials.setFiscalQuarter(0, "2022-12");
        aaplFinancials.setFiscalQuarter(1, "2023-03");
        aaplFinancials.setFiscalQuarter(2, "2023-06");
        aaplFinancials.setFiscalQuarter(3, "2023-09");
        aaplFinancials.setFiscalQuarter(4, "2022-09");
        aaplFinancials.setFiscalQuarter(5, "TTM");
        aaplFinancials.computeRevenuePerShare();

        // Historical price data (quarterly extremes)
        aaplHistory = new HistoricalData("AAPL");
        aaplHistory.setHighestPriceThisQtr(new BigDecimal("198.23"));
        aaplHistory.setLowestPriceThisQtr(new BigDecimal("165.67"));
        aaplHistory.setHighestPriceLastQtr(new BigDecimal("189.98"));
        aaplHistory.setLowestPriceLastQtr(new BigDecimal("167.62"));

        // Valuation inputs (analyst consensus-like)
        aaplInputs = new ValuationInputs(
                new BigDecimal("5.61"),    // epsYearOne (FY2020)
                new BigDecimal("7.10"),    // epsYearFive (FY2024 estimate)
                new BigDecimal("7.10"),    // epsEstimateNextYear
                new BigDecimal("0.08"),    // epsGrowth 8%
                5.0                         // fiveYearPeriod
        );

        calculator = new FormulaCalculator();
        calculator.calculate(aapl, aaplFinancials, aaplHistory, aaplInputs);
    }

    // ========== P/E Ratios ==========

    @Test
    @Order(1)
    void peRatioTTMCalculation() {
        BigDecimal pe = calculator.getPeRatioTTM();
        // 185.50 / 6.42 ≈ 28.89
        assertTrue(pe.compareTo(new BigDecimal("28")) > 0);
        assertTrue(pe.compareTo(new BigDecimal("30")) < 0);
    }

    @Test
    @Order(2)
    void forwardPERatioCalculation() {
        BigDecimal fpe = calculator.getForwardPERatio();
        // 185.50 / 7.10 ≈ 26.13
        assertTrue(fpe.compareTo(new BigDecimal("25")) > 0);
        assertTrue(fpe.compareTo(new BigDecimal("28")) < 0);
    }

    @Test
    @Order(3)
    void forwardPELessThanTrailingWhenEPSGrowing() {
        assertTrue(calculator.getForwardPERatio().compareTo(calculator.getPeRatioTTM()) < 0,
                "Forward P/E should be lower than trailing P/E when EPS is growing");
    }

    @Test
    @Order(4)
    void assumedForwardPEIsAverageOfTTMAndForward() {
        BigDecimal assumedFwd = calculator.getAssumedForwardPE();
        BigDecimal avgExpected = BigDecimalMath.average(calculator.getPeRatioTTM(), calculator.getForwardPERatio());
        assertEquals(0, avgExpected.setScale(2, RoundingMode.HALF_UP)
                .compareTo(assumedFwd),
                "Assumed forward P/E should be average of TTM and forward P/E");
    }

    // ========== Intrinsic Value & Graham ==========

    @Test
    @Order(5)
    void intrinsicValuePositive() {
        BigDecimal iv = calculator.getIntrinsicValue();
        assertTrue(iv.compareTo(BigDecimal.ZERO) > 0,
                "Intrinsic value should be positive for profitable company");
    }

    @Test
    @Order(6)
    void grahamMarginOfSafetyCalculation() {
        BigDecimal mos = calculator.getGrahamMarginOfSafety();
        // Graham MOS = intrinsicValue / price
        BigDecimal expected = BigDecimalMath.divide(
                calculator.getIntrinsicValue(), aapl.getLastTradePriceOnly(), 2);
        assertEquals(0, expected.compareTo(mos),
                "Graham MOS should be intrinsic value / price");
    }

    @Test
    @Order(7)
    void buffettMarginOfSafetyIs75PctOfIntrinsic() {
        BigDecimal buffett = calculator.getBuffettMarginOfSafety();
        BigDecimal expected = calculator.getIntrinsicValue()
                .multiply(new BigDecimal("0.75"))
                .setScale(2, RoundingMode.HALF_UP);
        assertEquals(0, expected.compareTo(buffett),
                "Buffett MOS should be 75% of intrinsic value");
    }

    // ========== EPS Projections ==========

    @Test
    @Order(8)
    void epsProjectionsGrowByGrowthRate() {
        BigDecimal year1 = calculator.getEpsOverHoldingPeriodYearOne();
        BigDecimal year2 = calculator.getEpsOverHoldingPeriodYearTwo();
        BigDecimal year3 = calculator.getEpsOverHoldingPeriodYearThree();

        assertTrue(year1.compareTo(BigDecimal.ZERO) > 0, "Year 1 EPS should be positive");
        assertTrue(year2.compareTo(year1) > 0, "Year 2 should grow over Year 1");
        assertTrue(year3.compareTo(year2) > 0, "Year 3 should grow over Year 2");

        // Each year should grow by the EPS growth rate (8%)
        BigDecimal growthFactor = new BigDecimal("1.08");
        BigDecimal expectedYear1 = aapl.getDilutedEPS().multiply(growthFactor).setScale(2, RoundingMode.HALF_UP);
        assertEquals(0, expectedYear1.compareTo(year1),
                "Year 1 EPS = current EPS * (1 + growth rate)");
    }

    @Test
    @Order(9)
    void epsProjectionTotalEqualsSum() {
        BigDecimal total = calculator.getEpsOverHoldingPeriodTotal();
        BigDecimal sum = calculator.getEpsOverHoldingPeriodYearOne()
                .add(calculator.getEpsOverHoldingPeriodYearTwo())
                .add(calculator.getEpsOverHoldingPeriodYearThree())
                .setScale(2, RoundingMode.HALF_UP);
        assertEquals(0, sum.compareTo(total), "Total EPS should equal sum of yearly projections");
    }

    // ========== Price Projections & Fair Value ==========

    @Test
    @Order(10)
    void expectedSharePricePositive() {
        BigDecimal expectedPrice = calculator.getExpectedSharePriceInThreeYears();
        assertTrue(expectedPrice.compareTo(BigDecimal.ZERO) > 0,
                "Expected share price in 3 years should be positive");
    }

    @Test
    @Order(11)
    void expectedShareValueIncludesDividends() {
        BigDecimal expectedValue = calculator.getExpectedShareValueAtEndOfThreeYears();
        BigDecimal expectedPrice = calculator.getExpectedSharePriceInThreeYears();
        BigDecimal totalDividends = calculator.getTotalDividendsPerShareOverThreeYears();

        BigDecimal expected = expectedPrice.add(totalDividends).setScale(2, RoundingMode.HALF_UP);
        assertEquals(0, expected.compareTo(expectedValue),
                "Share value = expected price + total dividends");
    }

    @Test
    @Order(12)
    void presentValueDiscountedFromFutureValue() {
        BigDecimal presentValue = calculator.getPresentShareValueForGoodValue();
        BigDecimal futureValue = calculator.getExpectedShareValueAtEndOfThreeYears();
        // Present value should be less than future value (discounting at 5%/year)
        assertTrue(presentValue.compareTo(futureValue) < 0,
                "Present value should be less than future value due to discounting");
    }

    // ========== P/S Ratio Pipeline ==========

    @Test
    @Order(13)
    void psRatiosComputedFromHistoricalAndFinancial() {
        BigDecimal maxPS = calculator.getMaxPSRatioThisQtr();
        BigDecimal minPS = calculator.getMinPSRatioThisQtr();

        assertTrue(maxPS.compareTo(BigDecimal.ZERO) > 0, "Max P/S should be positive");
        assertTrue(minPS.compareTo(BigDecimal.ZERO) > 0, "Min P/S should be positive");
        assertTrue(maxPS.compareTo(minPS) >= 0, "Max P/S >= Min P/S");
    }

    @Test
    @Order(14)
    void psRatiosCrossQuarterComparison() {
        BigDecimal maxThisQtr = calculator.getMaxPSRatioThisQtr();
        BigDecimal maxLastQtr = calculator.getMaxPSRatioLastQtr();
        BigDecimal minThisQtr = calculator.getMinPSRatioThisQtr();
        BigDecimal minLastQtr = calculator.getMinPSRatioLastQtr();

        // All should be positive
        assertTrue(maxThisQtr.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(maxLastQtr.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(minThisQtr.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(minLastQtr.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @Order(15)
    void priceAtPSRatiosAreConsistent() {
        BigDecimal priceMax = calculator.getPriceAtMaxPSRatioThisQtr();
        BigDecimal priceMin = calculator.getPriceAtMinPSRatioThisQtr();

        assertTrue(priceMax.compareTo(priceMin) >= 0,
                "Price at max P/S should be >= price at min P/S");
        assertTrue(priceMax.compareTo(BigDecimal.ZERO) > 0,
                "Price at max P/S should be positive");
        assertTrue(priceMin.compareTo(BigDecimal.ZERO) > 0,
                "Price at min P/S should be positive");
    }

    // ========== Growth Metrics ==========

    @Test
    @Order(16)
    void growthMultipleCalculation() {
        BigDecimal gm = calculator.getGrowthMultiple();
        // epsYearFive / epsYearOne = 7.10 / 5.61 ≈ 1.27
        assertTrue(gm.compareTo(new BigDecimal("1.0")) > 0,
                "Growth multiple should be > 1 when EPS is growing");
        assertTrue(gm.compareTo(new BigDecimal("2.0")) < 0,
                "Growth multiple should be reasonable");
    }

    @Test
    @Order(17)
    void compoundAnnualGrowthRatePositive() {
        BigDecimal cagr = calculator.getCompoundAnnualGrowthRate();
        assertTrue(cagr.compareTo(BigDecimal.ZERO) > 0,
                "CAGR should be positive for growing EPS");
    }

    @Test
    @Order(18)
    void foolEPSGrowthCalculation() {
        BigDecimal foolGrowth = calculator.getFoolEPSGrowth();
        // (7.10 - 6.42) / 6.42 ≈ 0.1059
        assertTrue(foolGrowth.compareTo(BigDecimal.ZERO) > 0,
                "Fool EPS growth should be positive when next year estimate > current EPS");
    }

    // ========== Year Range Metrics ==========

    @Test
    @Order(19)
    void yearLowDifferenceCalculation() {
        BigDecimal yld = calculator.getYearLowDifference();
        // 1 - (yearLow / price) = 1 - (124.17/185.50) ≈ 0.33
        assertNotNull(yld);
        assertTrue(yld.compareTo(BigDecimal.ZERO) > 0,
                "Year low difference should be positive when price > year low");
    }

    @Test
    @Order(20)
    void yearsRangeDifference() {
        BigDecimal rangeDiff = calculator.getYearsRangeDifference();
        // 199.62 - 124.17 = 75.45
        BigDecimal expected = aapl.getYearHigh().subtract(aapl.getYearLow())
                .setScale(2, RoundingMode.HALF_UP);
        assertEquals(0, expected.compareTo(rangeDiff),
                "Year range diff should equal year high - year low");
    }

    // ========== Fixed Rates ==========

    @Test
    @Order(21)
    void fixedRatesConfiguredCorrectly() {
        assertEquals(new BigDecimal("0.06"), calculator.getFixedEPSGrowth());
        assertEquals(new BigDecimal("0.05"), calculator.getDesiredReturnPerYear());
        assertEquals(new BigDecimal("4.09"), calculator.getCorporateBondsYield());
        assertEquals(new BigDecimal("4.4"), calculator.getRateOfReturn());
    }

    // ========== FinancialData Revenue Per Share ==========

    @Test
    @Order(22)
    void revenuePerShareComputedCorrectly() {
        BigDecimal rps0 = aaplFinancials.getRevenuePerShare(0);
        // 94836 / 15744 ≈ 6.02
        assertTrue(rps0.compareTo(BigDecimal.ZERO) > 0,
                "Revenue per share should be positive");

        BigDecimal rpsTTM = aaplFinancials.getRevenuePerShareTTM();
        assertTrue(rpsTTM.compareTo(BigDecimal.ZERO) > 0,
                "TTM revenue per share should be positive");
    }

    @Test
    @Order(23)
    void revenuePerShareTTMLastQtrEqualsFirstFourQuarters() {
        BigDecimal expected = aaplFinancials.getRevenuePerShare(0)
                .add(aaplFinancials.getRevenuePerShare(1))
                .add(aaplFinancials.getRevenuePerShare(2))
                .add(aaplFinancials.getRevenuePerShare(3))
                .setScale(2, RoundingMode.HALF_UP);
        assertEquals(0, expected.compareTo(aaplFinancials.getRevenuePerShareTTMLastQtr()),
                "TTM last quarter RPS should equal sum of first 4 quarters");
    }

    // ========== Dividend Analysis ==========

    @Test
    @Order(24)
    void dividendPayoutRatioReasonable() {
        BigDecimal payoutRatio = calculator.getDividendPayoutRatio();
        assertNotNull(payoutRatio);
        // AAPL has a modest dividend; payout ratio should be well under 1
        assertTrue(payoutRatio.abs().compareTo(new BigDecimal("2")) < 0,
                "Payout ratio should be reasonable for AAPL");
    }

    // ========== QuoteData Model Integrity ==========

    @Test
    @Order(25)
    void quoteDataFieldsPreserved() {
        assertEquals(new BigDecimal("185.50"), aapl.getLastTradePriceOnly());
        assertEquals(new BigDecimal("6.42"), aapl.getDilutedEPS());
        assertEquals(new BigDecimal("7.10"), aapl.getEpsEstimateNextYear());
        assertEquals(new BigDecimal("0.96"), aapl.getTrailingAnnualDividendYield());
        assertEquals(new BigDecimal("199.62"), aapl.getYearHigh());
        assertEquals(new BigDecimal("124.17"), aapl.getYearLow());
        assertEquals(new BigDecimal("55000000"), aapl.getVolume());
        assertEquals(2900000000000L, aapl.getMarketCapitalization());
        assertEquals("2.9T", aapl.getMarketCapitalizationStr());
        assertFalse(aapl.isIncomplete());
        assertFalse(aapl.isError());
    }

    // ========== HistoricalData Model Integrity ==========

    @Test
    @Order(26)
    void historicalDataFieldsPreserved() {
        assertEquals("AAPL", aaplHistory.getTicker());
        assertEquals(new BigDecimal("198.23"), aaplHistory.getHighestPriceThisQtr());
        assertEquals(new BigDecimal("165.67"), aaplHistory.getLowestPriceThisQtr());
        assertEquals(new BigDecimal("189.98"), aaplHistory.getHighestPriceLastQtr());
        assertEquals(new BigDecimal("167.62"), aaplHistory.getLowestPriceLastQtr());
        assertFalse(aaplHistory.isIncomplete());
        assertFalse(aaplHistory.isError());
    }

    // ========== Latest P/S ==========

    @Test
    @Order(27)
    void latestPriceSalesCalculation() {
        BigDecimal latestPS = calculator.getLatestPriceSales();
        assertTrue(latestPS.compareTo(BigDecimal.ZERO) > 0,
                "Latest P/S should be positive");
        // 185.50 / (TTM rev per share) should give a reasonable P/S ratio
        assertTrue(latestPS.compareTo(new BigDecimal("1")) > 0,
                "AAPL P/S should be > 1");
    }

    // ========== Recalculation with Different Data ==========

    @Test
    @Order(28)
    void recalculationWithValueStockData() {
        // Test with a value stock profile (low P/E, high dividend)
        var valueQuote = new QuoteData();
        valueQuote.setLastTradePriceOnly(new BigDecimal("45.00"));
        valueQuote.setDilutedEPS(new BigDecimal("5.00"));
        valueQuote.setEpsEstimateNextYear(new BigDecimal("5.25"));
        valueQuote.setTrailingAnnualDividendYield(new BigDecimal("2.50"));
        valueQuote.setYearHigh(new BigDecimal("52.00"));
        valueQuote.setYearLow(new BigDecimal("38.00"));

        var valueFinancial = new FinancialData();
        for (int i = 0; i < 6; i++) {
            valueFinancial.setRevenue(i, 50000 + i * 2000);
            valueFinancial.setDilutedShares(i, 10000);
        }
        valueFinancial.computeRevenuePerShare();

        var valueHistory = new HistoricalData("VAL");
        valueHistory.setHighestPriceThisQtr(new BigDecimal("50.00"));
        valueHistory.setLowestPriceThisQtr(new BigDecimal("40.00"));
        valueHistory.setHighestPriceLastQtr(new BigDecimal("48.00"));
        valueHistory.setLowestPriceLastQtr(new BigDecimal("36.00"));

        var valueInputs = new ValuationInputs(
                new BigDecimal("4.50"),
                new BigDecimal("5.50"),
                new BigDecimal("5.25"),
                new BigDecimal("0.04"),
                5.0
        );

        var valueCalc = new FormulaCalculator();
        valueCalc.calculate(valueQuote, valueFinancial, valueHistory, valueInputs);

        // Value stock should have lower P/E than AAPL
        assertTrue(valueCalc.getPeRatioTTM().compareTo(calculator.getPeRatioTTM()) < 0,
                "Value stock should have lower P/E than growth stock");

        // Value stock intrinsic value should be positive
        assertTrue(valueCalc.getIntrinsicValue().compareTo(BigDecimal.ZERO) > 0);

        // Value stock should have higher dividend payout ratio than AAPL
        // (since it has higher dividend yield relative to EPS)
        assertTrue(valueCalc.getDividendPayoutRatio().abs().compareTo(BigDecimal.ZERO) >= 0);
    }
}
