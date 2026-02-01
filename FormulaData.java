import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class FormulaData {

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
    private BigDecimal epsYearFive = BigDecimal.ZERO;
    private BigDecimal epsYearOne = BigDecimal.ZERO;
    private BigDecimal epsEstimateNextYear = BigDecimal.ZERO;
    private BigDecimal epsGrowth = BigDecimal.ZERO;
    private BigDecimal maxPSRatioThisQtr = BigDecimal.ZERO;
    private BigDecimal minPSRatioThisQtr = BigDecimal.ZERO;
    private BigDecimal maxPSRatioLastQtr = BigDecimal.ZERO;
    private BigDecimal minPSRatioLastQtr = BigDecimal.ZERO;
    private BigDecimal priceAtMaxPSRatioThisQtr = BigDecimal.ZERO;
    private BigDecimal priceAtMaxPSRatioLastQtr = BigDecimal.ZERO;
    private BigDecimal priceAtMinPSRatioThisQtr = BigDecimal.ZERO;
    private BigDecimal priceAtMinPSRatioLastQtr = BigDecimal.ZERO;
    private double fiveYearPeriod;

    public FormulaData() {
        // All fields default to BigDecimal.ZERO
    }

    public FormulaData(YahooFinanceData yf, MorningstarData ms, YahooHistoricalData yh) {
        BDCalculator bd = new BDCalculator();
        var roundTenDecimals = new DecimalFormat("#.##########");
        var mc = new MathContext(2);

        BigDecimal revenuePerShareTTM = ms.getRevenuePerShareTTM();
        BigDecimal revenuePerShareTTMLastQtr = ms.getRevenuePerShareTTMLastQtr();

        maxPSRatioThisQtr = yh.getHighestPriceThisQtr().divide(revenuePerShareTTM, 2, RoundingMode.CEILING);
        minPSRatioThisQtr = yh.getLowestPriceThisQtr().divide(revenuePerShareTTM, 2, RoundingMode.CEILING);
        maxPSRatioLastQtr = yh.getHighestPriceLastQtr().divide(revenuePerShareTTMLastQtr, 2, RoundingMode.CEILING);
        minPSRatioLastQtr = yh.getLowestPriceLastQtr().divide(revenuePerShareTTMLastQtr, 2, RoundingMode.CEILING);

        priceAtMaxPSRatioThisQtr = (maxPSRatioThisQtr.compareTo(maxPSRatioLastQtr) > 0)
                ? maxPSRatioThisQtr.multiply(revenuePerShareTTM, mc)
                : maxPSRatioLastQtr.multiply(revenuePerShareTTM, mc);

        priceAtMaxPSRatioLastQtr = (maxPSRatioLastQtr.compareTo(maxPSRatioThisQtr) > 0)
                ? maxPSRatioLastQtr.multiply(revenuePerShareTTMLastQtr, mc)
                : maxPSRatioThisQtr.multiply(revenuePerShareTTMLastQtr, mc);

        priceAtMinPSRatioThisQtr = (minPSRatioThisQtr.compareTo(minPSRatioLastQtr) < 0)
                ? minPSRatioThisQtr.multiply(revenuePerShareTTM, mc)
                : minPSRatioLastQtr.multiply(revenuePerShareTTM, mc);

        priceAtMinPSRatioLastQtr = (minPSRatioLastQtr.compareTo(minPSRatioThisQtr) < 0)
                ? minPSRatioLastQtr.multiply(revenuePerShareTTMLastQtr, mc)
                : minPSRatioThisQtr.multiply(revenuePerShareTTMLastQtr, mc);

        // Fixed Rates
        fixedEPSGrowth = DEFAULT_FIXED_EPS_GROWTH;
        desiredReturnPerYear = DEFAULT_DESIRED_RETURN;
        corporateBondsYield = DEFAULT_CORPORATE_BONDS_YIELD;
        rateOfReturn = DEFAULT_RATE_OF_RETURN;

        // Calculations
        differenceFromPriceAtMinPSRatio = new BigDecimal(bd.SetScaleTwo(bd.BDSubtract(BigDecimal.valueOf(1), bd.BDDivide(priceAtMinPSRatioThisQtr, yf.getLastTradePriceOnly()))).toString());
        differenceFromPriceAtMaxPSRatio = new BigDecimal(bd.SetScaleTwo(bd.BDSubtract(BigDecimal.valueOf(1), bd.BDDivide(yf.getLastTradePriceOnly(), priceAtMaxPSRatioThisQtr))).toString());
        growthMultiple = new BigDecimal(bd.SetScaleTwo(bd.BDDivide(epsYearFive, epsYearOne)).toString());
        fiveYearGrowthMultiple = new BigDecimal(bd.SetScaleTwo(BigDecimal.valueOf(Double.parseDouble(roundTenDecimals.format(Math.pow(Math.abs(growthMultiple.abs().doubleValue()), fiveYearPeriod))))).toString());
        yearLowDifference = new BigDecimal(bd.SetScaleTwo(bd.BDSubtract(BigDecimal.valueOf(1), bd.BDDivide(yf.getYearLow(), yf.getLastTradePriceOnly()))).toString());
        yearsRangeDifference = new BigDecimal(bd.SetScaleTwo(bd.BDSubtract(yf.getYearHigh(), yf.getYearLow())).toString());
        compoundAnnualGrowthRate = new BigDecimal(bd.SetScaleTwo(bd.BDMultiply(bd.BDSubtract(fiveYearGrowthMultiple, BigDecimal.valueOf(1)), BigDecimal.valueOf(100))).toString());
        foolEPSGrowth = new BigDecimal(bd.SetScaleTwo(bd.BDDivide(bd.BDSubtract(epsEstimateNextYear, yf.getDilutedEPS()), yf.getDilutedEPS())).toString());
        intrinsicValue = new BigDecimal(bd.SetScaleTwo(bd.BDDivide(bd.BDMultiply(bd.BDMultiply(yf.getDilutedEPS(), bd.BDAdd(BigDecimal.valueOf(8.5), bd.BDMultiply(BigDecimal.valueOf(2), bd.BDMultiply(epsGrowth, BigDecimal.valueOf(100))))), rateOfReturn), corporateBondsYield)).toString());
        grahamMarginOfSafety = new BigDecimal(bd.SetScaleTwo(bd.BDDivide(intrinsicValue, yf.getLastTradePriceOnly())).toString());
        buffettMarginOfSafety = new BigDecimal(bd.SetScaleTwo(bd.BDMultiply(intrinsicValue, BigDecimal.valueOf(0.75))).toString());
        peRatioTTM = new BigDecimal(bd.SetScaleTwo(bd.BDDivide(yf.getLastTradePriceOnly(), yf.getDilutedEPS())).toString());
        forwardPERatio = new BigDecimal(bd.SetScaleTwo(bd.BDDivide(yf.getLastTradePriceOnly(), epsEstimateNextYear)).toString());
        assumedForwardPE = new BigDecimal(bd.SetScaleTwo(bd.AverageCalculator(peRatioTTM, forwardPERatio, BigDecimal.ZERO)).toString());
        epsOverHoldingPeriodYearOne = new BigDecimal(bd.SetScaleTwo(bd.BDMultiply(yf.getDilutedEPS(), bd.BDAdd(epsGrowth, BigDecimal.ONE))).toString());
        epsOverHoldingPeriodYearTwo = new BigDecimal(bd.SetScaleTwo(bd.BDMultiply(epsOverHoldingPeriodYearOne, bd.BDAdd(epsGrowth, BigDecimal.ONE))).toString());
        epsOverHoldingPeriodYearThree = new BigDecimal(bd.SetScaleTwo(bd.BDMultiply(epsOverHoldingPeriodYearTwo, bd.BDAdd(epsGrowth, BigDecimal.ONE))).toString());
        epsOverHoldingPeriodTotal = new BigDecimal(bd.SetScaleTwo(bd.BDAdd(epsOverHoldingPeriodYearOne, bd.BDAdd(epsOverHoldingPeriodYearTwo, epsOverHoldingPeriodYearThree))).toString());
        expectedSharePriceInThreeYears = new BigDecimal(bd.SetScaleTwo(bd.BDMultiply(epsOverHoldingPeriodYearThree, assumedForwardPE)).toString());
        dividendPayoutRatio = new BigDecimal(bd.SetScaleTwo(bd.BDDivide(yf.getTrailingAnnualDividendYield(), epsOverHoldingPeriodYearThree)).toString());
        totalDividendsPerShareOverThreeYears = new BigDecimal(bd.SetScaleTwo(bd.BDMultiply(dividendPayoutRatio, epsOverHoldingPeriodTotal)).toString());
        expectedShareValueAtEndOfThreeYears = new BigDecimal(bd.SetScaleTwo(bd.BDAdd(totalDividendsPerShareOverThreeYears, expectedSharePriceInThreeYears)).toString());
        presentShareValueForGoodValue = new BigDecimal(bd.SetScaleTwo(bd.BDDivide(expectedShareValueAtEndOfThreeYears, bd.BDAdd(BigDecimal.ONE, desiredReturnPerYear).pow(3))).toString());
        latestPriceSales = new BigDecimal(bd.SetScaleTwo(bd.BDDivide(yf.getLastTradePriceOnly(), ms.getRevenuePerShareTTM())).toString());
    }

    // Getters and setters
    public BigDecimal getFixedEPSGrowth() { return fixedEPSGrowth; }
    public void setFixedEPSGrowth(BigDecimal v) { this.fixedEPSGrowth = v; }
    public BigDecimal getDesiredReturnPerYear() { return desiredReturnPerYear; }
    public void setDesiredReturnPerYear(BigDecimal v) { this.desiredReturnPerYear = v; }
    public BigDecimal getCorporateBondsYield() { return corporateBondsYield; }
    public void setCorporateBondsYield(BigDecimal v) { this.corporateBondsYield = v; }
    public BigDecimal getRateOfReturn() { return rateOfReturn; }
    public void setRateOfReturn(BigDecimal v) { this.rateOfReturn = v; }
    public BigDecimal getDifferenceFromPriceAtMinPSRatio() { return differenceFromPriceAtMinPSRatio; }
    public void setDifferenceFromPriceAtMinPSRatio(BigDecimal v) { this.differenceFromPriceAtMinPSRatio = v; }
    public BigDecimal getDifferenceFromPriceAtMaxPSRatio() { return differenceFromPriceAtMaxPSRatio; }
    public void setDifferenceFromPriceAtMaxPSRatio(BigDecimal v) { this.differenceFromPriceAtMaxPSRatio = v; }
    public BigDecimal getGrowthMultiple() { return growthMultiple; }
    public void setGrowthMultiple(BigDecimal v) { this.growthMultiple = v; }
    public BigDecimal getFiveYearGrowthMultiple() { return fiveYearGrowthMultiple; }
    public void setFiveYearGrowthMultiple(BigDecimal v) { this.fiveYearGrowthMultiple = v; }
    public BigDecimal getYearLowDifference() { return yearLowDifference; }
    public void setYearLowDifference(BigDecimal v) { this.yearLowDifference = v; }
    public BigDecimal getYearsRangeDifference() { return yearsRangeDifference; }
    public void setYearsRangeDifference(BigDecimal v) { this.yearsRangeDifference = v; }
    public BigDecimal getCompoundAnnualGrowthRate() { return compoundAnnualGrowthRate; }
    public void setCompoundAnnualGrowthRate(BigDecimal v) { this.compoundAnnualGrowthRate = v; }
    public BigDecimal getFoolEPSGrowth() { return foolEPSGrowth; }
    public void setFoolEPSGrowth(BigDecimal v) { this.foolEPSGrowth = v; }
    public BigDecimal getIntrinsicValue() { return intrinsicValue; }
    public void setIntrinsicValue(BigDecimal v) { this.intrinsicValue = v; }
    public BigDecimal getGrahamMarginOfSafety() { return grahamMarginOfSafety; }
    public void setGrahamMarginOfSafety(BigDecimal v) { this.grahamMarginOfSafety = v; }
    public BigDecimal getBuffettMarginOfSafety() { return buffettMarginOfSafety; }
    public void setBuffettMarginOfSafety(BigDecimal v) { this.buffettMarginOfSafety = v; }
    public BigDecimal getPERatioTTM() { return peRatioTTM; }
    public void setPERatioTTM(BigDecimal v) { this.peRatioTTM = v; }
    public BigDecimal getForwardPERatio() { return forwardPERatio; }
    public void setForwardPERatio(BigDecimal v) { this.forwardPERatio = v; }
    public BigDecimal getAssumedForwardPE() { return assumedForwardPE; }
    public void setAssumedForwardPE(BigDecimal v) { this.assumedForwardPE = v; }
    public BigDecimal getEPSOverHoldingPeriodYearOne() { return epsOverHoldingPeriodYearOne; }
    public void setEPSOverHoldingPeriodYearOne(BigDecimal v) { this.epsOverHoldingPeriodYearOne = v; }
    public BigDecimal getEPSOverHoldingPeriodYearTwo() { return epsOverHoldingPeriodYearTwo; }
    public void setEPSOverHoldingPeriodYearTwo(BigDecimal v) { this.epsOverHoldingPeriodYearTwo = v; }
    public BigDecimal getEPSOverHoldingPeriodYearThree() { return epsOverHoldingPeriodYearThree; }
    public void setEPSOverHoldingPeriodYearThree(BigDecimal v) { this.epsOverHoldingPeriodYearThree = v; }
    public BigDecimal getEPSOverHoldingPeriodTotal() { return epsOverHoldingPeriodTotal; }
    public void setEPSOverHoldingPeriodTotal(BigDecimal v) { this.epsOverHoldingPeriodTotal = v; }
    public BigDecimal getExpectedSharePriceInThreeYears() { return expectedSharePriceInThreeYears; }
    public void setExpectedSharePriceInThreeYears(BigDecimal v) { this.expectedSharePriceInThreeYears = v; }
    public BigDecimal getDividendPayoutRatio() { return dividendPayoutRatio; }
    public void setDividendPayoutRatio(BigDecimal v) { this.dividendPayoutRatio = v; }
    public BigDecimal getTotalDividendsPerShareOverThreeYears() { return totalDividendsPerShareOverThreeYears; }
    public void setTotalDividendsPerShareOverThreeYears(BigDecimal v) { this.totalDividendsPerShareOverThreeYears = v; }
    public BigDecimal getExpectedShareValueAtEndOfThreeYears() { return expectedShareValueAtEndOfThreeYears; }
    public void setExpectedShareValueAtEndOfThreeYears(BigDecimal v) { this.expectedShareValueAtEndOfThreeYears = v; }
    public BigDecimal getPresentShareValueForGoodValue() { return presentShareValueForGoodValue; }
    public void setPresentShareValueForGoodValue(BigDecimal v) { this.presentShareValueForGoodValue = v; }
    public BigDecimal getLatestPriceSales() { return latestPriceSales; }
    public void setLatestPriceSales(BigDecimal v) { this.latestPriceSales = v; }
    public BigDecimal getEPSYearFive() { return epsYearFive; }
    public void setEPSYearFive(BigDecimal v) { this.epsYearFive = v; }
    public BigDecimal getEPSYearOne() { return epsYearOne; }
    public void setEPSYearOne(BigDecimal v) { this.epsYearOne = v; }
    public BigDecimal getEPSEstimateNextYear() { return epsEstimateNextYear; }
    public void setEPSEstimateNextYear(BigDecimal v) { this.epsEstimateNextYear = v; }
    public BigDecimal getEPSGrowth() { return epsGrowth; }
    public void setEPSGrowth(BigDecimal v) { this.epsGrowth = v; }
    public BigDecimal getMaxPSRatioThisQtr() { return maxPSRatioThisQtr; }
    public void setMaxPSRatioThisQtr(BigDecimal v) { this.maxPSRatioThisQtr = v; }
    public BigDecimal getMinPSRatioThisQtr() { return minPSRatioThisQtr; }
    public void setMinPSRatioThisQtr(BigDecimal v) { this.minPSRatioThisQtr = v; }
    public BigDecimal getMaxPSRatioLastQtr() { return maxPSRatioLastQtr; }
    public void setMaxPSRatioLastQtr(BigDecimal v) { this.maxPSRatioLastQtr = v; }
    public BigDecimal getMinPSRatioLastQtr() { return minPSRatioLastQtr; }
    public void setMinPSRatioLastQtr(BigDecimal v) { this.minPSRatioLastQtr = v; }
    public BigDecimal getPriceAtMaxPSRatioThisQtr() { return priceAtMaxPSRatioThisQtr; }
    public void setPriceAtMaxPSRatioThisQtr(BigDecimal v) { this.priceAtMaxPSRatioThisQtr = v; }
    public BigDecimal getPriceAtMaxPSRatioLastQtr() { return priceAtMaxPSRatioLastQtr; }
    public void setPriceAtMaxPSRatioLastQtr(BigDecimal v) { this.priceAtMaxPSRatioLastQtr = v; }
    public BigDecimal getPriceAtMinPSRatioThisQtr() { return priceAtMinPSRatioThisQtr; }
    public void setPriceAtMinPSRatioThisQtr(BigDecimal v) { this.priceAtMinPSRatioThisQtr = v; }
    public BigDecimal getPriceAtMinPSRatioLastQtr() { return priceAtMinPSRatioLastQtr; }
    public void setPriceAtMinPSRatioLastQtr(BigDecimal v) { this.priceAtMinPSRatioLastQtr = v; }
    public double getFiveYearPeriod() { return fiveYearPeriod; }
    public void setFiveYearPeriod(double v) { this.fiveYearPeriod = v; }
}
