package com.stockdownloader.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Represents a trading alert generated from multi-indicator analysis.
 * Contains the signal direction, confidence level, contributing indicators,
 * and specific options recommendations (call/put with strike prices).
 */
public record AlertResult(
        String symbol,
        String date,
        BigDecimal currentPrice,
        Direction direction,
        double confluenceScore,
        int totalIndicators,
        List<String> bullishIndicators,
        List<String> bearishIndicators,
        OptionsRecommendation callRecommendation,
        OptionsRecommendation putRecommendation,
        List<BigDecimal> supportLevels,
        List<BigDecimal> resistanceLevels,
        IndicatorValues indicators
) {

    public enum Direction { STRONG_BUY, BUY, NEUTRAL, SELL, STRONG_SELL }

    /**
     * Options recommendation with strike, expiration, and rationale.
     */
    public record OptionsRecommendation(
            OptionType type,
            Action action,
            BigDecimal suggestedStrike,
            int suggestedDTE,
            BigDecimal estimatedPremium,
            BigDecimal targetDelta,
            String rationale
    ) {
        public enum Action { BUY, SELL, HOLD }

        @Override
        public String toString() {
            if (action == Action.HOLD) return "%s: No action recommended".formatted(type);
            return "%s %s $%s strike, %dDTE, est. premium $%s, delta %.2f - %s".formatted(
                    action, type,
                    suggestedStrike.setScale(2, RoundingMode.HALF_UP),
                    suggestedDTE,
                    estimatedPremium.setScale(2, RoundingMode.HALF_UP),
                    targetDelta.doubleValue(),
                    rationale);
        }
    }

    public String getSignalStrength() {
        return "%.0f%% (%d/%d indicators)".formatted(
                confluenceScore * 100, (int) (confluenceScore * totalIndicators), totalIndicators);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("=".repeat(80)).append("\n");
        sb.append("  TRADING ALERT: %s - %s\n".formatted(symbol, date));
        sb.append("=".repeat(80)).append("\n\n");

        sb.append("  Current Price:     $%s\n".formatted(currentPrice.setScale(2, RoundingMode.HALF_UP)));
        sb.append("  Signal:            %s\n".formatted(direction));
        sb.append("  Confluence:        %s\n".formatted(getSignalStrength()));
        sb.append("\n");

        sb.append("-".repeat(80)).append("\n");
        sb.append("  BULLISH INDICATORS (%d)\n".formatted(bullishIndicators.size()));
        sb.append("-".repeat(80)).append("\n");
        for (String ind : bullishIndicators) {
            sb.append("    + %s\n".formatted(ind));
        }
        sb.append("\n");

        sb.append("-".repeat(80)).append("\n");
        sb.append("  BEARISH INDICATORS (%d)\n".formatted(bearishIndicators.size()));
        sb.append("-".repeat(80)).append("\n");
        for (String ind : bearishIndicators) {
            sb.append("    - %s\n".formatted(ind));
        }
        sb.append("\n");

        sb.append("-".repeat(80)).append("\n");
        sb.append("  OPTIONS RECOMMENDATIONS\n");
        sb.append("-".repeat(80)).append("\n");
        sb.append("    CALL: %s\n".formatted(callRecommendation));
        sb.append("    PUT:  %s\n".formatted(putRecommendation));
        sb.append("\n");

        sb.append("-".repeat(80)).append("\n");
        sb.append("  SUPPORT & RESISTANCE\n");
        sb.append("-".repeat(80)).append("\n");
        if (!supportLevels.isEmpty()) {
            sb.append("    Support:    ");
            for (int i = 0; i < Math.min(3, supportLevels.size()); i++) {
                sb.append("$%s  ".formatted(supportLevels.get(i).setScale(2, RoundingMode.HALF_UP)));
            }
            sb.append("\n");
        }
        if (!resistanceLevels.isEmpty()) {
            sb.append("    Resistance: ");
            for (int i = 0; i < Math.min(3, resistanceLevels.size()); i++) {
                sb.append("$%s  ".formatted(resistanceLevels.get(i).setScale(2, RoundingMode.HALF_UP)));
            }
            sb.append("\n");
        }
        sb.append("\n");

        sb.append("-".repeat(80)).append("\n");
        sb.append("  INDICATOR SNAPSHOT\n");
        sb.append("-".repeat(80)).append("\n");
        sb.append(indicators.summary()).append("\n");

        sb.append("\n").append("=".repeat(80)).append("\n");
        sb.append("  DISCLAIMER: This is for educational purposes only.\n");
        sb.append("  Not financial advice. Always do your own research.\n");
        sb.append("=".repeat(80)).append("\n");

        return sb.toString();
    }
}
