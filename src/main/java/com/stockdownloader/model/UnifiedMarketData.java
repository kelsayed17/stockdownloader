package com.stockdownloader.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Unified view of all financial data for a single symbol, consolidating
 * price data, quote data, historical data, financial data, and options chain
 * into a single cohesive model. All volume metrics are captured and accessible
 * from a single point.
 */
public final class UnifiedMarketData {

    private final String symbol;
    private PriceData latestPrice;
    private List<PriceData> priceHistory;
    private QuoteData quote;
    private HistoricalData historical;
    private FinancialData financials;
    private OptionsChain optionsChain;

    public UnifiedMarketData(String symbol) {
        this.symbol = Objects.requireNonNull(symbol, "symbol must not be null");
        this.priceHistory = List.of();
    }

    // --- Volume aggregation (unified across all data sources) ---

    /**
     * Latest equity trading volume from quote data.
     */
    public BigDecimal getEquityVolume() {
        return quote != null ? quote.getVolume() : BigDecimal.ZERO;
    }

    /**
     * Total options volume (calls + puts) across all expirations.
     */
    public long getOptionsVolume() {
        return optionsChain != null ? optionsChain.getTotalVolume() : 0;
    }

    /**
     * Total call volume across all expirations.
     */
    public long getCallVolume() {
        return optionsChain != null ? optionsChain.getTotalCallVolume() : 0;
    }

    /**
     * Total put volume across all expirations.
     */
    public long getPutVolume() {
        return optionsChain != null ? optionsChain.getTotalPutVolume() : 0;
    }

    /**
     * Combined volume: equity trading volume + options volume.
     */
    public BigDecimal getTotalCombinedVolume() {
        return getEquityVolume().add(BigDecimal.valueOf(getOptionsVolume()));
    }

    /**
     * Average daily equity volume from price history.
     */
    public BigDecimal getAverageDailyVolume(int days) {
        if (priceHistory == null || priceHistory.isEmpty()) return BigDecimal.ZERO;
        int limit = Math.min(days, priceHistory.size());
        long totalVol = 0;
        for (int i = priceHistory.size() - limit; i < priceHistory.size(); i++) {
            totalVol += priceHistory.get(i).volume();
        }
        return BigDecimal.valueOf(totalVol)
                .divide(BigDecimal.valueOf(limit), 0, RoundingMode.HALF_UP);
    }

    /**
     * Total open interest across all options.
     */
    public long getTotalOpenInterest() {
        if (optionsChain == null) return 0;
        return optionsChain.getTotalCallOpenInterest() + optionsChain.getTotalPutOpenInterest();
    }

    /**
     * Put/call ratio based on volume.
     */
    public BigDecimal getPutCallRatio() {
        return optionsChain != null ? optionsChain.getPutCallRatio() : BigDecimal.ZERO;
    }

    // --- Price metrics ---

    public BigDecimal getCurrentPrice() {
        if (quote != null && quote.getLastTradePriceOnly().compareTo(BigDecimal.ZERO) > 0) {
            return quote.getLastTradePriceOnly();
        }
        if (latestPrice != null) {
            return latestPrice.close();
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getMarketCap() {
        return quote != null ? BigDecimal.valueOf(quote.getMarketCapitalization()) : BigDecimal.ZERO;
    }

    // --- Completeness checks ---

    public boolean hasQuoteData() { return quote != null && !quote.isError(); }
    public boolean hasPriceHistory() { return priceHistory != null && !priceHistory.isEmpty(); }
    public boolean hasFinancialData() { return financials != null && !financials.isError(); }
    public boolean hasOptionsChain() { return optionsChain != null && !optionsChain.getExpirationDates().isEmpty(); }
    public boolean hasHistoricalData() { return historical != null && !historical.isError(); }

    public boolean isComplete() {
        return hasQuoteData() && hasPriceHistory() && hasFinancialData()
                && hasOptionsChain() && hasHistoricalData();
    }

    // --- Getters and setters ---

    public String getSymbol() { return symbol; }

    public PriceData getLatestPrice() { return latestPrice; }
    public void setLatestPrice(PriceData latestPrice) { this.latestPrice = latestPrice; }

    public List<PriceData> getPriceHistory() { return priceHistory; }
    public void setPriceHistory(List<PriceData> priceHistory) {
        this.priceHistory = priceHistory != null ? List.copyOf(priceHistory) : List.of();
    }

    public QuoteData getQuote() { return quote; }
    public void setQuote(QuoteData quote) { this.quote = quote; }

    public HistoricalData getHistorical() { return historical; }
    public void setHistorical(HistoricalData historical) { this.historical = historical; }

    public FinancialData getFinancials() { return financials; }
    public void setFinancials(FinancialData financials) { this.financials = financials; }

    public OptionsChain getOptionsChain() { return optionsChain; }
    public void setOptionsChain(OptionsChain optionsChain) { this.optionsChain = optionsChain; }

    @Override
    public String toString() {
        return "UnifiedMarketData[%s] price=$%s eqVol=%s optVol=%d OI=%d P/C=%.4f".formatted(
                symbol, getCurrentPrice(),
                getEquityVolume(), getOptionsVolume(),
                getTotalOpenInterest(), getPutCallRatio().doubleValue());
    }
}
