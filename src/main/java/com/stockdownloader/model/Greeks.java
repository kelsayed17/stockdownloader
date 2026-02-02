package com.stockdownloader.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Option Greeks representing sensitivity measures for an options contract.
 * Delta, gamma, theta, vega, and rho quantify how option price changes
 * with respect to underlying price, time, volatility, and interest rate.
 */
public record Greeks(
        BigDecimal delta,
        BigDecimal gamma,
        BigDecimal theta,
        BigDecimal vega,
        BigDecimal rho,
        BigDecimal impliedVolatility) {

    public Greeks {
        Objects.requireNonNull(delta, "delta must not be null");
        Objects.requireNonNull(gamma, "gamma must not be null");
        Objects.requireNonNull(theta, "theta must not be null");
        Objects.requireNonNull(vega, "vega must not be null");
        Objects.requireNonNull(rho, "rho must not be null");
        Objects.requireNonNull(impliedVolatility, "impliedVolatility must not be null");
    }

    public static Greeks zero() {
        return new Greeks(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    @Override
    public String toString() {
        return "Greeks[δ=%s, γ=%s, θ=%s, ν=%s, ρ=%s, IV=%s]".formatted(
                delta, gamma, theta, vega, rho, impliedVolatility);
    }
}
