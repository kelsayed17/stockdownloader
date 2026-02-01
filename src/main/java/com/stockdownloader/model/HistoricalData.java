package com.stockdownloader.model;

import com.google.common.collect.HashMultimap;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Historical price data and derived pattern information for a stock ticker.
 */
public class HistoricalData {

    private final String ticker;

    private BigDecimal highestPriceThisQtr = BigDecimal.ZERO;
    private BigDecimal lowestPriceThisQtr = BigDecimal.ZERO;
    private BigDecimal highestPriceLastQtr = BigDecimal.ZERO;
    private BigDecimal lowestPriceLastQtr = BigDecimal.ZERO;

    private List<String> historicalPrices = new ArrayList<>();
    private HashMultimap<String, String> patterns = HashMultimap.create();

    private boolean incomplete;
    private boolean error;

    public HistoricalData(String ticker) {
        this.ticker = ticker;
    }

    public String getTicker() { return ticker; }

    public BigDecimal getHighestPriceThisQtr() { return highestPriceThisQtr; }
    public void setHighestPriceThisQtr(BigDecimal v) { this.highestPriceThisQtr = v; }
    public BigDecimal getLowestPriceThisQtr() { return lowestPriceThisQtr; }
    public void setLowestPriceThisQtr(BigDecimal v) { this.lowestPriceThisQtr = v; }
    public BigDecimal getHighestPriceLastQtr() { return highestPriceLastQtr; }
    public void setHighestPriceLastQtr(BigDecimal v) { this.highestPriceLastQtr = v; }
    public BigDecimal getLowestPriceLastQtr() { return lowestPriceLastQtr; }
    public void setLowestPriceLastQtr(BigDecimal v) { this.lowestPriceLastQtr = v; }

    public List<String> getHistoricalPrices() { return List.copyOf(historicalPrices); }
    public void setHistoricalPrices(List<String> v) { this.historicalPrices = new ArrayList<>(v); }

    public HashMultimap<String, String> getPatterns() { return patterns; }
    public void setPatterns(HashMultimap<String, String> v) { this.patterns = v; }

    public boolean isIncomplete() { return incomplete; }
    public void setIncomplete(boolean v) { this.incomplete = v; }
    public boolean isError() { return error; }
    public void setError(boolean v) { this.error = v; }
}
