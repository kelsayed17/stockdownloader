package com.stockdownloader.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Unified market data snapshot that combines equity price data, options chain data,
 * and volume analytics into a single coherent view for a given symbol and date.
 * This is the central data model that ties all financial data together for
 * backtesting and analysis.
 */
public final class UnifiedMarketData {

    private final String symbol;
    private final String date;
    private final PriceData priceData;
    private final VolumeProfile volumeProfile;
    private final OptionsChain optionsChain;

    public UnifiedMarketData(String symbol, String date, PriceData priceData,
                             VolumeProfile volumeProfile, OptionsChain optionsChain) {
        this.symbol = Objects.requireNonNull(symbol);
        this.date = Objects.requireNonNull(date);
        this.priceData = Objects.requireNonNull(priceData);
        this.volumeProfile = volumeProfile; // nullable if no volume analysis available
        this.optionsChain = optionsChain;   // nullable if no options data available
    }

    public BigDecimal getUnderlyingPrice() {
        return priceData.close();
    }

    public boolean hasOptionsData() {
        return optionsChain != null && !optionsChain.getCalls().isEmpty();
    }

    public boolean hasVolumeProfile() {
        return volumeProfile != null;
    }

    public String getSymbol() { return symbol; }
    public String getDate() { return date; }
    public PriceData getPriceData() { return priceData; }
    public VolumeProfile getVolumeProfile() { return volumeProfile; }
    public OptionsChain getOptionsChain() { return optionsChain; }

    /**
     * Builder for constructing UnifiedMarketData from individual data sources.
     * Ties price, volume, and options data together for a symbol on a date.
     */
    public static final class Builder {
        private String symbol;
        private String date;
        private PriceData priceData;
        private OptionsChain optionsChain;
        private List<PriceData> historicalPrices;
        private int currentIndex;

        public Builder symbol(String symbol) { this.symbol = symbol; return this; }
        public Builder date(String date) { this.date = date; return this; }
        public Builder priceData(PriceData priceData) { this.priceData = priceData; return this; }
        public Builder optionsChain(OptionsChain optionsChain) { this.optionsChain = optionsChain; return this; }
        public Builder historicalPrices(List<PriceData> prices, int currentIndex) {
            this.historicalPrices = prices;
            this.currentIndex = currentIndex;
            return this;
        }

        public UnifiedMarketData build() {
            Objects.requireNonNull(symbol, "symbol required");
            Objects.requireNonNull(date, "date required");
            Objects.requireNonNull(priceData, "priceData required");

            VolumeProfile volumeProfile = buildVolumeProfile();
            return new UnifiedMarketData(symbol, date, priceData, volumeProfile, optionsChain);
        }

        private VolumeProfile buildVolumeProfile() {
            BigDecimal avgVol20 = BigDecimal.ZERO;
            if (historicalPrices != null && currentIndex >= 0) {
                avgVol20 = VolumeProfile.computeAvgVolume(historicalPrices, currentIndex, 20);
            }

            long callVol = 0, putVol = 0, callOI = 0, putOI = 0;
            if (optionsChain != null) {
                callVol = optionsChain.getTotalCallVolume();
                putVol = optionsChain.getTotalPutVolume();
                callOI = optionsChain.getTotalCallOpenInterest();
                putOI = optionsChain.getTotalPutOpenInterest();
            }

            return new VolumeProfile(date, symbol, priceData.volume(),
                    callVol, putVol, callOI, putOI, avgVol20);
        }
    }

    @Override
    public String toString() {
        String optStr = hasOptionsData()
                ? " Opts:%d/%d".formatted(optionsChain.getCalls().size(), optionsChain.getPuts().size())
                : " NoOpts";
        String volStr = hasVolumeProfile()
                ? " RVOL:%.2f P/C:%.2f".formatted(
                        volumeProfile.getRelativeVolume().doubleValue(),
                        volumeProfile.getPutCallVolumeRatio().doubleValue())
                : "";
        return "Unified[%s %s $%s V:%d%s%s]".formatted(
                symbol, date,
                priceData.close().setScale(2, RoundingMode.HALF_UP),
                priceData.volume(), optStr, volStr);
    }
}
