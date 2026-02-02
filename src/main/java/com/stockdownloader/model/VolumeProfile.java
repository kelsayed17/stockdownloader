package com.stockdownloader.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Volume profile analysis combining underlying equity volume with
 * options volume and open interest data for a given date.
 */
public record VolumeProfile(
        String date,
        String symbol,
        long equityVolume,
        long callVolume,
        long putVolume,
        long callOpenInterest,
        long putOpenInterest,
        BigDecimal averageEquityVolume20d) {

    public VolumeProfile {
        Objects.requireNonNull(date, "date must not be null");
        Objects.requireNonNull(symbol, "symbol must not be null");
        Objects.requireNonNull(averageEquityVolume20d, "averageEquityVolume20d must not be null");
    }

    public long getTotalOptionsVolume() {
        return callVolume + putVolume;
    }

    public long getTotalOpenInterest() {
        return callOpenInterest + putOpenInterest;
    }

    public BigDecimal getPutCallVolumeRatio() {
        if (callVolume == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(putVolume)
                .divide(BigDecimal.valueOf(callVolume), 4, RoundingMode.HALF_UP);
    }

    public BigDecimal getPutCallOIRatio() {
        if (callOpenInterest == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(putOpenInterest)
                .divide(BigDecimal.valueOf(callOpenInterest), 4, RoundingMode.HALF_UP);
    }

    public BigDecimal getRelativeVolume() {
        if (averageEquityVolume20d.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(equityVolume)
                .divide(averageEquityVolume20d, 4, RoundingMode.HALF_UP);
    }

    public boolean isHighVolume() {
        return getRelativeVolume().compareTo(BigDecimal.valueOf(1.5)) > 0;
    }

    public static BigDecimal computeAvgVolume(List<PriceData> data, int currentIndex, int period) {
        if (currentIndex < period - 1 || data.isEmpty()) return BigDecimal.ZERO;
        long sum = 0;
        int count = 0;
        for (int i = Math.max(0, currentIndex - period + 1); i <= currentIndex; i++) {
            sum += data.get(i).volume();
            count++;
        }
        if (count == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return "%s %s EqVol:%d CallVol:%d PutVol:%d P/C:%.2f RVOL:%.2f".formatted(
                date, symbol, equityVolume, callVolume, putVolume,
                getPutCallVolumeRatio().doubleValue(),
                getRelativeVolume().doubleValue());
    }
}
