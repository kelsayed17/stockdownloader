package com.stockdownloader.model;

import com.stockdownloader.util.MovingAverageCalculator;
import com.stockdownloader.util.TechnicalIndicators;
import com.stockdownloader.util.TechnicalIndicators.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Snapshot of all computed technical indicator values at a given point in time.
 * Used by multi-indicator strategies and the signal generator to make decisions
 * based on the full suite of available indicators rather than just price alone.
 */
public record IndicatorValues(
        // Price data
        String date,
        BigDecimal close,
        BigDecimal high,
        BigDecimal low,
        BigDecimal open,
        long volume,

        // Moving Averages
        BigDecimal sma20,
        BigDecimal sma50,
        BigDecimal sma200,
        BigDecimal ema12,
        BigDecimal ema26,

        // Momentum
        BigDecimal rsi14,
        BigDecimal macdLine,
        BigDecimal macdSignal,
        BigDecimal macdHistogram,
        BigDecimal roc12,
        BigDecimal williamsR,

        // Bollinger Bands
        BigDecimal bbUpper,
        BigDecimal bbMiddle,
        BigDecimal bbLower,
        BigDecimal bbWidth,
        BigDecimal bbPercentB,

        // Stochastic
        BigDecimal stochK,
        BigDecimal stochD,

        // Volatility
        BigDecimal atr14,

        // Volume
        BigDecimal obv,
        boolean obvRising,
        BigDecimal mfi14,
        BigDecimal avgVolume20,

        // Trend
        BigDecimal adx14,
        BigDecimal plusDI,
        BigDecimal minusDI,
        BigDecimal parabolicSAR,
        boolean sarBullish,

        // CCI
        BigDecimal cci20,

        // VWAP
        BigDecimal vwap,

        // Ichimoku
        BigDecimal ichimokuTenkan,
        BigDecimal ichimokuKijun,
        BigDecimal ichimokuSpanA,
        BigDecimal ichimokuSpanB,
        boolean priceAboveCloud,

        // Fibonacci
        BigDecimal fibHigh,
        BigDecimal fibLow,
        BigDecimal fib236,
        BigDecimal fib382,
        BigDecimal fib500,
        BigDecimal fib618,
        BigDecimal fib786
) {

    /**
     * Compute all indicator values at the given index in the price data.
     * Requires at least 200 bars for SMA(200); partial values are returned
     * for indicators that have enough data.
     */
    public static IndicatorValues compute(List<PriceData> data, int index) {
        PriceData bar = data.get(index);

        // Moving averages
        BigDecimal sma20 = index >= 19 ? MovingAverageCalculator.sma(data, index, 20) : BigDecimal.ZERO;
        BigDecimal sma50 = index >= 49 ? MovingAverageCalculator.sma(data, index, 50) : BigDecimal.ZERO;
        BigDecimal sma200 = index >= 199 ? MovingAverageCalculator.sma(data, index, 200) : BigDecimal.ZERO;
        BigDecimal ema12 = index >= 12 ? MovingAverageCalculator.ema(data, index, 12) : BigDecimal.ZERO;
        BigDecimal ema26 = index >= 26 ? MovingAverageCalculator.ema(data, index, 26) : BigDecimal.ZERO;

        // RSI
        BigDecimal rsi14 = TechnicalIndicators.rsi(data, index, 14);

        // MACD
        BigDecimal macdL = TechnicalIndicators.macdLine(data, index, 12, 26);
        BigDecimal macdS = TechnicalIndicators.macdSignal(data, index, 12, 26, 9);
        BigDecimal macdH = TechnicalIndicators.macdHistogram(data, index, 12, 26, 9);

        // ROC
        BigDecimal roc = TechnicalIndicators.roc(data, index, 12);

        // Williams %R
        BigDecimal willR = TechnicalIndicators.williamsR(data, index, 14);

        // Bollinger Bands
        BollingerBands bb = TechnicalIndicators.bollingerBands(data, index);
        BigDecimal bbPctB = index >= 19 ? TechnicalIndicators.bollingerPercentB(data, index, 20) : BigDecimal.ZERO;

        // Stochastic
        Stochastic stoch = TechnicalIndicators.stochastic(data, index);

        // ATR
        BigDecimal atr = TechnicalIndicators.atr(data, index, 14);

        // OBV
        BigDecimal obvVal = TechnicalIndicators.obv(data, index);
        boolean obvRis = TechnicalIndicators.isOBVRising(data, index, 5);

        // MFI
        BigDecimal mfiVal = TechnicalIndicators.mfi(data, index, 14);

        // Average Volume
        BigDecimal avgVol = TechnicalIndicators.averageVolume(data, index, 20);

        // ADX
        ADXResult adxResult = TechnicalIndicators.adx(data, index, 14);

        // Parabolic SAR
        BigDecimal psar = TechnicalIndicators.parabolicSAR(data, index);
        boolean sarBull = TechnicalIndicators.isSARBullish(data, index);

        // CCI
        BigDecimal cciVal = TechnicalIndicators.cci(data, index, 20);

        // VWAP
        BigDecimal vwapVal = TechnicalIndicators.vwap(data, index, 20);

        // Ichimoku
        IchimokuCloud ichimoku = TechnicalIndicators.ichimoku(data, index);

        // Fibonacci
        FibonacciLevels fib = TechnicalIndicators.fibonacciRetracement(data, index, 50);

        return new IndicatorValues(
                bar.date(), bar.close(), bar.high(), bar.low(), bar.open(), bar.volume(),
                sma20, sma50, sma200, ema12, ema26,
                rsi14, macdL, macdS, macdH, roc, willR,
                bb.upper(), bb.middle(), bb.lower(), bb.width(), bbPctB,
                stoch.percentK(), stoch.percentD(),
                atr,
                obvVal, obvRis, mfiVal, avgVol,
                adxResult.adx(), adxResult.plusDI(), adxResult.minusDI(),
                psar, sarBull,
                cciVal, vwapVal,
                ichimoku.tenkanSen(), ichimoku.kijunSen(), ichimoku.senkouSpanA(), ichimoku.senkouSpanB(),
                ichimoku.priceAboveCloud(),
                fib.high(), fib.low(), fib.level236(), fib.level382(),
                fib.level500(), fib.level618(), fib.level786()
        );
    }

    /**
     * Summary string for display in reports.
     */
    public String summary() {
        return """
                %s  Close: $%s  Vol: %,d
                  SMA(20/50/200): %s / %s / %s
                  RSI(14): %s  MACD: %s  ADX: %s
                  BB: [%s - %s - %s]  %%B: %s
                  Stoch: %%K=%s %%D=%s  ATR: %s
                  OBV Rising: %s  MFI: %s  CCI: %s
                  VWAP: %s  SAR: %s (%s)
                  Ichimoku: %s cloud  Williams %%R: %s
                  Fib Levels: 23.6%%=%s  38.2%%=%s  50%%=%s  61.8%%=%s"""
                .formatted(
                        date, close, volume,
                        fmt(sma20), fmt(sma50), fmt(sma200),
                        fmt(rsi14), fmt(macdLine), fmt(adx14),
                        fmt(bbLower), fmt(bbMiddle), fmt(bbUpper), fmt(bbPercentB),
                        fmt(stochK), fmt(stochD), fmt(atr14),
                        obvRising ? "Yes" : "No", fmt(mfi14), fmt(cci20),
                        fmt(vwap), fmt(parabolicSAR), sarBullish ? "Bullish" : "Bearish",
                        priceAboveCloud ? "Above" : "Below", fmt(williamsR),
                        fmt(fib236), fmt(fib382), fmt(fib500), fmt(fib618)
                );
    }

    private static String fmt(BigDecimal v) {
        if (v == null) return "N/A";
        return v.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
