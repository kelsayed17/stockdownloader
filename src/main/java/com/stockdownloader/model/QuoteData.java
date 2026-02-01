package com.stockdownloader.model;

import java.math.BigDecimal;

/**
 * Real-time stock quote data model with price, volume, and valuation fields.
 */
public class QuoteData {

    private BigDecimal priceSales = BigDecimal.ZERO;
    private BigDecimal trailingAnnualDividendYield = BigDecimal.ZERO;
    private BigDecimal dilutedEPS = BigDecimal.ZERO;
    private BigDecimal epsEstimateNextYear = BigDecimal.ZERO;
    private BigDecimal lastTradePriceOnly = BigDecimal.ZERO;
    private BigDecimal yearHigh = BigDecimal.ZERO;
    private BigDecimal yearLow = BigDecimal.ZERO;
    private BigDecimal fiftyDayMovingAverage = BigDecimal.ZERO;
    private BigDecimal twoHundredDayMovingAverage = BigDecimal.ZERO;
    private BigDecimal previousClose = BigDecimal.ZERO;
    private BigDecimal open = BigDecimal.ZERO;
    private BigDecimal daysHigh = BigDecimal.ZERO;
    private BigDecimal daysLow = BigDecimal.ZERO;
    private BigDecimal volume = BigDecimal.ZERO;
    private String yearRange = "";
    private String marketCapitalizationStr = "";
    private long marketCapitalization;
    private boolean incomplete;
    private boolean error;

    public BigDecimal getPriceSales() { return priceSales; }
    public void setPriceSales(BigDecimal v) { this.priceSales = v; }
    public BigDecimal getTrailingAnnualDividendYield() { return trailingAnnualDividendYield; }
    public void setTrailingAnnualDividendYield(BigDecimal v) { this.trailingAnnualDividendYield = v; }
    public BigDecimal getDilutedEPS() { return dilutedEPS; }
    public void setDilutedEPS(BigDecimal v) { this.dilutedEPS = v; }
    public BigDecimal getEpsEstimateNextYear() { return epsEstimateNextYear; }
    public void setEpsEstimateNextYear(BigDecimal v) { this.epsEstimateNextYear = v; }
    public BigDecimal getLastTradePriceOnly() { return lastTradePriceOnly; }
    public void setLastTradePriceOnly(BigDecimal v) { this.lastTradePriceOnly = v; }
    public BigDecimal getYearHigh() { return yearHigh; }
    public void setYearHigh(BigDecimal v) { this.yearHigh = v; }
    public BigDecimal getYearLow() { return yearLow; }
    public void setYearLow(BigDecimal v) { this.yearLow = v; }
    public BigDecimal getFiftyDayMovingAverage() { return fiftyDayMovingAverage; }
    public void setFiftyDayMovingAverage(BigDecimal v) { this.fiftyDayMovingAverage = v; }
    public BigDecimal getTwoHundredDayMovingAverage() { return twoHundredDayMovingAverage; }
    public void setTwoHundredDayMovingAverage(BigDecimal v) { this.twoHundredDayMovingAverage = v; }
    public BigDecimal getPreviousClose() { return previousClose; }
    public void setPreviousClose(BigDecimal v) { this.previousClose = v; }
    public BigDecimal getOpen() { return open; }
    public void setOpen(BigDecimal v) { this.open = v; }
    public BigDecimal getDaysHigh() { return daysHigh; }
    public void setDaysHigh(BigDecimal v) { this.daysHigh = v; }
    public BigDecimal getDaysLow() { return daysLow; }
    public void setDaysLow(BigDecimal v) { this.daysLow = v; }
    public BigDecimal getVolume() { return volume; }
    public void setVolume(BigDecimal v) { this.volume = v; }
    public String getYearRange() { return yearRange; }
    public void setYearRange(String v) { this.yearRange = v; }
    public String getMarketCapitalizationStr() { return marketCapitalizationStr; }
    public void setMarketCapitalizationStr(String v) { this.marketCapitalizationStr = v; }
    public long getMarketCapitalization() { return marketCapitalization; }
    public void setMarketCapitalization(long v) { this.marketCapitalization = v; }
    public boolean isIncomplete() { return incomplete; }
    public void setIncomplete(boolean v) { this.incomplete = v; }
    public boolean isError() { return error; }
    public void setError(boolean v) { this.error = v; }
}
