package com.stockdownloader.integration;

import com.stockdownloader.analysis.FormulaCalculator;
import com.stockdownloader.analysis.FormulaCalculator.ValuationInputs;
import com.stockdownloader.model.FinancialData;
import com.stockdownloader.model.HistoricalData;
import com.stockdownloader.model.QuoteData;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the full valuation calculation pipeline:
 * QuoteData + FinancialData + HistoricalData + ValuationInputs -> FormulaCalculator -> computed metrics.
 */
class FormulaCalculatorIntegrationTest {

    @Test
    void fullValuationPipeline() {
        // Set up QuoteData (simulating Yahoo Finance data)
        var quote = new QuoteData();
        quote.setLastTradePriceOnly(new BigDecimal("150.00"));
        quote.setDilutedEPS(new BigDecimal("6.50"));
        quote.setEpsEstimateNextYear(new BigDecimal("7.20"));
        quote.setTrailingAnnualDividendYield(new BigDecimal("0.92"));
        quote.setYearHigh(new BigDecimal("180.00"));
        quote.setYearLow(new BigDecimal("120.00"));
        quote.setPriceSales(new BigDecimal("7.5"));

        // Set up FinancialData (simulating Morningstar data)
        var financial = new FinancialData();
        for (int i = 0; i < 6; i++) {
            financial.setRevenue(i, 90000 + i * 5000);
            financial.setDilutedShares(i, 16000);
        }
        financial.computeRevenuePerShare();

        // Set up HistoricalData (simulating historical price data)
        var historical = new HistoricalData("AAPL");
        historical.setHighestPriceThisQtr(new BigDecimal("175.00"));
        historical.setLowestPriceThisQtr(new BigDecimal("130.00"));
        historical.setHighestPriceLastQtr(new BigDecimal("170.00"));
        historical.setLowestPriceLastQtr(new BigDecimal("125.00"));

        // Set up ValuationInputs
        var inputs = new ValuationInputs(
                new BigDecimal("4.50"),   // epsYearOne
                new BigDecimal("7.50"),   // epsYearFive
                new BigDecimal("7.20"),   // epsEstimateNextYear
                new BigDecimal("0.10"),   // epsGrowth (10%)
                5.0                        // fiveYearPeriod
        );

        // Run the calculation
        var calculator = new FormulaCalculator();
        calculator.calculate(quote, financial, historical, inputs);

        // Verify P/E ratios
        BigDecimal peRatioTTM = calculator.getPeRatioTTM();
        assertNotNull(peRatioTTM);
        assertTrue(peRatioTTM.compareTo(BigDecimal.ZERO) > 0,
                "P/E ratio TTM should be positive");
        // 150/6.5 ~ 23.08
        assertTrue(peRatioTTM.compareTo(new BigDecimal("22")) > 0);
        assertTrue(peRatioTTM.compareTo(new BigDecimal("24")) < 0);

        BigDecimal forwardPE = calculator.getForwardPERatio();
        assertNotNull(forwardPE);
        assertTrue(forwardPE.compareTo(BigDecimal.ZERO) > 0,
                "Forward P/E should be positive");
        // 150/7.2 ~ 20.83
        assertTrue(forwardPE.compareTo(new BigDecimal("20")) > 0);
        assertTrue(forwardPE.compareTo(new BigDecimal("22")) < 0);

        // Forward P/E should be less than TTM P/E when EPS is growing
        assertTrue(forwardPE.compareTo(peRatioTTM) < 0,
                "Forward P/E should be less than TTM P/E when EPS is growing");
    }

    @Test
    void intrinsicValueCalculation() {
        var quote = createStandardQuote();
        var financial = createStandardFinancial();
        var historical = createStandardHistorical();
        var inputs = new ValuationInputs(
                new BigDecimal("5.00"),
                new BigDecimal("8.00"),
                new BigDecimal("7.00"),
                new BigDecimal("0.08"),
                5.0
        );

        var calculator = new FormulaCalculator();
        calculator.calculate(quote, financial, historical, inputs);

        BigDecimal intrinsicValue = calculator.getIntrinsicValue();
        assertNotNull(intrinsicValue);
        // Intrinsic value should be a reasonable stock price
        assertTrue(intrinsicValue.compareTo(BigDecimal.ZERO) > 0,
                "Intrinsic value should be positive for positive EPS");
    }

    @Test
    void marginOfSafetyCalculation() {
        var quote = createStandardQuote();
        var financial = createStandardFinancial();
        var historical = createStandardHistorical();
        var inputs = new ValuationInputs(
                new BigDecimal("5.00"),
                new BigDecimal("8.00"),
                new BigDecimal("7.00"),
                new BigDecimal("0.08"),
                5.0
        );

        var calculator = new FormulaCalculator();
        calculator.calculate(quote, financial, historical, inputs);

        BigDecimal grahamMOS = calculator.getGrahamMarginOfSafety();
        BigDecimal buffettMOS = calculator.getBuffettMarginOfSafety();

        assertNotNull(grahamMOS);
        assertNotNull(buffettMOS);

        // Buffett MOS should be 75% of intrinsic value
        BigDecimal intrinsic = calculator.getIntrinsicValue();
        BigDecimal expectedBuffett = intrinsic.multiply(new BigDecimal("0.75"))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        assertEquals(0, expectedBuffett.compareTo(buffettMOS),
                "Buffett MOS should be 75% of intrinsic value");
    }

    @Test
    void epsProjectionsGrowOverTime() {
        var quote = createStandardQuote();
        var financial = createStandardFinancial();
        var historical = createStandardHistorical();
        var inputs = new ValuationInputs(
                new BigDecimal("5.00"),
                new BigDecimal("8.00"),
                new BigDecimal("7.00"),
                new BigDecimal("0.10"),
                5.0
        );

        var calculator = new FormulaCalculator();
        calculator.calculate(quote, financial, historical, inputs);

        BigDecimal year1 = calculator.getEpsOverHoldingPeriodYearOne();
        BigDecimal year2 = calculator.getEpsOverHoldingPeriodYearTwo();
        BigDecimal year3 = calculator.getEpsOverHoldingPeriodYearThree();

        assertTrue(year1.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(year2.compareTo(year1) > 0,
                "Year 2 EPS should be greater than Year 1 with positive growth");
        assertTrue(year3.compareTo(year2) > 0,
                "Year 3 EPS should be greater than Year 2 with positive growth");

        // Total should equal sum
        BigDecimal total = calculator.getEpsOverHoldingPeriodTotal();
        BigDecimal expectedTotal = year1.add(year2).add(year3)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        assertEquals(0, expectedTotal.compareTo(total),
                "EPS total should equal sum of yearly projections");
    }

    @Test
    void psRatioCalculation() {
        var quote = createStandardQuote();
        var financial = createStandardFinancial();
        var historical = createStandardHistorical();
        var inputs = new ValuationInputs(
                new BigDecimal("5.00"),
                new BigDecimal("8.00"),
                new BigDecimal("7.00"),
                new BigDecimal("0.08"),
                5.0
        );

        var calculator = new FormulaCalculator();
        calculator.calculate(quote, financial, historical, inputs);

        // Max PS ratio should be >= min PS ratio
        BigDecimal maxPS = calculator.getMaxPSRatioThisQtr();
        BigDecimal minPS = calculator.getMinPSRatioThisQtr();
        assertTrue(maxPS.compareTo(minPS) >= 0,
                "Max P/S ratio should be >= min P/S ratio");

        // Price at max ratio should be >= price at min ratio
        BigDecimal priceMax = calculator.getPriceAtMaxPSRatioThisQtr();
        BigDecimal priceMin = calculator.getPriceAtMinPSRatioThisQtr();
        assertTrue(priceMax.compareTo(priceMin) >= 0,
                "Price at max ratio should be >= price at min ratio");
    }

    @Test
    void fixedRatesAreSetCorrectly() {
        var quote = createStandardQuote();
        var financial = createStandardFinancial();
        var historical = createStandardHistorical();
        var inputs = new ValuationInputs(
                new BigDecimal("5.00"),
                new BigDecimal("8.00"),
                new BigDecimal("7.00"),
                new BigDecimal("0.08"),
                5.0
        );

        var calculator = new FormulaCalculator();
        calculator.calculate(quote, financial, historical, inputs);

        assertEquals(new BigDecimal("0.06"), calculator.getFixedEPSGrowth());
        assertEquals(new BigDecimal("0.05"), calculator.getDesiredReturnPerYear());
        assertEquals(new BigDecimal("4.09"), calculator.getCorporateBondsYield());
        assertEquals(new BigDecimal("4.4"), calculator.getRateOfReturn());
    }

    @Test
    void yearRangeDifference() {
        var quote = createStandardQuote();
        var financial = createStandardFinancial();
        var historical = createStandardHistorical();
        var inputs = new ValuationInputs(
                new BigDecimal("5.00"),
                new BigDecimal("8.00"),
                new BigDecimal("7.00"),
                new BigDecimal("0.08"),
                5.0
        );

        var calculator = new FormulaCalculator();
        calculator.calculate(quote, financial, historical, inputs);

        BigDecimal rangeDiff = calculator.getYearsRangeDifference();
        // 180 - 120 = 60
        assertEquals(0, new BigDecimal("60.00").compareTo(rangeDiff),
                "Year range difference should be year high minus year low");
    }

    private QuoteData createStandardQuote() {
        var quote = new QuoteData();
        quote.setLastTradePriceOnly(new BigDecimal("150.00"));
        quote.setDilutedEPS(new BigDecimal("6.00"));
        quote.setEpsEstimateNextYear(new BigDecimal("7.00"));
        quote.setTrailingAnnualDividendYield(new BigDecimal("0.88"));
        quote.setYearHigh(new BigDecimal("180.00"));
        quote.setYearLow(new BigDecimal("120.00"));
        quote.setPriceSales(new BigDecimal("7.5"));
        return quote;
    }

    private FinancialData createStandardFinancial() {
        var financial = new FinancialData();
        for (int i = 0; i < 6; i++) {
            financial.setRevenue(i, 80000 + i * 4000);
            financial.setDilutedShares(i, 15000);
        }
        financial.computeRevenuePerShare();
        return financial;
    }

    private HistoricalData createStandardHistorical() {
        var historical = new HistoricalData("TEST");
        historical.setHighestPriceThisQtr(new BigDecimal("175.00"));
        historical.setLowestPriceThisQtr(new BigDecimal("130.00"));
        historical.setHighestPriceLastQtr(new BigDecimal("170.00"));
        historical.setLowestPriceLastQtr(new BigDecimal("125.00"));
        return historical;
    }
}
