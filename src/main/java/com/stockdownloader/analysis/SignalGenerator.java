package com.stockdownloader.analysis;

import com.stockdownloader.model.*;
import com.stockdownloader.model.AlertResult.Direction;
import com.stockdownloader.model.AlertResult.OptionsRecommendation;
import com.stockdownloader.model.AlertResult.OptionsRecommendation.Action;
import com.stockdownloader.util.BlackScholesCalculator;
import com.stockdownloader.util.TechnicalIndicators;
import com.stockdownloader.util.TechnicalIndicators.SupportResistance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates trading alerts by analyzing all available technical indicators
 * and producing confluence-based buy/sell signals with options recommendations.
 *
 * The signal generator scores indicators across four dimensions:
 * - Trend (EMA crossovers, SMA(200) position, Ichimoku cloud, Parabolic SAR)
 * - Momentum (RSI, MACD, Stochastic, Williams %R, CCI, ROC)
 * - Volume (OBV, MFI, volume vs average)
 * - Volatility (Bollinger Bands, ATR)
 *
 * Options recommendations include:
 * - When to buy calls (bullish signals) or puts (bearish signals)
 * - Suggested strike prices based on Fibonacci levels and delta targets
 * - Suggested expiration (DTE) based on ATR and signal strength
 * - Estimated premium using Black-Scholes
 */
public final class SignalGenerator {

    private static final BigDecimal RISK_FREE_RATE = new BigDecimal("0.05");
    private static final int VOLATILITY_LOOKBACK = 20;

    private SignalGenerator() {}

    /**
     * Generate a full trading alert for the most recent bar in the data.
     */
    public static AlertResult generateAlert(String symbol, List<PriceData> data) {
        return generateAlert(symbol, data, data.size() - 1);
    }

    /**
     * Generate a trading alert at a specific index in the data.
     */
    public static AlertResult generateAlert(String symbol, List<PriceData> data, int index) {
        if (index < 200) {
            // Not enough data for full analysis
            return createNeutralAlert(symbol, data, index);
        }

        IndicatorValues current = IndicatorValues.compute(data, index);
        IndicatorValues previous = IndicatorValues.compute(data, index - 1);

        List<String> bullish = new ArrayList<>();
        List<String> bearish = new ArrayList<>();

        // === TREND INDICATORS ===
        scoreTrendIndicators(current, previous, bullish, bearish);

        // === MOMENTUM INDICATORS ===
        scoreMomentumIndicators(current, previous, bullish, bearish);

        // === VOLUME INDICATORS ===
        scoreVolumeIndicators(current, data, index, bullish, bearish);

        // === VOLATILITY INDICATORS ===
        scoreVolatilityIndicators(current, previous, bullish, bearish);

        // Calculate confluence
        int totalIndicators = bullish.size() + bearish.size();
        if (totalIndicators == 0) totalIndicators = 1;

        double bullishPct = (double) bullish.size() / totalIndicators;
        double bearishPct = (double) bearish.size() / totalIndicators;

        Direction direction;
        double confluenceScore;
        if (bullishPct >= 0.75) {
            direction = Direction.STRONG_BUY;
            confluenceScore = bullishPct;
        } else if (bullishPct >= 0.55) {
            direction = Direction.BUY;
            confluenceScore = bullishPct;
        } else if (bearishPct >= 0.75) {
            direction = Direction.STRONG_SELL;
            confluenceScore = bearishPct;
        } else if (bearishPct >= 0.55) {
            direction = Direction.SELL;
            confluenceScore = bearishPct;
        } else {
            direction = Direction.NEUTRAL;
            confluenceScore = Math.max(bullishPct, bearishPct);
        }

        // Generate options recommendations
        BigDecimal[] closePrices = data.stream().map(PriceData::close).toArray(BigDecimal[]::new);
        BigDecimal vol = BlackScholesCalculator.estimateVolatility(closePrices, Math.min(index + 1, VOLATILITY_LOOKBACK));

        OptionsRecommendation callRec = generateCallRecommendation(current, direction, vol, data, index);
        OptionsRecommendation putRec = generatePutRecommendation(current, direction, vol, data, index);

        // Support & Resistance
        SupportResistance sr = TechnicalIndicators.supportResistance(data, index, 100, 5);

        return new AlertResult(
                symbol, current.date(), current.close(),
                direction, confluenceScore, totalIndicators,
                bullish, bearish,
                callRec, putRec,
                sr.supportLevels(), sr.resistanceLevels(),
                current
        );
    }

    // =========================================================================
    // TREND SCORING
    // =========================================================================

    private static void scoreTrendIndicators(IndicatorValues current, IndicatorValues previous,
                                              List<String> bullish, List<String> bearish) {
        // EMA(12) vs EMA(26) crossover
        if (current.ema12().compareTo(current.ema26()) > 0) {
            bullish.add("EMA(12) > EMA(26) — short-term uptrend");
        } else if (current.ema12().compareTo(current.ema26()) < 0) {
            bearish.add("EMA(12) < EMA(26) — short-term downtrend");
        }

        // Price vs SMA(200) — long-term trend
        if (current.sma200().compareTo(BigDecimal.ZERO) > 0) {
            if (current.close().compareTo(current.sma200()) > 0) {
                bullish.add("Price above SMA(200) — long-term uptrend");
            } else {
                bearish.add("Price below SMA(200) — long-term downtrend");
            }
        }

        // SMA(50) vs SMA(200) — golden/death cross
        if (current.sma50().compareTo(BigDecimal.ZERO) > 0 && current.sma200().compareTo(BigDecimal.ZERO) > 0) {
            if (current.sma50().compareTo(current.sma200()) > 0
                    && previous.sma50().compareTo(previous.sma200()) <= 0) {
                bullish.add("Golden Cross — SMA(50) crossed above SMA(200)");
            } else if (current.sma50().compareTo(current.sma200()) < 0
                    && previous.sma50().compareTo(previous.sma200()) >= 0) {
                bearish.add("Death Cross — SMA(50) crossed below SMA(200)");
            }
        }

        // Ichimoku Cloud
        if (current.ichimokuSpanA().compareTo(BigDecimal.ZERO) > 0) {
            if (current.priceAboveCloud()) {
                bullish.add("Ichimoku — price above cloud (bullish)");
            } else {
                bearish.add("Ichimoku — price below cloud (bearish)");
            }

            // Tenkan/Kijun cross
            if (current.ichimokuTenkan().compareTo(current.ichimokuKijun()) > 0
                    && previous.ichimokuTenkan().compareTo(previous.ichimokuKijun()) <= 0) {
                bullish.add("Ichimoku TK Cross — Tenkan above Kijun (bullish)");
            } else if (current.ichimokuTenkan().compareTo(current.ichimokuKijun()) < 0
                    && previous.ichimokuTenkan().compareTo(previous.ichimokuKijun()) >= 0) {
                bearish.add("Ichimoku TK Cross — Tenkan below Kijun (bearish)");
            }
        }

        // Parabolic SAR
        if (current.sarBullish() && !previous.sarBullish()) {
            bullish.add("Parabolic SAR flipped bullish (SAR below price)");
        } else if (!current.sarBullish() && previous.sarBullish()) {
            bearish.add("Parabolic SAR flipped bearish (SAR above price)");
        }

        // ADX trend strength
        if (current.adx14().doubleValue() > 25) {
            if (current.plusDI().compareTo(current.minusDI()) > 0) {
                bullish.add("ADX > 25 with +DI > -DI — strong uptrend");
            } else {
                bearish.add("ADX > 25 with -DI > +DI — strong downtrend");
            }
        }
    }

    // =========================================================================
    // MOMENTUM SCORING
    // =========================================================================

    private static void scoreMomentumIndicators(IndicatorValues current, IndicatorValues previous,
                                                 List<String> bullish, List<String> bearish) {
        // RSI
        double rsi = current.rsi14().doubleValue();
        double prevRSI = previous.rsi14().doubleValue();
        if (rsi > 30 && prevRSI <= 30) {
            bullish.add("RSI(14) = %.1f — recovering from oversold".formatted(rsi));
        } else if (rsi < 70 && prevRSI >= 70) {
            bearish.add("RSI(14) = %.1f — falling from overbought".formatted(rsi));
        } else if (rsi < 30) {
            bullish.add("RSI(14) = %.1f — oversold (potential reversal up)".formatted(rsi));
        } else if (rsi > 70) {
            bearish.add("RSI(14) = %.1f — overbought (potential reversal down)".formatted(rsi));
        }

        // MACD crossover
        if (current.macdLine().compareTo(current.macdSignal()) > 0
                && previous.macdLine().compareTo(previous.macdSignal()) <= 0) {
            bullish.add("MACD bullish crossover (MACD > Signal)");
        } else if (current.macdLine().compareTo(current.macdSignal()) < 0
                && previous.macdLine().compareTo(previous.macdSignal()) >= 0) {
            bearish.add("MACD bearish crossover (MACD < Signal)");
        }

        // MACD histogram direction
        if (current.macdHistogram().compareTo(BigDecimal.ZERO) > 0
                && current.macdHistogram().compareTo(previous.macdHistogram()) > 0) {
            bullish.add("MACD histogram expanding positive");
        } else if (current.macdHistogram().compareTo(BigDecimal.ZERO) < 0
                && current.macdHistogram().compareTo(previous.macdHistogram()) < 0) {
            bearish.add("MACD histogram expanding negative");
        }

        // Stochastic
        double stochK = current.stochK().doubleValue();
        if (stochK < 20) {
            bullish.add("Stochastic %%K = %.1f — oversold".formatted(stochK));
        } else if (stochK > 80) {
            bearish.add("Stochastic %%K = %.1f — overbought".formatted(stochK));
        }

        // Williams %R
        double willR = current.williamsR().doubleValue();
        if (willR < -80) {
            bullish.add("Williams %%R = %.1f — oversold".formatted(willR));
        } else if (willR > -20) {
            bearish.add("Williams %%R = %.1f — overbought".formatted(willR));
        }

        // CCI
        double cci = current.cci20().doubleValue();
        if (cci < -100) {
            bullish.add("CCI(20) = %.1f — oversold".formatted(cci));
        } else if (cci > 100) {
            bearish.add("CCI(20) = %.1f — overbought".formatted(cci));
        }

        // ROC
        double roc = current.roc12().doubleValue();
        if (roc > 0 && previous.roc12().doubleValue() <= 0) {
            bullish.add("ROC(12) crossed positive — momentum turning up");
        } else if (roc < 0 && previous.roc12().doubleValue() >= 0) {
            bearish.add("ROC(12) crossed negative — momentum turning down");
        }
    }

    // =========================================================================
    // VOLUME SCORING
    // =========================================================================

    private static void scoreVolumeIndicators(IndicatorValues current, List<PriceData> data,
                                               int index, List<String> bullish, List<String> bearish) {
        // OBV
        if (current.obvRising()) {
            bullish.add("OBV rising — accumulation (buying pressure)");
        } else {
            bearish.add("OBV falling — distribution (selling pressure)");
        }

        // MFI
        double mfi = current.mfi14().doubleValue();
        if (mfi < 20) {
            bullish.add("MFI(14) = %.1f — oversold (volume-weighted)".formatted(mfi));
        } else if (mfi > 80) {
            bearish.add("MFI(14) = %.1f — overbought (volume-weighted)".formatted(mfi));
        }

        // Volume vs average
        BigDecimal avgVol = current.avgVolume20();
        if (avgVol.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal volRatio = BigDecimal.valueOf(current.volume())
                    .divide(avgVol, 4, RoundingMode.HALF_UP);
            if (volRatio.doubleValue() > 1.5) {
                // High volume confirms current trend
                if (current.close().compareTo(data.get(index - 1).close()) > 0) {
                    bullish.add("Volume %.1fx avg — confirming upward move".formatted(volRatio.doubleValue()));
                } else {
                    bearish.add("Volume %.1fx avg — confirming downward move".formatted(volRatio.doubleValue()));
                }
            }
        }
    }

    // =========================================================================
    // VOLATILITY SCORING
    // =========================================================================

    private static void scoreVolatilityIndicators(IndicatorValues current, IndicatorValues previous,
                                                   List<String> bullish, List<String> bearish) {
        // Bollinger Bands
        if (current.bbPercentB().doubleValue() <= 0) {
            bullish.add("Price at/below lower Bollinger Band — oversold");
        } else if (current.bbPercentB().doubleValue() >= 1) {
            bearish.add("Price at/above upper Bollinger Band — overbought");
        }

        // Price vs VWAP
        if (current.vwap().compareTo(BigDecimal.ZERO) > 0) {
            if (current.close().compareTo(current.vwap()) > 0) {
                bullish.add("Price above VWAP — bullish intraday bias");
            } else {
                bearish.add("Price below VWAP — bearish intraday bias");
            }
        }
    }

    // =========================================================================
    // OPTIONS RECOMMENDATIONS
    // =========================================================================

    private static OptionsRecommendation generateCallRecommendation(
            IndicatorValues current, Direction direction, BigDecimal vol,
            List<PriceData> data, int index) {

        BigDecimal price = current.close();

        if (direction == Direction.STRONG_BUY || direction == Direction.BUY) {
            // Recommend buying calls
            BigDecimal targetDelta = direction == Direction.STRONG_BUY
                    ? new BigDecimal("0.50")  // ATM for strong signal
                    : new BigDecimal("0.35"); // Slight OTM for moderate signal

            // Use nearest Fibonacci level or ATM
            BigDecimal strike = selectCallStrike(current, price);
            int dte = selectDTE(current.atr14(), price, direction);

            BigDecimal timeToExpiry = BigDecimal.valueOf(dte)
                    .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);
            BigDecimal premium = BlackScholesCalculator.price(
                    OptionType.CALL, price, strike, timeToExpiry, RISK_FREE_RATE, vol);

            String rationale = buildCallRationale(current, direction);

            return new OptionsRecommendation(
                    OptionType.CALL, Action.BUY, strike, dte, premium, targetDelta, rationale);

        } else if (direction == Direction.STRONG_SELL || direction == Direction.SELL) {
            // In a bearish scenario, sell calls (covered call / income)
            BigDecimal strike = price.multiply(new BigDecimal("1.05"))
                    .setScale(0, RoundingMode.CEILING);
            int dte = 30;

            BigDecimal timeToExpiry = BigDecimal.valueOf(dte)
                    .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);
            BigDecimal premium = BlackScholesCalculator.price(
                    OptionType.CALL, price, strike, timeToExpiry, RISK_FREE_RATE, vol);

            return new OptionsRecommendation(
                    OptionType.CALL, Action.SELL, strike, dte, premium,
                    new BigDecimal("0.30"),
                    "Bearish outlook — sell OTM covered call for income");

        } else {
            return new OptionsRecommendation(
                    OptionType.CALL, Action.HOLD, price, 0, BigDecimal.ZERO,
                    BigDecimal.ZERO, "Neutral — wait for clearer signal");
        }
    }

    private static OptionsRecommendation generatePutRecommendation(
            IndicatorValues current, Direction direction, BigDecimal vol,
            List<PriceData> data, int index) {

        BigDecimal price = current.close();

        if (direction == Direction.STRONG_SELL || direction == Direction.SELL) {
            // Recommend buying puts
            BigDecimal targetDelta = direction == Direction.STRONG_SELL
                    ? new BigDecimal("0.50")  // ATM
                    : new BigDecimal("0.35"); // Slight OTM

            BigDecimal strike = selectPutStrike(current, price);
            int dte = selectDTE(current.atr14(), price, direction);

            BigDecimal timeToExpiry = BigDecimal.valueOf(dte)
                    .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);
            BigDecimal premium = BlackScholesCalculator.price(
                    OptionType.PUT, price, strike, timeToExpiry, RISK_FREE_RATE, vol);

            String rationale = buildPutRationale(current, direction);

            return new OptionsRecommendation(
                    OptionType.PUT, Action.BUY, strike, dte, premium, targetDelta, rationale);

        } else if (direction == Direction.STRONG_BUY || direction == Direction.BUY) {
            // In a bullish scenario, sell puts (cash-secured put for income)
            BigDecimal strike = price.multiply(new BigDecimal("0.95"))
                    .setScale(0, RoundingMode.FLOOR);
            int dte = 30;

            BigDecimal timeToExpiry = BigDecimal.valueOf(dte)
                    .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);
            BigDecimal premium = BlackScholesCalculator.price(
                    OptionType.PUT, price, strike, timeToExpiry, RISK_FREE_RATE, vol);

            return new OptionsRecommendation(
                    OptionType.PUT, Action.SELL, strike, dte, premium,
                    new BigDecimal("-0.30"),
                    "Bullish outlook — sell OTM cash-secured put for income");

        } else {
            return new OptionsRecommendation(
                    OptionType.PUT, Action.HOLD, price, 0, BigDecimal.ZERO,
                    BigDecimal.ZERO, "Neutral — wait for clearer signal");
        }
    }

    private static BigDecimal selectCallStrike(IndicatorValues current, BigDecimal price) {
        // For strong signals, use ATM. Otherwise use nearest Fibonacci resistance or 2-3% OTM
        if (current.fib382().compareTo(BigDecimal.ZERO) > 0
                && current.fib382().compareTo(price) > 0
                && current.fib382().subtract(price).abs()
                .divide(price, 4, RoundingMode.HALF_UP).doubleValue() < 0.05) {
            return current.fib382().setScale(0, RoundingMode.CEILING);
        }
        // Default: ATM or slightly OTM
        return price.setScale(0, RoundingMode.CEILING);
    }

    private static BigDecimal selectPutStrike(IndicatorValues current, BigDecimal price) {
        // Use nearest Fibonacci support or ATM
        if (current.fib618().compareTo(BigDecimal.ZERO) > 0
                && current.fib618().compareTo(price) < 0
                && price.subtract(current.fib618())
                .divide(price, 4, RoundingMode.HALF_UP).doubleValue() < 0.05) {
            return current.fib618().setScale(0, RoundingMode.FLOOR);
        }
        return price.setScale(0, RoundingMode.FLOOR);
    }

    private static int selectDTE(BigDecimal atr, BigDecimal price, Direction direction) {
        // Strong signals get shorter DTE (more gamma), weaker signals get longer DTE
        if (direction == Direction.STRONG_BUY || direction == Direction.STRONG_SELL) {
            return 30; // Optimal theta/gamma balance
        }
        // Calculate ATR as % of price
        if (price.compareTo(BigDecimal.ZERO) > 0) {
            double atrPct = atr.divide(price, 6, RoundingMode.HALF_UP).doubleValue();
            if (atrPct > 0.03) {
                return 45; // High volatility: longer DTE to avoid theta decay
            }
        }
        return 30;
    }

    private static String buildCallRationale(IndicatorValues current, Direction direction) {
        var parts = new ArrayList<String>();
        if (current.rsi14().doubleValue() < 40) parts.add("RSI oversold");
        if (current.macdHistogram().compareTo(BigDecimal.ZERO) > 0) parts.add("MACD bullish");
        if (current.priceAboveCloud()) parts.add("above Ichimoku cloud");
        if (current.obvRising()) parts.add("OBV rising");
        if (current.sarBullish()) parts.add("SAR bullish");

        String strength = direction == Direction.STRONG_BUY ? "Strong" : "Moderate";
        return "%s bullish confluence: %s".formatted(strength, String.join(", ", parts));
    }

    private static String buildPutRationale(IndicatorValues current, Direction direction) {
        var parts = new ArrayList<String>();
        if (current.rsi14().doubleValue() > 60) parts.add("RSI overbought");
        if (current.macdHistogram().compareTo(BigDecimal.ZERO) < 0) parts.add("MACD bearish");
        if (!current.priceAboveCloud()) parts.add("below Ichimoku cloud");
        if (!current.obvRising()) parts.add("OBV falling");
        if (!current.sarBullish()) parts.add("SAR bearish");

        String strength = direction == Direction.STRONG_SELL ? "Strong" : "Moderate";
        return "%s bearish confluence: %s".formatted(strength, String.join(", ", parts));
    }

    private static AlertResult createNeutralAlert(String symbol, List<PriceData> data, int index) {
        PriceData bar = data.get(index);
        var noopCall = new OptionsRecommendation(OptionType.CALL, Action.HOLD,
                bar.close(), 0, BigDecimal.ZERO, BigDecimal.ZERO,
                "Insufficient data for analysis (need 200+ bars)");
        var noopPut = new OptionsRecommendation(OptionType.PUT, Action.HOLD,
                bar.close(), 0, BigDecimal.ZERO, BigDecimal.ZERO,
                "Insufficient data for analysis (need 200+ bars)");

        return new AlertResult(
                symbol, bar.date(), bar.close(),
                Direction.NEUTRAL, 0, 0,
                List.of(), List.of(),
                noopCall, noopPut,
                List.of(), List.of(),
                index >= 52 ? IndicatorValues.compute(data, index) : null
        );
    }
}
