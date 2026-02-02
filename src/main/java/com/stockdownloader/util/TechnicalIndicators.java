package com.stockdownloader.util;

import com.stockdownloader.model.PriceData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive technical indicator calculations for stock analysis.
 * Provides Bollinger Bands, Stochastic Oscillator, ATR, OBV, ADX, Parabolic SAR,
 * Williams %R, CCI, VWAP, Fibonacci Retracement, ROC, MFI, and Ichimoku Cloud.
 *
 * All methods operate on List&lt;PriceData&gt; with an endIndex parameter to allow
 * incremental calculation during backtesting.
 */
public final class TechnicalIndicators {

    private static final int SCALE = 10;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    private TechnicalIndicators() {}

    // =========================================================================
    // BOLLINGER BANDS
    // =========================================================================

    public record BollingerBands(BigDecimal upper, BigDecimal middle, BigDecimal lower, BigDecimal width) {}

    /**
     * Calculate Bollinger Bands: Middle = SMA(period), Upper/Lower = Middle +/- numStdDev * StdDev.
     */
    public static BollingerBands bollingerBands(List<PriceData> data, int endIndex, int period, double numStdDev) {
        if (endIndex < period - 1) return new BollingerBands(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        BigDecimal sma = MovingAverageCalculator.sma(data, endIndex, period);
        BigDecimal stdDev = standardDeviation(data, endIndex, period);
        BigDecimal deviation = stdDev.multiply(BigDecimal.valueOf(numStdDev));

        BigDecimal upper = sma.add(deviation);
        BigDecimal lower = sma.subtract(deviation);
        BigDecimal width = upper.subtract(lower);

        return new BollingerBands(upper, sma, lower, width);
    }

    public static BollingerBands bollingerBands(List<PriceData> data, int endIndex) {
        return bollingerBands(data, endIndex, 20, 2.0);
    }

    /**
     * Calculate the Bollinger Band %B: (Price - Lower) / (Upper - Lower).
     * Values > 1 indicate above upper band, < 0 indicate below lower band.
     */
    public static BigDecimal bollingerPercentB(List<PriceData> data, int endIndex, int period) {
        BollingerBands bb = bollingerBands(data, endIndex, period, 2.0);
        BigDecimal range = bb.upper.subtract(bb.lower);
        if (range.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return data.get(endIndex).close().subtract(bb.lower)
                .divide(range, SCALE, RoundingMode.HALF_UP);
    }

    // =========================================================================
    // STOCHASTIC OSCILLATOR
    // =========================================================================

    public record Stochastic(BigDecimal percentK, BigDecimal percentD) {}

    /**
     * Calculate Stochastic Oscillator.
     * %K = (Close - Lowest Low) / (Highest High - Lowest Low) * 100
     * %D = SMA(%K, smoothPeriod) -- approximated by single-bar %K and a 3-period average
     */
    public static Stochastic stochastic(List<PriceData> data, int endIndex, int kPeriod, int dPeriod) {
        if (endIndex < kPeriod - 1) return new Stochastic(BigDecimal.ZERO, BigDecimal.ZERO);

        BigDecimal percentK = calculatePercentK(data, endIndex, kPeriod);

        // Calculate %D as SMA of recent %K values
        BigDecimal sumK = BigDecimal.ZERO;
        int count = 0;
        for (int i = Math.max(kPeriod - 1, endIndex - dPeriod + 1); i <= endIndex; i++) {
            sumK = sumK.add(calculatePercentK(data, i, kPeriod));
            count++;
        }
        BigDecimal percentD = count > 0
                ? sumK.divide(BigDecimal.valueOf(count), SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new Stochastic(percentK, percentD);
    }

    public static Stochastic stochastic(List<PriceData> data, int endIndex) {
        return stochastic(data, endIndex, 14, 3);
    }

    private static BigDecimal calculatePercentK(List<PriceData> data, int endIndex, int period) {
        BigDecimal highestHigh = BigDecimal.ZERO;
        BigDecimal lowestLow = BigDecimal.valueOf(Double.MAX_VALUE);

        for (int i = endIndex - period + 1; i <= endIndex; i++) {
            BigDecimal high = data.get(i).high();
            BigDecimal low = data.get(i).low();
            if (high.compareTo(highestHigh) > 0) highestHigh = high;
            if (low.compareTo(lowestLow) < 0) lowestLow = low;
        }

        BigDecimal range = highestHigh.subtract(lowestLow);
        if (range.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        return data.get(endIndex).close().subtract(lowestLow)
                .divide(range, SCALE, RoundingMode.HALF_UP)
                .multiply(HUNDRED);
    }

    // =========================================================================
    // AVERAGE TRUE RANGE (ATR)
    // =========================================================================

    /**
     * Calculate Average True Range.
     * TR = max(High - Low, |High - PrevClose|, |Low - PrevClose|)
     * ATR = Wilder smooth of TR over period.
     */
    public static BigDecimal atr(List<PriceData> data, int endIndex, int period) {
        if (endIndex < period) return BigDecimal.ZERO;

        // Calculate initial ATR as simple average of first 'period' TRs
        BigDecimal sumTR = BigDecimal.ZERO;
        for (int i = endIndex - period + 1; i <= endIndex && i > 0; i++) {
            sumTR = sumTR.add(trueRange(data, i));
        }
        // Use Wilder smoothing if we have enough data
        int startIndex = Math.max(1, endIndex - period * 2);
        BigDecimal atr = BigDecimal.ZERO;
        int count = 0;
        for (int i = startIndex; i < startIndex + period && i <= endIndex; i++) {
            atr = atr.add(trueRange(data, i));
            count++;
        }
        if (count == 0) return BigDecimal.ZERO;
        atr = atr.divide(BigDecimal.valueOf(count), SCALE, RoundingMode.HALF_UP);

        BigDecimal multiplier = BigDecimal.ONE.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);
        BigDecimal oneMinusMult = BigDecimal.ONE.subtract(multiplier);

        for (int i = startIndex + count; i <= endIndex; i++) {
            atr = trueRange(data, i).multiply(multiplier)
                    .add(atr.multiply(oneMinusMult))
                    .setScale(SCALE, RoundingMode.HALF_UP);
        }

        return atr;
    }

    public static BigDecimal atr(List<PriceData> data, int endIndex) {
        return atr(data, endIndex, 14);
    }

    public static BigDecimal trueRange(List<PriceData> data, int index) {
        if (index <= 0) return data.get(index).high().subtract(data.get(index).low());

        BigDecimal high = data.get(index).high();
        BigDecimal low = data.get(index).low();
        BigDecimal prevClose = data.get(index - 1).close();

        BigDecimal tr1 = high.subtract(low);
        BigDecimal tr2 = high.subtract(prevClose).abs();
        BigDecimal tr3 = low.subtract(prevClose).abs();

        return tr1.max(tr2).max(tr3);
    }

    // =========================================================================
    // ON-BALANCE VOLUME (OBV)
    // =========================================================================

    /**
     * Calculate On-Balance Volume.
     * If close > prevClose: OBV += volume
     * If close < prevClose: OBV -= volume
     * If equal: OBV unchanged
     */
    public static BigDecimal obv(List<PriceData> data, int endIndex) {
        BigDecimal obv = BigDecimal.ZERO;
        for (int i = 1; i <= endIndex; i++) {
            int cmp = data.get(i).close().compareTo(data.get(i - 1).close());
            if (cmp > 0) {
                obv = obv.add(BigDecimal.valueOf(data.get(i).volume()));
            } else if (cmp < 0) {
                obv = obv.subtract(BigDecimal.valueOf(data.get(i).volume()));
            }
        }
        return obv;
    }

    /**
     * Check if OBV is rising (OBV now > OBV lookback bars ago).
     */
    public static boolean isOBVRising(List<PriceData> data, int endIndex, int lookback) {
        if (endIndex < lookback) return false;
        BigDecimal current = obv(data, endIndex);
        BigDecimal previous = obv(data, endIndex - lookback);
        return current.compareTo(previous) > 0;
    }

    // =========================================================================
    // AVERAGE DIRECTIONAL INDEX (ADX)
    // =========================================================================

    public record ADXResult(BigDecimal adx, BigDecimal plusDI, BigDecimal minusDI) {}

    /**
     * Calculate ADX (Average Directional Index) with +DI and -DI.
     */
    public static ADXResult adx(List<PriceData> data, int endIndex, int period) {
        if (endIndex < period * 2) return new ADXResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        int startIdx = Math.max(1, endIndex - period * 3);

        // Calculate smoothed +DM, -DM, and TR
        BigDecimal smoothPlusDM = BigDecimal.ZERO;
        BigDecimal smoothMinusDM = BigDecimal.ZERO;
        BigDecimal smoothTR = BigDecimal.ZERO;

        // Seed with sum of first 'period' values
        int seedEnd = Math.min(startIdx + period, endIndex + 1);
        for (int i = startIdx; i < seedEnd && i > 0; i++) {
            BigDecimal high = data.get(i).high();
            BigDecimal low = data.get(i).low();
            BigDecimal prevHigh = data.get(i - 1).high();
            BigDecimal prevLow = data.get(i - 1).low();

            BigDecimal plusDM = high.subtract(prevHigh);
            BigDecimal minusDM = prevLow.subtract(low);

            if (plusDM.compareTo(BigDecimal.ZERO) > 0 && plusDM.compareTo(minusDM) > 0) {
                smoothPlusDM = smoothPlusDM.add(plusDM);
            }
            if (minusDM.compareTo(BigDecimal.ZERO) > 0 && minusDM.compareTo(plusDM) > 0) {
                smoothMinusDM = smoothMinusDM.add(minusDM);
            }
            smoothTR = smoothTR.add(trueRange(data, i));
        }

        // Wilder smoothing for remaining bars
        BigDecimal periodBD = BigDecimal.valueOf(period);
        List<BigDecimal> dxValues = new ArrayList<>();

        for (int i = seedEnd; i <= endIndex && i > 0; i++) {
            BigDecimal high = data.get(i).high();
            BigDecimal low = data.get(i).low();
            BigDecimal prevHigh = data.get(i - 1).high();
            BigDecimal prevLow = data.get(i - 1).low();

            BigDecimal plusDM = high.subtract(prevHigh);
            BigDecimal minusDM = prevLow.subtract(low);

            BigDecimal curPlusDM = BigDecimal.ZERO;
            BigDecimal curMinusDM = BigDecimal.ZERO;

            if (plusDM.compareTo(BigDecimal.ZERO) > 0 && plusDM.compareTo(minusDM) > 0) {
                curPlusDM = plusDM;
            }
            if (minusDM.compareTo(BigDecimal.ZERO) > 0 && minusDM.compareTo(plusDM) > 0) {
                curMinusDM = minusDM;
            }

            smoothPlusDM = smoothPlusDM.subtract(smoothPlusDM.divide(periodBD, SCALE, RoundingMode.HALF_UP))
                    .add(curPlusDM);
            smoothMinusDM = smoothMinusDM.subtract(smoothMinusDM.divide(periodBD, SCALE, RoundingMode.HALF_UP))
                    .add(curMinusDM);
            smoothTR = smoothTR.subtract(smoothTR.divide(periodBD, SCALE, RoundingMode.HALF_UP))
                    .add(trueRange(data, i));

            if (smoothTR.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal pDI = smoothPlusDM.divide(smoothTR, SCALE, RoundingMode.HALF_UP).multiply(HUNDRED);
                BigDecimal mDI = smoothMinusDM.divide(smoothTR, SCALE, RoundingMode.HALF_UP).multiply(HUNDRED);
                BigDecimal diSum = pDI.add(mDI);
                if (diSum.compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal dx = pDI.subtract(mDI).abs()
                            .divide(diSum, SCALE, RoundingMode.HALF_UP)
                            .multiply(HUNDRED);
                    dxValues.add(dx);
                }
            }
        }

        // ADX = smoothed average of DX values
        BigDecimal adxValue = BigDecimal.ZERO;
        if (!dxValues.isEmpty()) {
            int adxPeriod = Math.min(period, dxValues.size());
            BigDecimal sum = BigDecimal.ZERO;
            for (int i = dxValues.size() - adxPeriod; i < dxValues.size(); i++) {
                sum = sum.add(dxValues.get(i));
            }
            adxValue = sum.divide(BigDecimal.valueOf(adxPeriod), SCALE, RoundingMode.HALF_UP);
        }

        // Calculate current +DI and -DI
        BigDecimal plusDI = BigDecimal.ZERO;
        BigDecimal minusDI = BigDecimal.ZERO;
        if (smoothTR.compareTo(BigDecimal.ZERO) != 0) {
            plusDI = smoothPlusDM.divide(smoothTR, SCALE, RoundingMode.HALF_UP).multiply(HUNDRED);
            minusDI = smoothMinusDM.divide(smoothTR, SCALE, RoundingMode.HALF_UP).multiply(HUNDRED);
        }

        return new ADXResult(adxValue, plusDI, minusDI);
    }

    public static ADXResult adx(List<PriceData> data, int endIndex) {
        return adx(data, endIndex, 14);
    }

    // =========================================================================
    // PARABOLIC SAR
    // =========================================================================

    /**
     * Calculate Parabolic SAR at the given index.
     * Uses the standard Wilder method: AF starts at 0.02, increments by 0.02 per new EP, max 0.20.
     */
    public static BigDecimal parabolicSAR(List<PriceData> data, int endIndex) {
        if (endIndex < 2) return data.get(0).low();

        double af = 0.02;
        double maxAF = 0.20;
        double afStep = 0.02;

        boolean isUpTrend = data.get(1).close().compareTo(data.get(0).close()) > 0;
        double sar = isUpTrend ? data.get(0).low().doubleValue() : data.get(0).high().doubleValue();
        double ep = isUpTrend ? data.get(1).high().doubleValue() : data.get(1).low().doubleValue();

        for (int i = 2; i <= endIndex; i++) {
            double high = data.get(i).high().doubleValue();
            double low = data.get(i).low().doubleValue();

            sar = sar + af * (ep - sar);

            if (isUpTrend) {
                sar = Math.min(sar, Math.min(data.get(i - 1).low().doubleValue(),
                        data.get(i - 2).low().doubleValue()));
                if (low < sar) {
                    // Flip to downtrend
                    isUpTrend = false;
                    sar = ep;
                    ep = low;
                    af = afStep;
                } else {
                    if (high > ep) {
                        ep = high;
                        af = Math.min(af + afStep, maxAF);
                    }
                }
            } else {
                sar = Math.max(sar, Math.max(data.get(i - 1).high().doubleValue(),
                        data.get(i - 2).high().doubleValue()));
                if (high > sar) {
                    // Flip to uptrend
                    isUpTrend = true;
                    sar = ep;
                    ep = high;
                    af = afStep;
                } else {
                    if (low < ep) {
                        ep = low;
                        af = Math.min(af + afStep, maxAF);
                    }
                }
            }
        }

        return BigDecimal.valueOf(sar).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Returns true if SAR indicates uptrend (SAR below price).
     */
    public static boolean isSARBullish(List<PriceData> data, int endIndex) {
        BigDecimal sar = parabolicSAR(data, endIndex);
        return data.get(endIndex).close().compareTo(sar) > 0;
    }

    // =========================================================================
    // WILLIAMS %R
    // =========================================================================

    /**
     * Calculate Williams %R.
     * %R = (Highest High - Close) / (Highest High - Lowest Low) * -100
     */
    public static BigDecimal williamsR(List<PriceData> data, int endIndex, int period) {
        if (endIndex < period - 1) return BigDecimal.ZERO;

        BigDecimal highestHigh = BigDecimal.ZERO;
        BigDecimal lowestLow = BigDecimal.valueOf(Double.MAX_VALUE);

        for (int i = endIndex - period + 1; i <= endIndex; i++) {
            if (data.get(i).high().compareTo(highestHigh) > 0) highestHigh = data.get(i).high();
            if (data.get(i).low().compareTo(lowestLow) < 0) lowestLow = data.get(i).low();
        }

        BigDecimal range = highestHigh.subtract(lowestLow);
        if (range.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        return highestHigh.subtract(data.get(endIndex).close())
                .divide(range, SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(-100));
    }

    public static BigDecimal williamsR(List<PriceData> data, int endIndex) {
        return williamsR(data, endIndex, 14);
    }

    // =========================================================================
    // COMMODITY CHANNEL INDEX (CCI)
    // =========================================================================

    /**
     * Calculate CCI.
     * CCI = (TP - SMA(TP)) / (0.015 * Mean Deviation)
     * where TP = (High + Low + Close) / 3
     */
    public static BigDecimal cci(List<PriceData> data, int endIndex, int period) {
        if (endIndex < period - 1) return BigDecimal.ZERO;

        // Calculate typical prices
        BigDecimal[] tp = new BigDecimal[period];
        BigDecimal sumTP = BigDecimal.ZERO;

        for (int i = 0; i < period; i++) {
            int idx = endIndex - period + 1 + i;
            tp[i] = data.get(idx).high().add(data.get(idx).low()).add(data.get(idx).close())
                    .divide(BigDecimal.valueOf(3), SCALE, RoundingMode.HALF_UP);
            sumTP = sumTP.add(tp[i]);
        }

        BigDecimal smaTP = sumTP.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);

        // Mean deviation
        BigDecimal sumDev = BigDecimal.ZERO;
        for (BigDecimal t : tp) {
            sumDev = sumDev.add(t.subtract(smaTP).abs());
        }
        BigDecimal meanDev = sumDev.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);

        BigDecimal constant = new BigDecimal("0.015");
        BigDecimal divisor = constant.multiply(meanDev);

        if (divisor.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        BigDecimal currentTP = tp[period - 1];
        return currentTP.subtract(smaTP).divide(divisor, SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal cci(List<PriceData> data, int endIndex) {
        return cci(data, endIndex, 20);
    }

    // =========================================================================
    // VWAP (Volume-Weighted Average Price)
    // =========================================================================

    /**
     * Calculate VWAP over a lookback period.
     * VWAP = Sum(TP * Volume) / Sum(Volume)
     * where TP = (High + Low + Close) / 3
     */
    public static BigDecimal vwap(List<PriceData> data, int endIndex, int lookback) {
        int startIdx = Math.max(0, endIndex - lookback + 1);

        BigDecimal sumTPV = BigDecimal.ZERO;
        BigDecimal sumVol = BigDecimal.ZERO;

        for (int i = startIdx; i <= endIndex; i++) {
            BigDecimal tp = data.get(i).high().add(data.get(i).low()).add(data.get(i).close())
                    .divide(BigDecimal.valueOf(3), SCALE, RoundingMode.HALF_UP);
            BigDecimal vol = BigDecimal.valueOf(data.get(i).volume());
            sumTPV = sumTPV.add(tp.multiply(vol));
            sumVol = sumVol.add(vol);
        }

        if (sumVol.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return sumTPV.divide(sumVol, SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal vwap(List<PriceData> data, int endIndex) {
        return vwap(data, endIndex, 20);
    }

    // =========================================================================
    // FIBONACCI RETRACEMENT
    // =========================================================================

    public record FibonacciLevels(
            BigDecimal high, BigDecimal low,
            BigDecimal level236, BigDecimal level382,
            BigDecimal level500, BigDecimal level618,
            BigDecimal level786) {}

    /**
     * Calculate Fibonacci retracement levels based on the high and low within a lookback period.
     * Levels are calculated from the swing high to swing low.
     */
    public static FibonacciLevels fibonacciRetracement(List<PriceData> data, int endIndex, int lookback) {
        int startIdx = Math.max(0, endIndex - lookback + 1);

        BigDecimal highest = BigDecimal.ZERO;
        BigDecimal lowest = BigDecimal.valueOf(Double.MAX_VALUE);

        for (int i = startIdx; i <= endIndex; i++) {
            if (data.get(i).high().compareTo(highest) > 0) highest = data.get(i).high();
            if (data.get(i).low().compareTo(lowest) < 0) lowest = data.get(i).low();
        }

        BigDecimal range = highest.subtract(lowest);

        return new FibonacciLevels(
                highest, lowest,
                highest.subtract(range.multiply(new BigDecimal("0.236"))).setScale(4, RoundingMode.HALF_UP),
                highest.subtract(range.multiply(new BigDecimal("0.382"))).setScale(4, RoundingMode.HALF_UP),
                highest.subtract(range.multiply(new BigDecimal("0.500"))).setScale(4, RoundingMode.HALF_UP),
                highest.subtract(range.multiply(new BigDecimal("0.618"))).setScale(4, RoundingMode.HALF_UP),
                highest.subtract(range.multiply(new BigDecimal("0.786"))).setScale(4, RoundingMode.HALF_UP)
        );
    }

    public static FibonacciLevels fibonacciRetracement(List<PriceData> data, int endIndex) {
        return fibonacciRetracement(data, endIndex, 50);
    }

    // =========================================================================
    // RATE OF CHANGE (ROC)
    // =========================================================================

    /**
     * Calculate Rate of Change.
     * ROC = ((Close - Close_n) / Close_n) * 100
     */
    public static BigDecimal roc(List<PriceData> data, int endIndex, int period) {
        if (endIndex < period) return BigDecimal.ZERO;

        BigDecimal currentClose = data.get(endIndex).close();
        BigDecimal pastClose = data.get(endIndex - period).close();

        if (pastClose.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        return currentClose.subtract(pastClose)
                .divide(pastClose, SCALE, RoundingMode.HALF_UP)
                .multiply(HUNDRED);
    }

    public static BigDecimal roc(List<PriceData> data, int endIndex) {
        return roc(data, endIndex, 12);
    }

    // =========================================================================
    // MONEY FLOW INDEX (MFI)
    // =========================================================================

    /**
     * Calculate Money Flow Index (volume-weighted RSI).
     * MFI = 100 - (100 / (1 + Money Ratio))
     * Money Ratio = Positive Money Flow / Negative Money Flow
     */
    public static BigDecimal mfi(List<PriceData> data, int endIndex, int period) {
        if (endIndex < period) return BigDecimal.ZERO;

        BigDecimal positiveFlow = BigDecimal.ZERO;
        BigDecimal negativeFlow = BigDecimal.ZERO;

        for (int i = endIndex - period + 1; i <= endIndex; i++) {
            BigDecimal tp = data.get(i).high().add(data.get(i).low()).add(data.get(i).close())
                    .divide(BigDecimal.valueOf(3), SCALE, RoundingMode.HALF_UP);
            BigDecimal prevTP = data.get(i - 1).high().add(data.get(i - 1).low()).add(data.get(i - 1).close())
                    .divide(BigDecimal.valueOf(3), SCALE, RoundingMode.HALF_UP);

            BigDecimal moneyFlow = tp.multiply(BigDecimal.valueOf(data.get(i).volume()));

            if (tp.compareTo(prevTP) > 0) {
                positiveFlow = positiveFlow.add(moneyFlow);
            } else if (tp.compareTo(prevTP) < 0) {
                negativeFlow = negativeFlow.add(moneyFlow);
            }
        }

        if (negativeFlow.compareTo(BigDecimal.ZERO) == 0) {
            return positiveFlow.compareTo(BigDecimal.ZERO) > 0 ? HUNDRED : BigDecimal.ZERO;
        }

        BigDecimal moneyRatio = positiveFlow.divide(negativeFlow, SCALE, RoundingMode.HALF_UP);
        return HUNDRED.subtract(
                HUNDRED.divide(BigDecimal.ONE.add(moneyRatio), SCALE, RoundingMode.HALF_UP));
    }

    public static BigDecimal mfi(List<PriceData> data, int endIndex) {
        return mfi(data, endIndex, 14);
    }

    // =========================================================================
    // ICHIMOKU CLOUD
    // =========================================================================

    public record IchimokuCloud(
            BigDecimal tenkanSen,     // Conversion Line (9-period)
            BigDecimal kijunSen,      // Base Line (26-period)
            BigDecimal senkouSpanA,   // Leading Span A
            BigDecimal senkouSpanB,   // Leading Span B (52-period)
            BigDecimal chikouSpan,    // Lagging Span
            boolean priceAboveCloud) {}

    /**
     * Calculate Ichimoku Cloud components.
     */
    public static IchimokuCloud ichimoku(List<PriceData> data, int endIndex) {
        if (endIndex < 52) {
            return new IchimokuCloud(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, false);
        }

        BigDecimal tenkan = periodMidpoint(data, endIndex, 9);
        BigDecimal kijun = periodMidpoint(data, endIndex, 26);
        BigDecimal senkouA = tenkan.add(kijun).divide(TWO, SCALE, RoundingMode.HALF_UP);

        // Senkou Span B uses 52-period midpoint
        BigDecimal senkouB = periodMidpoint(data, endIndex, 52);

        // Chikou Span = current close (plotted 26 periods back)
        BigDecimal chikou = data.get(endIndex).close();

        BigDecimal price = data.get(endIndex).close();
        boolean aboveCloud = price.compareTo(senkouA.max(senkouB)) > 0;

        return new IchimokuCloud(tenkan, kijun, senkouA, senkouB, chikou, aboveCloud);
    }

    private static BigDecimal periodMidpoint(List<PriceData> data, int endIndex, int period) {
        BigDecimal highest = BigDecimal.ZERO;
        BigDecimal lowest = BigDecimal.valueOf(Double.MAX_VALUE);

        for (int i = endIndex - period + 1; i <= endIndex; i++) {
            if (data.get(i).high().compareTo(highest) > 0) highest = data.get(i).high();
            if (data.get(i).low().compareTo(lowest) < 0) lowest = data.get(i).low();
        }

        return highest.add(lowest).divide(TWO, SCALE, RoundingMode.HALF_UP);
    }

    // =========================================================================
    // RSI (standalone utility method for use in strategies)
    // =========================================================================

    /**
     * Calculate RSI at the given index.
     */
    public static BigDecimal rsi(List<PriceData> data, int endIndex, int period) {
        if (endIndex < period + 1) return BigDecimal.valueOf(50);

        BigDecimal avgGain = BigDecimal.ZERO;
        BigDecimal avgLoss = BigDecimal.ZERO;

        for (int i = endIndex - period + 1; i <= endIndex; i++) {
            BigDecimal change = data.get(i).close().subtract(data.get(i - 1).close());
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                avgGain = avgGain.add(change);
            } else {
                avgLoss = avgLoss.add(change.abs());
            }
        }

        BigDecimal periodBD = BigDecimal.valueOf(period);
        avgGain = avgGain.divide(periodBD, SCALE, RoundingMode.HALF_UP);
        avgLoss = avgLoss.divide(periodBD, SCALE, RoundingMode.HALF_UP);

        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) return HUNDRED;

        BigDecimal rs = avgGain.divide(avgLoss, SCALE, RoundingMode.HALF_UP);
        return HUNDRED.subtract(HUNDRED.divide(BigDecimal.ONE.add(rs), 6, RoundingMode.HALF_UP));
    }

    /**
     * Calculate MACD line value.
     */
    public static BigDecimal macdLine(List<PriceData> data, int endIndex, int fast, int slow) {
        if (endIndex < slow) return BigDecimal.ZERO;
        return MovingAverageCalculator.ema(data, endIndex, fast)
                .subtract(MovingAverageCalculator.ema(data, endIndex, slow));
    }

    /**
     * Calculate MACD signal line.
     */
    public static BigDecimal macdSignal(List<PriceData> data, int endIndex, int fast, int slow, int signal) {
        if (endIndex < slow + signal) return BigDecimal.ZERO;

        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (signal + 1));
        BigDecimal oneMinusMult = BigDecimal.ONE.subtract(multiplier);

        int startIdx = Math.max(slow, endIndex - signal + 1);

        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (int i = startIdx; i < startIdx + signal && i <= endIndex; i++) {
            sum = sum.add(macdLine(data, i, fast, slow));
            count++;
        }
        if (count == 0) return BigDecimal.ZERO;

        BigDecimal signalEMA = sum.divide(BigDecimal.valueOf(count), SCALE, RoundingMode.HALF_UP);

        for (int i = startIdx + count; i <= endIndex; i++) {
            BigDecimal macdVal = macdLine(data, i, fast, slow);
            signalEMA = macdVal.multiply(multiplier)
                    .add(signalEMA.multiply(oneMinusMult))
                    .setScale(SCALE, RoundingMode.HALF_UP);
        }

        return signalEMA;
    }

    /**
     * Calculate MACD histogram.
     */
    public static BigDecimal macdHistogram(List<PriceData> data, int endIndex, int fast, int slow, int signal) {
        return macdLine(data, endIndex, fast, slow).subtract(macdSignal(data, endIndex, fast, slow, signal));
    }

    // =========================================================================
    // SUPPORT & RESISTANCE
    // =========================================================================

    public record SupportResistance(List<BigDecimal> supportLevels, List<BigDecimal> resistanceLevels) {}

    /**
     * Detect support and resistance levels using swing points.
     * A swing high requires 'window' bars on each side to be lower.
     * A swing low requires 'window' bars on each side to be higher.
     */
    public static SupportResistance supportResistance(List<PriceData> data, int endIndex, int lookback, int window) {
        int startIdx = Math.max(window, endIndex - lookback);
        int endLimit = Math.min(endIndex - window, endIndex);

        List<BigDecimal> supports = new ArrayList<>();
        List<BigDecimal> resistances = new ArrayList<>();

        BigDecimal currentPrice = data.get(endIndex).close();

        for (int i = startIdx; i <= endLimit; i++) {
            boolean isSwingHigh = true;
            boolean isSwingLow = true;

            for (int j = 1; j <= window; j++) {
                if (i - j < 0 || i + j > endIndex) {
                    isSwingHigh = false;
                    isSwingLow = false;
                    break;
                }
                if (data.get(i).high().compareTo(data.get(i - j).high()) <= 0 ||
                        data.get(i).high().compareTo(data.get(i + j).high()) <= 0) {
                    isSwingHigh = false;
                }
                if (data.get(i).low().compareTo(data.get(i - j).low()) >= 0 ||
                        data.get(i).low().compareTo(data.get(i + j).low()) >= 0) {
                    isSwingLow = false;
                }
            }

            if (isSwingHigh) {
                BigDecimal level = data.get(i).high();
                if (level.compareTo(currentPrice) > 0) {
                    resistances.add(level);
                }
            }
            if (isSwingLow) {
                BigDecimal level = data.get(i).low();
                if (level.compareTo(currentPrice) < 0) {
                    supports.add(level);
                }
            }
        }

        // Sort: supports descending (nearest first), resistances ascending (nearest first)
        supports.sort((a, b) -> b.compareTo(a));
        resistances.sort(BigDecimal::compareTo);

        // Deduplicate levels that are within 1% of each other
        supports = deduplicateLevels(supports, currentPrice);
        resistances = deduplicateLevels(resistances, currentPrice);

        return new SupportResistance(supports, resistances);
    }

    private static List<BigDecimal> deduplicateLevels(List<BigDecimal> levels, BigDecimal reference) {
        if (levels.isEmpty()) return levels;
        BigDecimal tolerance = reference.multiply(new BigDecimal("0.01"));
        List<BigDecimal> deduped = new ArrayList<>();
        deduped.add(levels.getFirst());

        for (int i = 1; i < levels.size(); i++) {
            boolean tooClose = false;
            for (BigDecimal existing : deduped) {
                if (levels.get(i).subtract(existing).abs().compareTo(tolerance) < 0) {
                    tooClose = true;
                    break;
                }
            }
            if (!tooClose) {
                deduped.add(levels.get(i));
            }
        }
        return deduped;
    }

    // =========================================================================
    // AVERAGE VOLUME
    // =========================================================================

    /**
     * Calculate average volume over a period.
     */
    public static BigDecimal averageVolume(List<PriceData> data, int endIndex, int period) {
        if (endIndex < period - 1) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = endIndex - period + 1; i <= endIndex; i++) {
            sum = sum.add(BigDecimal.valueOf(data.get(i).volume()));
        }
        return sum.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    /**
     * Calculate standard deviation of close prices over a period.
     */
    public static BigDecimal standardDeviation(List<PriceData> data, int endIndex, int period) {
        if (endIndex < period - 1) return BigDecimal.ZERO;

        BigDecimal sum = BigDecimal.ZERO;
        for (int i = endIndex - period + 1; i <= endIndex; i++) {
            sum = sum.add(data.get(i).close());
        }
        BigDecimal mean = sum.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);

        BigDecimal sumSqDiff = BigDecimal.ZERO;
        for (int i = endIndex - period + 1; i <= endIndex; i++) {
            BigDecimal diff = data.get(i).close().subtract(mean);
            sumSqDiff = sumSqDiff.add(diff.multiply(diff));
        }

        double variance = sumSqDiff.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP).doubleValue();
        return BigDecimal.valueOf(Math.sqrt(variance)).setScale(SCALE, RoundingMode.HALF_UP);
    }
}
