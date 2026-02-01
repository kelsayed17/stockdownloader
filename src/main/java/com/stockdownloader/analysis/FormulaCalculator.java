package com.stockdownloader.analysis;

import com.stockdownloader.model.FinancialData;
import com.stockdownloader.model.HistoricalData;
import com.stockdownloader.model.QuoteData;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.stockdownloader.util.BigDecimalMath.*;

/**
 * Calculates stock valuation metrics including Graham Number, intrinsic value,
 * margin of safety, P/E ratios, and projected returns.
 */
public class FormulaCalculator {

    private static final BigDecimal DEFAULT_FIXED_EPS_GROWTH = new BigDecimal("0.06");
    private static final BigDecimal DEFAULT_DESIRED_RETURN = new BigDecimal("0.05");
    private static final BigDecimal DEFAULT_CORPORATE_BONDS_YIELD = new BigDecimal("4.09");
    private static final BigDecimal DEFAULT_RATE_OF_RETURN = new BigDecimal("4.4");

    private BigDecimal fixedEPSGrowth = BigDecimal.ZERO;
    private BigDecimal desiredReturnPerYear = BigDecimal.ZERO;
    private BigDecimal corporateBondsYield = BigDecimal.ZERO;
    private BigDecimal rateOfReturn = BigDecimal.ZERO;
    private BigDecimal differenceFromPriceAtMinPSRatio = BigDecimal.ZERO;
    private BigDecimal differenceFromPriceAtMaxPSRatio = BigDecimal.ZERO;
    private BigDecimal growthMultiple = BigDecimal.ZERO;
    private BigDecimal fiveYearGrowthMultiple = BigDecimal.ZERO;
    private BigDecimal yearLowDifference = BigDecimal.ZERO;
    private BigDecimal yearsRangeDifference = BigDecimal.ZERO;
    private BigDecimal compoundAnnualGrowthRate = BigDecimal.ZERO;
    private BigDecimal foolEPSGrowth = BigDecimal.ZERO;
    private BigDecimal intrinsicValue = BigDecimal.ZERO;
    private BigDecimal grahamMarginOfSafety = BigDecimal.ZERO;
    private BigDecimal buffettMarginOfSafety = BigDecimal.ZERO;
    private BigDecimal peRatioTTM = BigDecimal.ZERO;
    private BigDecimal forwardPERatio = BigDecimal.ZERO;
    private BigDecimal assumedForwardPE = BigDecimal.ZERO;
    private BigDecimal epsOverHoldingPeriodYearOne = BigDecimal.ZERO;
    private BigDecimal epsOverHoldingPeriodYearTwo = BigDecimal.ZERO;
    private BigDecimal epsOverHoldingPeriodYearThree = BigDecimal.ZERO;
    private BigDecimal epsOverHoldingPeriodTotal = BigDecimal.ZERO;
    private BigDecimal expectedSharePriceInThreeYears = BigDecimal.ZERO;
    private BigDecimal dividendPayoutRatio = BigDecimal.ZERO;
    private BigDecimal totalDividendsPerShareOverThreeYears = BigDecimal.ZERO;
    private BigDecimal expectedShareValueAtEndOfThreeYears = BigDecimal.ZERO;
    private BigDecimal presentShareValueForGoodValue = BigDecimal.ZERO;
    private BigDecimal latestPriceSales = BigDecimal.ZERO;
    private BigDecimal maxPSRatioThisQtr = BigDecimal.ZERO;
    private BigDecimal minPSRatioThisQtr = BigDecimal.ZERO;
    private BigDecimal maxPSRatioLastQtr = BigDecimal.ZERO;
    private BigDecimal minPSRatioLastQtr = BigDecimal.ZERO;
    private BigDecimal priceAtMaxPSRatioThisQtr = BigDecimal.ZERO;
    private BigDecimal priceAtMaxPSRatioLastQtr = BigDecimal.ZERO;
    private BigDecimal priceAtMinPSRatioThisQtr = BigDecimal.ZERO;
    private BigDecimal priceAtMinPSRatioLastQtr = BigDecimal.ZERO;
    private BigDecimal epsYearFive = BigDecimal.ZERO;
    private BigDecimal epsYearOne = BigDecimal.ZERO;
    private BigDecimal epsEstimateNextYear = BigDecimal.ZERO;
    private BigDecimal epsGrowth = BigDecimal.ZERO;
    private double fiveYearPeriod;

    public FormulaCalculator() {}

    public void calculate(QuoteData yf, FinancialData ms, HistoricalData yh) {
        BigDecimal revenuePerShareTTM = ms.getRevenuePerShareTTM();
        BigDecimal revenuePerShareTTMLastQtr = ms.getRevenuePerShareTTMLastQtr();

        computePSRatios(yh, revenuePerShareTTM, revenuePerShareTTMLastQtr);
        computeFixedRates();
        computeValuationMetrics(yf);
        computeProjections(yf, ms);
    }

    private void computePSRatios(HistoricalData yh, BigDecimal revTTM, BigDecimal revTTMLastQtr) {
        maxPSRatioThisQtr = divide(yh.getHighestPriceThisQtr(), revTTM, 2);
        minPSRatioThisQtr = divide(yh.getLowestPriceThisQtr(), revTTM, 2);
        maxPSRatioLastQtr = divide(yh.getHighestPriceLastQtr(), revTTMLastQtr, 2);
        minPSRatioLastQtr = divide(yh.getLowestPriceLastQtr(), revTTMLastQtr, 2);

        BigDecimal maxRatio = maxPSRatioThisQtr.max(maxPSRatioLastQtr);
        BigDecimal minRatio = minPSRatioThisQtr.min(minPSRatioLastQtr);

        priceAtMaxPSRatioThisQtr = scale2(maxRatio.multiply(revTTM));
        priceAtMaxPSRatioLastQtr = scale2(maxRatio.multiply(revTTMLastQtr));
        priceAtMinPSRatioThisQtr = scale2(minRatio.multiply(revTTM));
        priceAtMinPSRatioLastQtr = scale2(minRatio.multiply(revTTMLastQtr));
    }

    private void computeFixedRates() {
        fixedEPSGrowth = DEFAULT_FIXED_EPS_GROWTH;
        desiredReturnPerYear = DEFAULT_DESIRED_RETURN;
        corporateBondsYield = DEFAULT_CORPORATE_BONDS_YIELD;
        rateOfReturn = DEFAULT_RATE_OF_RETURN;
    }

    private void computeValuationMetrics(QuoteData yf) {
        BigDecimal price = yf.getLastTradePriceOnly();
        BigDecimal eps = yf.getDilutedEPS();

        differenceFromPriceAtMinPSRatio = scale2(subtract(BigDecimal.ONE, divide(priceAtMinPSRatioThisQtr, price)));
        differenceFromPriceAtMaxPSRatio = scale2(subtract(BigDecimal.ONE, divide(price, priceAtMaxPSRatioThisQtr)));
        growthMultiple = scale2(divide(epsYearFive, epsYearOne));
        fiveYearGrowthMultiple = scale2(BigDecimal.valueOf(
                Math.pow(Math.abs(growthMultiple.abs().doubleValue()), fiveYearPeriod)));
        yearLowDifference = scale2(subtract(BigDecimal.ONE, divide(yf.getYearLow(), price)));
        yearsRangeDifference = scale2(subtract(yf.getYearHigh(), yf.getYearLow()));
        compoundAnnualGrowthRate = scale2(multiply(subtract(fiveYearGrowthMultiple, BigDecimal.ONE), BigDecimal.valueOf(100)));
        foolEPSGrowth = scale2(divide(subtract(epsEstimateNextYear, eps), eps));

        BigDecimal adjustedGrowth = add(BigDecimal.valueOf(8.5), multiply(BigDecimal.valueOf(2), multiply(epsGrowth, BigDecimal.valueOf(100))));
        intrinsicValue = scale2(divide(multiply(multiply(eps, adjustedGrowth), rateOfReturn), corporateBondsYield));
        grahamMarginOfSafety = scale2(divide(intrinsicValue, price));
        buffettMarginOfSafety = scale2(multiply(intrinsicValue, BigDecimal.valueOf(0.75)));

        peRatioTTM = scale2(divide(price, eps));
        forwardPERatio = scale2(divide(price, epsEstimateNextYear));
        assumedForwardPE = scale2(average(peRatioTTM, forwardPERatio));
    }

    private void computeProjections(QuoteData yf, FinancialData ms) {
        BigDecimal eps = yf.getDilutedEPS();
        BigDecimal growthFactor = add(epsGrowth, BigDecimal.ONE);

        epsOverHoldingPeriodYearOne = scale2(multiply(eps, growthFactor));
        epsOverHoldingPeriodYearTwo = scale2(multiply(epsOverHoldingPeriodYearOne, growthFactor));
        epsOverHoldingPeriodYearThree = scale2(multiply(epsOverHoldingPeriodYearTwo, growthFactor));
        epsOverHoldingPeriodTotal = scale2(add(epsOverHoldingPeriodYearOne,
                add(epsOverHoldingPeriodYearTwo, epsOverHoldingPeriodYearThree)));

        expectedSharePriceInThreeYears = scale2(multiply(epsOverHoldingPeriodYearThree, assumedForwardPE));
        dividendPayoutRatio = scale2(divide(yf.getTrailingAnnualDividendYield(), epsOverHoldingPeriodYearThree));
        totalDividendsPerShareOverThreeYears = scale2(multiply(dividendPayoutRatio, epsOverHoldingPeriodTotal));
        expectedShareValueAtEndOfThreeYears = scale2(add(totalDividendsPerShareOverThreeYears, expectedSharePriceInThreeYears));
        presentShareValueForGoodValue = scale2(divide(expectedShareValueAtEndOfThreeYears,
                add(BigDecimal.ONE, desiredReturnPerYear).pow(3)));
        latestPriceSales = scale2(divide(yf.getLastTradePriceOnly(), ms.getRevenuePerShareTTM()));
    }

    // Getters
    public BigDecimal getFixedEPSGrowth() { return fixedEPSGrowth; }
    public BigDecimal getDesiredReturnPerYear() { return desiredReturnPerYear; }
    public BigDecimal getCorporateBondsYield() { return corporateBondsYield; }
    public BigDecimal getRateOfReturn() { return rateOfReturn; }
    public BigDecimal getDifferenceFromPriceAtMinPSRatio() { return differenceFromPriceAtMinPSRatio; }
    public BigDecimal getDifferenceFromPriceAtMaxPSRatio() { return differenceFromPriceAtMaxPSRatio; }
    public BigDecimal getGrowthMultiple() { return growthMultiple; }
    public BigDecimal getFiveYearGrowthMultiple() { return fiveYearGrowthMultiple; }
    public BigDecimal getYearLowDifference() { return yearLowDifference; }
    public BigDecimal getYearsRangeDifference() { return yearsRangeDifference; }
    public BigDecimal getCompoundAnnualGrowthRate() { return compoundAnnualGrowthRate; }
    public BigDecimal getFoolEPSGrowth() { return foolEPSGrowth; }
    public BigDecimal getIntrinsicValue() { return intrinsicValue; }
    public BigDecimal getGrahamMarginOfSafety() { return grahamMarginOfSafety; }
    public BigDecimal getBuffettMarginOfSafety() { return buffettMarginOfSafety; }
    public BigDecimal getPeRatioTTM() { return peRatioTTM; }
    public BigDecimal getForwardPERatio() { return forwardPERatio; }
    public BigDecimal getAssumedForwardPE() { return assumedForwardPE; }
    public BigDecimal getEpsOverHoldingPeriodYearOne() { return epsOverHoldingPeriodYearOne; }
    public BigDecimal getEpsOverHoldingPeriodYearTwo() { return epsOverHoldingPeriodYearTwo; }
    public BigDecimal getEpsOverHoldingPeriodYearThree() { return epsOverHoldingPeriodYearThree; }
    public BigDecimal getEpsOverHoldingPeriodTotal() { return epsOverHoldingPeriodTotal; }
    public BigDecimal getExpectedSharePriceInThreeYears() { return expectedSharePriceInThreeYears; }
    public BigDecimal getDividendPayoutRatio() { return dividendPayoutRatio; }
    public BigDecimal getTotalDividendsPerShareOverThreeYears() { return totalDividendsPerShareOverThreeYears; }
    public BigDecimal getExpectedShareValueAtEndOfThreeYears() { return expectedShareValueAtEndOfThreeYears; }
    public BigDecimal getPresentShareValueForGoodValue() { return presentShareValueForGoodValue; }
    public BigDecimal getLatestPriceSales() { return latestPriceSales; }
    public BigDecimal getMaxPSRatioThisQtr() { return maxPSRatioThisQtr; }
    public BigDecimal getMinPSRatioThisQtr() { return minPSRatioThisQtr; }
    public BigDecimal getMaxPSRatioLastQtr() { return maxPSRatioLastQtr; }
    public BigDecimal getMinPSRatioLastQtr() { return minPSRatioLastQtr; }
    public BigDecimal getPriceAtMaxPSRatioThisQtr() { return priceAtMaxPSRatioThisQtr; }
    public BigDecimal getPriceAtMaxPSRatioLastQtr() { return priceAtMaxPSRatioLastQtr; }
    public BigDecimal getPriceAtMinPSRatioThisQtr() { return priceAtMinPSRatioThisQtr; }
    public BigDecimal getPriceAtMinPSRatioLastQtr() { return priceAtMinPSRatioLastQtr; }

    // Setters for configurable inputs
    public void setEpsYearFive(BigDecimal v) { this.epsYearFive = v; }
    public void setEpsYearOne(BigDecimal v) { this.epsYearOne = v; }
    public void setEpsEstimateNextYear(BigDecimal v) { this.epsEstimateNextYear = v; }
    public void setEpsGrowth(BigDecimal v) { this.epsGrowth = v; }
    public void setFiveYearPeriod(double v) { this.fiveYearPeriod = v; }
}
