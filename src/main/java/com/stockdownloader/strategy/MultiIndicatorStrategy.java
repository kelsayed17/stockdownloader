package com.stockdownloader.strategy;

import com.stockdownloader.model.IndicatorValues;
import com.stockdownloader.model.PriceData;

import java.math.BigDecimal;
import java.util.List;

/**
 * Multi-indicator confluence strategy that scores buy/sell signals across
 * trend, momentum, volatility, and volume indicator categories.
 *
 * Each indicator that agrees with a direction adds to the confluence score.
 * A trade is only taken when the score meets or exceeds the threshold.
 *
 * Scored indicators (8 total):
 *   Trend:      EMA(20) > EMA(50), Price > SMA(200), Ichimoku above cloud
 *   Momentum:   RSI recovery from oversold, MACD bullish crossover, Stochastic oversold
 *   Volume:     OBV rising, MFI oversold
 *
 * Signal threshold: configurable (default = 4 out of 8 indicators must agree)
 */
public final class MultiIndicatorStrategy implements TradingStrategy {

    private final int buyThreshold;
    private final int sellThreshold;

    public MultiIndicatorStrategy(int buyThreshold, int sellThreshold) {
        if (buyThreshold < 1 || sellThreshold < 1) {
            throw new IllegalArgumentException("Thresholds must be >= 1");
        }
        this.buyThreshold = buyThreshold;
        this.sellThreshold = sellThreshold;
    }

    public MultiIndicatorStrategy() {
        this(4, 4);
    }

    @Override
    public String getName() {
        return "Multi-Indicator Confluence (Buy>=%d, Sell>=%d)".formatted(buyThreshold, sellThreshold);
    }

    @Override
    public Signal evaluate(List<PriceData> data, int currentIndex) {
        if (currentIndex < getWarmupPeriod()) return Signal.HOLD;

        IndicatorValues current = IndicatorValues.compute(data, currentIndex);
        IndicatorValues previous = IndicatorValues.compute(data, currentIndex - 1);

        int buyScore = computeBuyScore(current, previous);
        int sellScore = computeSellScore(current, previous);

        if (buyScore >= buyThreshold && buyScore > sellScore) {
            return Signal.BUY;
        }
        if (sellScore >= sellThreshold && sellScore > buyScore) {
            return Signal.SELL;
        }

        return Signal.HOLD;
    }

    @Override
    public int getWarmupPeriod() {
        return 201; // Need 200 bars for SMA(200) + 1 for crossover
    }

    private int computeBuyScore(IndicatorValues current, IndicatorValues previous) {
        int score = 0;

        // Trend: EMA(12) > EMA(26) -- short-term trend bullish
        if (current.ema12().compareTo(current.ema26()) > 0) score++;

        // Trend: Price > SMA(200) -- long-term uptrend
        if (current.sma200().compareTo(BigDecimal.ZERO) > 0
                && current.close().compareTo(current.sma200()) > 0) score++;

        // Trend: Ichimoku -- price above cloud
        if (current.priceAboveCloud()) score++;

        // Momentum: RSI recovers from oversold (crosses above 30)
        if (current.rsi14().doubleValue() > 30 && previous.rsi14().doubleValue() <= 30) score++;

        // Momentum: MACD bullish crossover
        if (current.macdLine().compareTo(current.macdSignal()) > 0
                && previous.macdLine().compareTo(previous.macdSignal()) <= 0) score++;

        // Momentum: Stochastic %K crossing above %D from oversold zone
        if (current.stochK().doubleValue() < 30
                && current.stochK().compareTo(current.stochD()) > 0
                && previous.stochK().compareTo(previous.stochD()) <= 0) score++;

        // Volume: OBV rising (accumulation)
        if (current.obvRising()) score++;

        // Volume: MFI oversold recovery (< 20 or recovering from < 20)
        if (current.mfi14().doubleValue() < 30 && current.mfi14().doubleValue() > previous.mfi14().doubleValue()) score++;

        return score;
    }

    private int computeSellScore(IndicatorValues current, IndicatorValues previous) {
        int score = 0;

        // Trend: EMA(12) < EMA(26) -- short-term trend bearish
        if (current.ema12().compareTo(current.ema26()) < 0) score++;

        // Trend: Price < SMA(200) -- long-term downtrend
        if (current.sma200().compareTo(BigDecimal.ZERO) > 0
                && current.close().compareTo(current.sma200()) < 0) score++;

        // Trend: Ichimoku -- price below cloud
        if (!current.priceAboveCloud()
                && current.ichimokuSpanA().compareTo(BigDecimal.ZERO) > 0) score++;

        // Momentum: RSI falls from overbought (crosses below 70)
        if (current.rsi14().doubleValue() < 70 && previous.rsi14().doubleValue() >= 70) score++;

        // Momentum: MACD bearish crossover
        if (current.macdLine().compareTo(current.macdSignal()) < 0
                && previous.macdLine().compareTo(previous.macdSignal()) >= 0) score++;

        // Momentum: Stochastic %K crossing below %D from overbought zone
        if (current.stochK().doubleValue() > 70
                && current.stochK().compareTo(current.stochD()) < 0
                && previous.stochK().compareTo(previous.stochD()) >= 0) score++;

        // Volume: OBV falling (distribution)
        if (!current.obvRising()) score++;

        // Volume: MFI overbought reversal (> 80 or falling from > 80)
        if (current.mfi14().doubleValue() > 70 && current.mfi14().doubleValue() < previous.mfi14().doubleValue()) score++;

        return score;
    }
}
