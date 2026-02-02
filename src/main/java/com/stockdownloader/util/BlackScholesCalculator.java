package com.stockdownloader.util;

import com.stockdownloader.model.OptionType;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Black-Scholes option pricing model for synthetic option price estimation.
 * Used by the options backtesting engine to estimate option prices from
 * underlying price data when historical options data is not available.
 */
public final class BlackScholesCalculator {

    private static final MathContext MC = new MathContext(10);
    private static final BigDecimal TWO = BigDecimal.valueOf(2);
    private static final BigDecimal SQRT_TWO_PI = BigDecimal.valueOf(Math.sqrt(2 * Math.PI));

    private BlackScholesCalculator() {}

    /**
     * Calculate the theoretical option price using the Black-Scholes formula.
     *
     * @param type          CALL or PUT
     * @param spot          current underlying price
     * @param strike        option strike price
     * @param timeToExpiry  time to expiration in years (e.g., 30/365 for 30 days)
     * @param riskFreeRate  annual risk-free interest rate (e.g., 0.05 for 5%)
     * @param volatility    annualized implied volatility (e.g., 0.20 for 20%)
     * @return theoretical option price
     */
    public static BigDecimal price(OptionType type, BigDecimal spot, BigDecimal strike,
                                   BigDecimal timeToExpiry, BigDecimal riskFreeRate,
                                   BigDecimal volatility) {
        if (timeToExpiry.compareTo(BigDecimal.ZERO) <= 0) {
            // At or past expiration: intrinsic value only
            return intrinsicValue(type, spot, strike);
        }
        if (volatility.compareTo(BigDecimal.ZERO) <= 0) {
            return intrinsicValue(type, spot, strike);
        }

        double S = spot.doubleValue();
        double K = strike.doubleValue();
        double T = timeToExpiry.doubleValue();
        double r = riskFreeRate.doubleValue();
        double sigma = volatility.doubleValue();

        double sqrtT = Math.sqrt(T);
        double d1 = (Math.log(S / K) + (r + sigma * sigma / 2.0) * T) / (sigma * sqrtT);
        double d2 = d1 - sigma * sqrtT;

        double result;
        if (type == OptionType.CALL) {
            result = S * cnd(d1) - K * Math.exp(-r * T) * cnd(d2);
        } else {
            result = K * Math.exp(-r * T) * cnd(-d2) - S * cnd(-d1);
        }

        return BigDecimal.valueOf(Math.max(result, 0)).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Calculate delta: rate of change of option price with respect to underlying price.
     */
    public static BigDecimal delta(OptionType type, BigDecimal spot, BigDecimal strike,
                                   BigDecimal timeToExpiry, BigDecimal riskFreeRate,
                                   BigDecimal volatility) {
        if (timeToExpiry.compareTo(BigDecimal.ZERO) <= 0 || volatility.compareTo(BigDecimal.ZERO) <= 0) {
            if (type == OptionType.CALL) {
                return spot.compareTo(strike) > 0 ? BigDecimal.ONE : BigDecimal.ZERO;
            } else {
                return spot.compareTo(strike) < 0 ? BigDecimal.ONE.negate() : BigDecimal.ZERO;
            }
        }

        double S = spot.doubleValue();
        double K = strike.doubleValue();
        double T = timeToExpiry.doubleValue();
        double r = riskFreeRate.doubleValue();
        double sigma = volatility.doubleValue();

        double d1 = (Math.log(S / K) + (r + sigma * sigma / 2.0) * T) / (sigma * Math.sqrt(T));

        double d;
        if (type == OptionType.CALL) {
            d = cnd(d1);
        } else {
            d = cnd(d1) - 1.0;
        }

        return BigDecimal.valueOf(d).setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * Calculate theta: time decay per day.
     */
    public static BigDecimal theta(OptionType type, BigDecimal spot, BigDecimal strike,
                                   BigDecimal timeToExpiry, BigDecimal riskFreeRate,
                                   BigDecimal volatility) {
        if (timeToExpiry.compareTo(BigDecimal.ZERO) <= 0 || volatility.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        double S = spot.doubleValue();
        double K = strike.doubleValue();
        double T = timeToExpiry.doubleValue();
        double r = riskFreeRate.doubleValue();
        double sigma = volatility.doubleValue();

        double sqrtT = Math.sqrt(T);
        double d1 = (Math.log(S / K) + (r + sigma * sigma / 2.0) * T) / (sigma * sqrtT);
        double d2 = d1 - sigma * sqrtT;

        double nd1 = Math.exp(-d1 * d1 / 2.0) / Math.sqrt(2 * Math.PI);

        double dailyTheta;
        if (type == OptionType.CALL) {
            dailyTheta = (-S * nd1 * sigma / (2 * sqrtT) - r * K * Math.exp(-r * T) * cnd(d2)) / 365.0;
        } else {
            dailyTheta = (-S * nd1 * sigma / (2 * sqrtT) + r * K * Math.exp(-r * T) * cnd(-d2)) / 365.0;
        }

        return BigDecimal.valueOf(dailyTheta).setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * Estimate implied volatility from historical price data.
     * Uses the standard deviation of log returns annualized.
     */
    public static BigDecimal estimateVolatility(BigDecimal[] closePrices, int lookback) {
        if (closePrices == null || closePrices.length < 2) {
            return BigDecimal.valueOf(0.20); // default 20% IV
        }
        int start = Math.max(0, closePrices.length - lookback);
        int count = closePrices.length - start - 1;
        if (count < 1) return BigDecimal.valueOf(0.20);

        double[] logReturns = new double[count];
        for (int i = 0; i < count; i++) {
            double prev = closePrices[start + i].doubleValue();
            double curr = closePrices[start + i + 1].doubleValue();
            if (prev > 0) {
                logReturns[i] = Math.log(curr / prev);
            }
        }

        double mean = 0;
        for (double lr : logReturns) mean += lr;
        mean /= logReturns.length;

        double sumSqDiff = 0;
        for (double lr : logReturns) {
            sumSqDiff += Math.pow(lr - mean, 2);
        }
        double dailyVol = Math.sqrt(sumSqDiff / logReturns.length);

        // Annualize
        double annualVol = dailyVol * Math.sqrt(252);
        return BigDecimal.valueOf(Math.max(annualVol, 0.01)).setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * Intrinsic value of an option.
     */
    public static BigDecimal intrinsicValue(OptionType type, BigDecimal spot, BigDecimal strike) {
        BigDecimal diff;
        if (type == OptionType.CALL) {
            diff = spot.subtract(strike);
        } else {
            diff = strike.subtract(spot);
        }
        return diff.max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Cumulative standard normal distribution function.
     */
    private static double cnd(double x) {
        // Abramowitz and Stegun approximation
        double a1 = 0.254829592;
        double a2 = -0.284496736;
        double a3 = 1.421413741;
        double a4 = -1.453152027;
        double a5 = 1.061405429;
        double p = 0.3275911;

        int sign = x < 0 ? -1 : 1;
        x = Math.abs(x) / Math.sqrt(2);

        double t = 1.0 / (1.0 + p * x);
        double y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * Math.exp(-x * x);

        return 0.5 * (1.0 + sign * y);
    }
}
