import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class FormulaData {
    private BigDecimal fixedEPSGrowth;
    private BigDecimal desiredReturnPerYear;
    private BigDecimal corporateBondsYield;
    private BigDecimal rateOfReturn;
    private BigDecimal differenceFromPriceAtMinPSRatio;
    private BigDecimal differenceFromPriceAtMaxPSRatio;
    private BigDecimal growthMultiple;
    private BigDecimal fiveYearGrowthMultiple;
    private BigDecimal yearLowDifference;
    private BigDecimal yearsRangeDifference;
    private BigDecimal compoundAnnualGrowthRate;
    private BigDecimal foolEPSGrowth;
    private BigDecimal intrinsicValue;
    private BigDecimal grahamMarginOfSafety;
    private BigDecimal buffettMarginOfSafety;
    private BigDecimal PERatioTTM;
    private BigDecimal forwardPERatio;
    private BigDecimal assumedForwardPE;
    private BigDecimal EPSOverHoldingPeriodYearOne;
    private BigDecimal EPSOverHoldingPeriodYearTwo;
    private BigDecimal EPSOverHoldingPeriodYearThree;
    private BigDecimal EPSOverHoldingPeriodTotal;
    private BigDecimal expectedSharePriceInThreeYears;
    private BigDecimal dividendPayoutRatio;
    private BigDecimal totalDividendsPerShareOverThreeYears;
    private BigDecimal expectedShareValueAtEndOfThreeYears;
    private BigDecimal presentShareValueForGoodValue;
    private BigDecimal latestPriceSales;
    private BigDecimal EPSYearFive;
    private BigDecimal EPSYearOne;
    private BigDecimal EPSEstimateNextYear;
    private BigDecimal EPSGrowth;
    private BigDecimal maxPSRatioThisQtr;
    private BigDecimal minPSRatioThisQtr;
    private BigDecimal maxPSRatioLastQtr;
    private BigDecimal minPSRatioLastQtr;
    private BigDecimal priceAtMaxPSRatioThisQtr;
    private BigDecimal priceAtMaxPSRatioLastQtr;
    private BigDecimal priceAtMinPSRatioThisQtr;
    private BigDecimal priceAtMinPSRatioLastQtr;
    private double fiveYearPeriod = 0;

    public FormulaData() {
        fixedEPSGrowth = new BigDecimal("0.00");
        desiredReturnPerYear = new BigDecimal("0.00");
        corporateBondsYield = new BigDecimal("0.00");
        rateOfReturn = new BigDecimal("0.00");
        differenceFromPriceAtMinPSRatio = new BigDecimal("0.00");
        differenceFromPriceAtMaxPSRatio = new BigDecimal("0.00");
        growthMultiple = new BigDecimal("0.00");
        fiveYearGrowthMultiple = new BigDecimal("0.00");
        yearLowDifference = new BigDecimal("0.00");
        yearsRangeDifference = new BigDecimal("0.00");
        compoundAnnualGrowthRate = new BigDecimal("0.00");
        foolEPSGrowth = new BigDecimal("0.00");
        intrinsicValue = new BigDecimal("0.00");
        grahamMarginOfSafety = new BigDecimal("0.00");
        buffettMarginOfSafety = new BigDecimal("0.00");
        PERatioTTM = new BigDecimal("0.00");
        forwardPERatio = new BigDecimal("0.00");
        assumedForwardPE = new BigDecimal("0.00");
        EPSOverHoldingPeriodYearOne = new BigDecimal("0.00");
        EPSOverHoldingPeriodYearTwo = new BigDecimal("0.00");
        EPSOverHoldingPeriodYearThree = new BigDecimal("0.00");
        EPSOverHoldingPeriodTotal = new BigDecimal("0.00");
        expectedSharePriceInThreeYears = new BigDecimal("0.00");
        dividendPayoutRatio = new BigDecimal("0.00");
        totalDividendsPerShareOverThreeYears = new BigDecimal("0.00");
        expectedShareValueAtEndOfThreeYears = new BigDecimal("0.00");
        presentShareValueForGoodValue = new BigDecimal("0.00");
        latestPriceSales = new BigDecimal("0.00");
        EPSYearFive = new BigDecimal("0.00");
        EPSYearOne = new BigDecimal("0.00");
        EPSEstimateNextYear = new BigDecimal("0.00");
        EPSGrowth = new BigDecimal("0.00");
        maxPSRatioThisQtr = new BigDecimal("0.00");
        minPSRatioThisQtr = new BigDecimal("0.00");
        maxPSRatioLastQtr = new BigDecimal("0.00");
        minPSRatioLastQtr = new BigDecimal("0.00");
        priceAtMaxPSRatioThisQtr = new BigDecimal("0.00");
        priceAtMaxPSRatioLastQtr = new BigDecimal("0.00");
        priceAtMinPSRatioThisQtr = new BigDecimal("0.00");
        priceAtMinPSRatioLastQtr = new BigDecimal("0.00");
    }

    public FormulaData(YahooFinanceData yf, MorningstarData ms, YahooHistoricalData yh) {
        BDCalculator bd = new BDCalculator();
        DecimalFormat RoundTenDecimals = new DecimalFormat("#.##########");

        MathContext mc = new MathContext(2);

        BigDecimal RevenuePerShareTTM = ms.getRevenuePerShareTTM();
        BigDecimal RevenuePerShareTTMLastQtr = ms.getRevenuePerShareTTMLastQtr();

        maxPSRatioThisQtr = yh.getHighestPriceThisQtr().divide(RevenuePerShareTTM, 2, RoundingMode.CEILING);
        minPSRatioThisQtr = yh.getLowestPriceThisQtr().divide(RevenuePerShareTTM, 2, RoundingMode.CEILING);
        maxPSRatioLastQtr = yh.getHighestPriceLastQtr().divide(RevenuePerShareTTMLastQtr, 2, RoundingMode.CEILING);
        minPSRatioLastQtr = yh.getLowestPriceLastQtr().divide(RevenuePerShareTTMLastQtr, 2, RoundingMode.CEILING);

        if (maxPSRatioThisQtr.compareTo(maxPSRatioLastQtr) == 1)
            priceAtMaxPSRatioThisQtr = maxPSRatioThisQtr.multiply(RevenuePerShareTTM, mc);
        else
            priceAtMaxPSRatioThisQtr = maxPSRatioLastQtr.multiply(RevenuePerShareTTM, mc);

        if (maxPSRatioLastQtr.compareTo(maxPSRatioThisQtr) == 1)
            priceAtMaxPSRatioLastQtr = maxPSRatioLastQtr.multiply(RevenuePerShareTTMLastQtr, mc);
        else
            priceAtMaxPSRatioLastQtr = maxPSRatioThisQtr.multiply(RevenuePerShareTTMLastQtr, mc);

        if (minPSRatioThisQtr.compareTo(minPSRatioLastQtr) == -1)
            priceAtMinPSRatioThisQtr = minPSRatioThisQtr.multiply(RevenuePerShareTTM, mc);
        else
            priceAtMinPSRatioThisQtr = minPSRatioLastQtr.multiply(RevenuePerShareTTM, mc);

        if (minPSRatioLastQtr.compareTo(minPSRatioThisQtr) == -1)
            priceAtMinPSRatioLastQtr = minPSRatioLastQtr.multiply(RevenuePerShareTTMLastQtr, mc);
        else
            priceAtMinPSRatioLastQtr = minPSRatioThisQtr.multiply(RevenuePerShareTTMLastQtr, mc);

        // Fixed Rates (BigDecimal)
        fixedEPSGrowth = new BigDecimal(0.06);
        desiredReturnPerYear = new BigDecimal(0.05);
        corporateBondsYield = new BigDecimal(4.09);
        rateOfReturn = new BigDecimal(4.4);

        // Calculations (BigDecimal)
        differenceFromPriceAtMinPSRatio = new BigDecimal(bd.SetScaleTwo(bd.BDSubtract(BigDecimal.valueOf(1), bd.BDDivide(priceAtMinPSRatioThisQtr, yf.getLastTradePriceOnly()))).toString());
        differenceFromPriceAtMaxPSRatio = new BigDecimal(bd.SetScaleTwo(bd.BDSubtract(BigDecimal.valueOf(1), bd.BDDivide(yf.getLastTradePriceOnly(), priceAtMaxPSRatioThisQtr))).toString());
        growthMultiple = new BigDecimal(bd.SetScaleTwo(bd.BDDivide(EPSYearFive, EPSYearOne)).toString());
        fiveYearGrowthMultiple = new BigDecimal(bd.SetScaleTwo(BigDecimal.valueOf(Double.parseDouble(RoundTenDecimals.format(Math.pow(Math.abs(growthMultiple.abs().doubleValue()), fiveYearPeriod))))).toString());
        yearLowDifference = new BigDecimal(bd.SetScaleTwo(bd.BDSubtract(BigDecimal.valueOf(1), bd.BDDivide(yf.getYearLow(), yf.getLastTradePriceOnly()))).toString());
        yearsRangeDifference = new BigDecimal(bd.SetScaleTwo(bd.BDSubtract(yf.getYearHigh(), yf.getYearLow())).toString());
        compoundAnnualGrowthRate = new BigDecimal(bd.SetScaleTwo(bd.BDMultiply(bd.BDSubtract(fiveYearGrowthMultiple, BigDecimal.valueOf(1)), BigDecimal.valueOf(100))).toString());
        foolEPSGrowth = new BigDecimal(bd.SetScaleTwo(bd.BDDivide(bd.BDSubtract(EPSEstimateNextYear, yf.getDilutedEPS()), yf.getDilutedEPS())).toString());
        intrinsicValue = new BigDecimal(bd.SetScaleTwo(bd.BDDivide(bd.BDMultiply(bd.BDMultiply(yf.getDilutedEPS(), bd.BDAdd(BigDecimal.valueOf(8.5), bd.BDMultiply(BigDecimal.valueOf(2), bd.BDMultiply(EPSGrowth, BigDecimal.valueOf(100))))), rateOfReturn), corporateBondsYield)).toString());
        grahamMarginOfSafety = new BigDecimal(bd.SetScaleTwo(bd.BDDivide(intrinsicValue, yf.getLastTradePriceOnly())).toString());
        buffettMarginOfSafety = new BigDecimal(bd.SetScaleTwo(bd.BDMultiply(intrinsicValue, BigDecimal.valueOf(0.75))).toString());
        PERatioTTM = new BigDecimal(bd.SetScaleTwo(bd.BDDivide(yf.getLastTradePriceOnly(), yf.getDilutedEPS())).toString());
        forwardPERatio = new BigDecimal(bd.SetScaleTwo(bd.BDDivide(yf.getLastTradePriceOnly(), EPSEstimateNextYear)).toString());
        assumedForwardPE = new BigDecimal(bd.SetScaleTwo(bd.AverageCalculator(PERatioTTM, forwardPERatio, BigDecimal.valueOf(0))).toString());
        EPSOverHoldingPeriodYearOne = new BigDecimal(bd.SetScaleTwo(bd.BDMultiply(yf.getDilutedEPS(), bd.BDAdd(EPSGrowth, BigDecimal.valueOf(1)))).toString());
        EPSOverHoldingPeriodYearTwo = new BigDecimal(bd.SetScaleTwo(bd.BDMultiply(EPSOverHoldingPeriodYearOne, bd.BDAdd(EPSGrowth, BigDecimal.valueOf(1)))).toString());
        EPSOverHoldingPeriodYearThree = new BigDecimal(bd.SetScaleTwo(bd.BDMultiply(EPSOverHoldingPeriodYearTwo, bd.BDAdd(EPSGrowth, BigDecimal.valueOf(1)))).toString());
        EPSOverHoldingPeriodTotal = new BigDecimal(bd.SetScaleTwo(bd.BDAdd(EPSOverHoldingPeriodYearOne, bd.BDAdd(EPSOverHoldingPeriodYearTwo, EPSOverHoldingPeriodYearThree))).toString());
        expectedSharePriceInThreeYears = new BigDecimal(bd.SetScaleTwo(bd.BDMultiply(EPSOverHoldingPeriodYearThree, assumedForwardPE)).toString());
        dividendPayoutRatio = new BigDecimal(bd.SetScaleTwo(bd.BDDivide(yf.getTrailingAnnualDividendYield(), EPSOverHoldingPeriodYearThree)).toString());
        totalDividendsPerShareOverThreeYears = new BigDecimal(bd.SetScaleTwo(bd.BDMultiply(dividendPayoutRatio, EPSOverHoldingPeriodTotal)).toString());
        expectedShareValueAtEndOfThreeYears = new BigDecimal(bd.SetScaleTwo(bd.BDAdd(totalDividendsPerShareOverThreeYears, expectedSharePriceInThreeYears)).toString());
        presentShareValueForGoodValue = new BigDecimal(bd.SetScaleTwo(bd.BDDivide(expectedShareValueAtEndOfThreeYears, bd.BDAdd(BigDecimal.valueOf(1), desiredReturnPerYear).pow(3))).toString());
        latestPriceSales = new BigDecimal(bd.SetScaleTwo(bd.BDDivide(yf.getLastTradePriceOnly(), ms.getRevenuePerShareTTM())).toString());
    }

    public BigDecimal getFixedEPSGrowth() {
        return fixedEPSGrowth;
    }

    public void setFixedEPSGrowth(BigDecimal fixedEPSGrowth) {
        this.fixedEPSGrowth = fixedEPSGrowth;
    }

    public BigDecimal getDesiredReturnPerYear() {
        return desiredReturnPerYear;
    }

    public void setDesiredReturnPerYear(BigDecimal desiredReturnPerYear) {
        this.desiredReturnPerYear = desiredReturnPerYear;
    }

    public BigDecimal getCorporateBondsYield() {
        return corporateBondsYield;
    }

    public void setCorporateBondsYield(BigDecimal corporateBondsYield) {
        this.corporateBondsYield = corporateBondsYield;
    }

    public BigDecimal getRateOfReturn() {
        return rateOfReturn;
    }

    public void setRateOfReturn(BigDecimal rateOfReturn) {
        this.rateOfReturn = rateOfReturn;
    }

    public BigDecimal getDifferenceFromPriceAtMinPSRatio() {
        return differenceFromPriceAtMinPSRatio;
    }

    public void setDifferenceFromPriceAtMinPSRatio(BigDecimal differenceFromPriceAtMinPSRatio) {
        this.differenceFromPriceAtMinPSRatio = differenceFromPriceAtMinPSRatio;
    }

    public BigDecimal getDifferenceFromPriceAtMaxPSRatio() {
        return differenceFromPriceAtMaxPSRatio;
    }

    public void setDifferenceFromPriceAtMaxPSRatio(BigDecimal differenceFromPriceAtMaxPSRatio) {
        this.differenceFromPriceAtMaxPSRatio = differenceFromPriceAtMaxPSRatio;
    }

    public BigDecimal getGrowthMultiple() {
        return growthMultiple;
    }

    public void setGrowthMultiple(BigDecimal growthMultiple) {
        this.growthMultiple = growthMultiple;
    }

    public BigDecimal getFiveYearGrowthMultiple() {
        return fiveYearGrowthMultiple;
    }

    public void setFiveYearGrowthMultiple(BigDecimal fiveYearGrowthMultiple) {
        this.fiveYearGrowthMultiple = fiveYearGrowthMultiple;
    }

    public BigDecimal getYearLowDifference() {
        return yearLowDifference;
    }

    public void setYearLowDifference(BigDecimal yearLowDifference) {
        this.yearLowDifference = yearLowDifference;
    }

    public BigDecimal getYearsRangeDifference() {
        return yearsRangeDifference;
    }

    public void setYearsRangeDifference(BigDecimal yearsRangeDifference) {
        this.yearsRangeDifference = yearsRangeDifference;
    }

    public BigDecimal getCompoundAnnualGrowthRate() {
        return compoundAnnualGrowthRate;
    }

    public void setCompoundAnnualGrowthRate(BigDecimal compoundAnnualGrowthRate) {
        this.compoundAnnualGrowthRate = compoundAnnualGrowthRate;
    }

    public BigDecimal getFoolEPSGrowth() {
        return foolEPSGrowth;
    }

    public void setFoolEPSGrowth(BigDecimal foolEPSGrowth) {
        this.foolEPSGrowth = foolEPSGrowth;
    }

    public BigDecimal getIntrinsicValue() {
        return intrinsicValue;
    }

    public void setIntrinsicValue(BigDecimal intrinsicValue) {
        this.intrinsicValue = intrinsicValue;
    }

    public BigDecimal getGrahamMarginOfSafety() {
        return grahamMarginOfSafety;
    }

    public void setGrahamMarginOfSafety(BigDecimal grahamMarginOfSafety) {
        this.grahamMarginOfSafety = grahamMarginOfSafety;
    }

    public BigDecimal getBuffettMarginOfSafety() {
        return buffettMarginOfSafety;
    }

    public void setBuffettMarginOfSafety(BigDecimal buffettMarginOfSafety) {
        this.buffettMarginOfSafety = buffettMarginOfSafety;
    }

    public BigDecimal getPERatioTTM() {
        return PERatioTTM;
    }

    public void setPERatioTTM(BigDecimal PERatioTTM) {
        this.PERatioTTM = PERatioTTM;
    }

    public BigDecimal getForwardPERatio() {
        return forwardPERatio;
    }

    public void setForwardPERatio(BigDecimal forwardPERatio) {
        this.forwardPERatio = forwardPERatio;
    }

    public BigDecimal getAssumedForwardPE() {
        return assumedForwardPE;
    }

    public void setAssumedForwardPE(BigDecimal assumedForwardPE) {
        this.assumedForwardPE = assumedForwardPE;
    }

    public BigDecimal getEPSOverHoldingPeriodYearOne() {
        return EPSOverHoldingPeriodYearOne;
    }

    public void setEPSOverHoldingPeriodYearOne(BigDecimal EPSOverHoldingPeriodYearOne) {
        this.EPSOverHoldingPeriodYearOne = EPSOverHoldingPeriodYearOne;
    }

    public BigDecimal getEPSOverHoldingPeriodYearTwo() {
        return EPSOverHoldingPeriodYearTwo;
    }

    public void setEPSOverHoldingPeriodYearTwo(BigDecimal EPSOverHoldingPeriodYearTwo) {
        this.EPSOverHoldingPeriodYearTwo = EPSOverHoldingPeriodYearTwo;
    }

    public BigDecimal getEPSOverHoldingPeriodYearThree() {
        return EPSOverHoldingPeriodYearThree;
    }

    public void setEPSOverHoldingPeriodYearThree(BigDecimal EPSOverHoldingPeriodYearThree) {
        this.EPSOverHoldingPeriodYearThree = EPSOverHoldingPeriodYearThree;
    }

    public BigDecimal getEPSOverHoldingPeriodTotal() {
        return EPSOverHoldingPeriodTotal;
    }

    public void setEPSOverHoldingPeriodTotal(BigDecimal EPSOverHoldingPeriodTotal) {
        this.EPSOverHoldingPeriodTotal = EPSOverHoldingPeriodTotal;
    }

    public BigDecimal getExpectedSharePriceInThreeYears() {
        return expectedSharePriceInThreeYears;
    }

    public void setExpectedSharePriceInThreeYears(BigDecimal expectedSharePriceInThreeYears) {
        this.expectedSharePriceInThreeYears = expectedSharePriceInThreeYears;
    }

    public BigDecimal getDividendPayoutRatio() {
        return dividendPayoutRatio;
    }

    public void setDividendPayoutRatio(BigDecimal dividendPayoutRatio) {
        this.dividendPayoutRatio = dividendPayoutRatio;
    }

    public BigDecimal getTotalDividendsPerShareOverThreeYears() {
        return totalDividendsPerShareOverThreeYears;
    }

    public void setTotalDividendsPerShareOverThreeYears(BigDecimal totalDividendsPerShareOverThreeYears) {
        this.totalDividendsPerShareOverThreeYears = totalDividendsPerShareOverThreeYears;
    }

    public BigDecimal getExpectedShareValueAtEndOfThreeYears() {
        return expectedShareValueAtEndOfThreeYears;
    }

    public void setExpectedShareValueAtEndOfThreeYears(BigDecimal expectedShareValueAtEndOfThreeYears) {
        this.expectedShareValueAtEndOfThreeYears = expectedShareValueAtEndOfThreeYears;
    }

    public BigDecimal getPresentShareValueForGoodValue() {
        return presentShareValueForGoodValue;
    }

    public void setPresentShareValueForGoodValue(BigDecimal presentShareValueForGoodValue) {
        this.presentShareValueForGoodValue = presentShareValueForGoodValue;
    }

    public BigDecimal getLatestPriceSales() {
        return latestPriceSales;
    }

    public void setLatestPriceSales(BigDecimal latestPriceSales) {
        this.latestPriceSales = latestPriceSales;
    }

    public BigDecimal getEPSYearFive() {
        return EPSYearFive;
    }

    public void setEPSYearFive(BigDecimal EPSYearFive) {
        this.EPSYearFive = EPSYearFive;
    }

    public BigDecimal getEPSYearOne() {
        return EPSYearOne;
    }

    public void setEPSYearOne(BigDecimal EPSYearOne) {
        this.EPSYearOne = EPSYearOne;
    }

    public BigDecimal getEPSEstimateNextYear() {
        return EPSEstimateNextYear;
    }

    public void setEPSEstimateNextYear(BigDecimal EPSEstimateNextYear) {
        this.EPSEstimateNextYear = EPSEstimateNextYear;
    }

    public BigDecimal getEPSGrowth() {
        return EPSGrowth;
    }

    public void setEPSGrowth(BigDecimal EPSGrowth) {
        this.EPSGrowth = EPSGrowth;
    }

    public BigDecimal getMaxPSRatioThisQtr() {
        return maxPSRatioThisQtr;
    }

    public void setMaxPSRatioThisQtr(BigDecimal maxPSRatioThisQtr) {
        this.maxPSRatioThisQtr = maxPSRatioThisQtr;
    }

    public BigDecimal getMinPSRatioThisQtr() {
        return minPSRatioThisQtr;
    }

    public void setMinPSRatioThisQtr(BigDecimal minPSRatioThisQtr) {
        this.minPSRatioThisQtr = minPSRatioThisQtr;
    }

    public BigDecimal getMaxPSRatioLastQtr() {
        return maxPSRatioLastQtr;
    }

    public void setMaxPSRatioLastQtr(BigDecimal maxPSRatioLastQtr) {
        this.maxPSRatioLastQtr = maxPSRatioLastQtr;
    }

    public BigDecimal getMinPSRatioLastQtr() {
        return minPSRatioLastQtr;
    }

    public void setMinPSRatioLastQtr(BigDecimal minPSRatioLastQtr) {
        this.minPSRatioLastQtr = minPSRatioLastQtr;
    }

    public BigDecimal getPriceAtMaxPSRatioThisQtr() {
        return priceAtMaxPSRatioThisQtr;
    }

    public void setPriceAtMaxPSRatioThisQtr(BigDecimal priceAtMaxPSRatioThisQtr) {
        this.priceAtMaxPSRatioThisQtr = priceAtMaxPSRatioThisQtr;
    }

    public BigDecimal getPriceAtMaxPSRatioLastQtr() {
        return priceAtMaxPSRatioLastQtr;
    }

    public void setPriceAtMaxPSRatioLastQtr(BigDecimal priceAtMaxPSRatioLastQtr) {
        this.priceAtMaxPSRatioLastQtr = priceAtMaxPSRatioLastQtr;
    }

    public BigDecimal getPriceAtMinPSRatioThisQtr() {
        return priceAtMinPSRatioThisQtr;
    }

    public void setPriceAtMinPSRatioThisQtr(BigDecimal priceAtMinPSRatioThisQtr) {
        this.priceAtMinPSRatioThisQtr = priceAtMinPSRatioThisQtr;
    }

    public BigDecimal getPriceAtMinPSRatioLastQtr() {
        return priceAtMinPSRatioLastQtr;
    }

    public void setPriceAtMinPSRatioLastQtr(BigDecimal priceAtMinPSRatioLastQtr) {
        this.priceAtMinPSRatioLastQtr = priceAtMinPSRatioLastQtr;
    }

    public double getFiveYearPeriod() {
        return fiveYearPeriod;
    }

    public void setFiveYearPeriod(double fiveYearPeriod) {
        this.fiveYearPeriod = fiveYearPeriod;
    }
}