package com.stockdownloader.analysis;

import com.stockdownloader.model.OptionsChain;
import com.stockdownloader.model.PriceData;
import com.stockdownloader.model.VolumeProfile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Analyzes volume across equity and options markets to identify
 * unusual activity, confirm trends, and provide volume-weighted signals.
 * Integrates equity volume with options volume and open interest data.
 */
public final class VolumeAnalyzer {

    private VolumeAnalyzer() {}

    /**
     * Builds volume profiles for each bar in the price data, optionally
     * incorporating options chain volume when available.
     */
    public static List<VolumeProfile> buildProfiles(String symbol, List<PriceData> data,
                                                     OptionsChain optionsChain) {
        Objects.requireNonNull(data);
        List<VolumeProfile> profiles = new ArrayList<>(data.size());

        for (int i = 0; i < data.size(); i++) {
            PriceData bar = data.get(i);
            BigDecimal avgVol = VolumeProfile.computeAvgVolume(data, i, 20);

            long callVol = 0, putVol = 0, callOI = 0, putOI = 0;
            if (optionsChain != null) {
                callVol = optionsChain.getTotalCallVolume();
                putVol = optionsChain.getTotalPutVolume();
                callOI = optionsChain.getTotalCallOpenInterest();
                putOI = optionsChain.getTotalPutOpenInterest();
            }

            profiles.add(new VolumeProfile(bar.date(), symbol, bar.volume(),
                    callVol, putVol, callOI, putOI, avgVol));
        }

        return profiles;
    }

    /**
     * Detects days with unusual volume (above threshold relative to 20-day average).
     */
    public static List<VolumeProfile> findUnusualVolume(List<VolumeProfile> profiles,
                                                         BigDecimal threshold) {
        return profiles.stream()
                .filter(p -> p.getRelativeVolume().compareTo(threshold) > 0)
                .toList();
    }

    /**
     * Calculates the On-Balance Volume (OBV) indicator for the price data.
     * OBV adds volume on up days and subtracts on down days.
     */
    public static List<Long> computeOBV(List<PriceData> data) {
        List<Long> obv = new ArrayList<>(data.size());
        if (data.isEmpty()) return obv;

        obv.add(data.getFirst().volume());

        for (int i = 1; i < data.size(); i++) {
            int cmp = data.get(i).close().compareTo(data.get(i - 1).close());
            long prevOBV = obv.get(i - 1);
            if (cmp > 0) {
                obv.add(prevOBV + data.get(i).volume());
            } else if (cmp < 0) {
                obv.add(prevOBV - data.get(i).volume());
            } else {
                obv.add(prevOBV);
            }
        }

        return obv;
    }

    /**
     * Computes Volume-Weighted Average Price (VWAP) for the dataset.
     */
    public static List<BigDecimal> computeVWAP(List<PriceData> data) {
        List<BigDecimal> vwap = new ArrayList<>(data.size());
        BigDecimal cumulativeTPV = BigDecimal.ZERO; // typical price * volume
        BigDecimal cumulativeVolume = BigDecimal.ZERO;

        for (PriceData bar : data) {
            BigDecimal typicalPrice = bar.high().add(bar.low()).add(bar.close())
                    .divide(BigDecimal.valueOf(3), 6, RoundingMode.HALF_UP);
            BigDecimal tpv = typicalPrice.multiply(BigDecimal.valueOf(bar.volume()));

            cumulativeTPV = cumulativeTPV.add(tpv);
            cumulativeVolume = cumulativeVolume.add(BigDecimal.valueOf(bar.volume()));

            if (cumulativeVolume.compareTo(BigDecimal.ZERO) > 0) {
                vwap.add(cumulativeTPV.divide(cumulativeVolume, 4, RoundingMode.HALF_UP));
            } else {
                vwap.add(bar.close());
            }
        }

        return vwap;
    }

    /**
     * Calculates the Money Flow Index (MFI) - a volume-weighted RSI.
     * Combines price and volume to identify overbought/oversold conditions.
     */
    public static List<BigDecimal> computeMFI(List<PriceData> data, int period) {
        List<BigDecimal> mfi = new ArrayList<>(data.size());

        for (int i = 0; i < data.size(); i++) {
            if (i < period) {
                mfi.add(BigDecimal.valueOf(50)); // neutral default
                continue;
            }

            BigDecimal positiveFlow = BigDecimal.ZERO;
            BigDecimal negativeFlow = BigDecimal.ZERO;

            for (int j = i - period + 1; j <= i; j++) {
                BigDecimal typicalPrice = data.get(j).high().add(data.get(j).low()).add(data.get(j).close())
                        .divide(BigDecimal.valueOf(3), 6, RoundingMode.HALF_UP);
                BigDecimal prevTypical = data.get(j - 1).high().add(data.get(j - 1).low()).add(data.get(j - 1).close())
                        .divide(BigDecimal.valueOf(3), 6, RoundingMode.HALF_UP);
                BigDecimal rawMF = typicalPrice.multiply(BigDecimal.valueOf(data.get(j).volume()));

                if (typicalPrice.compareTo(prevTypical) > 0) {
                    positiveFlow = positiveFlow.add(rawMF);
                } else if (typicalPrice.compareTo(prevTypical) < 0) {
                    negativeFlow = negativeFlow.add(rawMF);
                }
            }

            if (negativeFlow.compareTo(BigDecimal.ZERO) == 0) {
                mfi.add(BigDecimal.valueOf(100));
            } else {
                BigDecimal moneyRatio = positiveFlow.divide(negativeFlow, 6, RoundingMode.HALF_UP);
                BigDecimal mfiValue = BigDecimal.valueOf(100).subtract(
                        BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(moneyRatio), 4, RoundingMode.HALF_UP));
                mfi.add(mfiValue);
            }
        }

        return mfi;
    }

    /**
     * Summarizes volume statistics for the dataset.
     */
    public static String summarize(List<VolumeProfile> profiles) {
        if (profiles.isEmpty()) return "No volume data available";

        long totalEquityVol = profiles.stream().mapToLong(VolumeProfile::equityVolume).sum();
        long totalCallVol = profiles.stream().mapToLong(VolumeProfile::callVolume).sum();
        long totalPutVol = profiles.stream().mapToLong(VolumeProfile::putVolume).sum();
        long highVolDays = profiles.stream().filter(VolumeProfile::isHighVolume).count();

        BigDecimal avgPCR = BigDecimal.ZERO;
        long pcrCount = 0;
        for (VolumeProfile p : profiles) {
            if (p.callVolume() > 0) {
                avgPCR = avgPCR.add(p.getPutCallVolumeRatio());
                pcrCount++;
            }
        }
        if (pcrCount > 0) {
            avgPCR = avgPCR.divide(BigDecimal.valueOf(pcrCount), 4, RoundingMode.HALF_UP);
        }

        return """
                Volume Summary (%d days):
                  Total Equity Volume:   %,d
                  Total Call Volume:     %,d
                  Total Put Volume:      %,d
                  Avg Put/Call Ratio:    %s
                  High Volume Days:      %d (%.1f%%)""".formatted(
                profiles.size(), totalEquityVol, totalCallVol, totalPutVol,
                avgPCR, highVolDays,
                profiles.isEmpty() ? 0.0 : (highVolDays * 100.0 / profiles.size()));
    }
}
