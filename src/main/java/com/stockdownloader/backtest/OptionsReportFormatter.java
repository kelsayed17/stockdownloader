package com.stockdownloader.backtest;

import com.stockdownloader.model.OptionTrade;
import com.stockdownloader.model.PriceData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Formats and prints options backtest reports with options-specific metrics
 * including premium analysis, assignment rates, volume statistics,
 * and strategy comparison.
 */
public final class OptionsReportFormatter {

    private static final int TRADING_DAYS_PER_YEAR = 252;

    private OptionsReportFormatter() {}

    public static void printReport(OptionsBacktestResult result, List<PriceData> data) {
        String separator = "=".repeat(80);
        String thinSep = "-".repeat(80);

        System.out.println();
        System.out.println(separator);
        System.out.println("  OPTIONS BACKTEST REPORT: " + result.getStrategyName());
        System.out.println(separator);
        System.out.println();
        System.out.println("  Period:              " + result.getStartDate() + " to " + result.getEndDate());
        System.out.println("  Initial Capital:     $" + result.getInitialCapital().setScale(2, RoundingMode.HALF_UP));
        System.out.println("  Final Capital:       $" + result.getFinalCapital().setScale(2, RoundingMode.HALF_UP));
        System.out.println();

        System.out.println(thinSep);
        System.out.println("  PERFORMANCE METRICS");
        System.out.println(thinSep);
        System.out.printf("  Total Return:        %s%%%n", result.getTotalReturn().setScale(2, RoundingMode.HALF_UP));
        System.out.printf("  Total P/L:           $%s%n", result.getTotalProfitLoss().setScale(2, RoundingMode.HALF_UP));
        System.out.printf("  Sharpe Ratio:        %s%n", result.getSharpeRatio(TRADING_DAYS_PER_YEAR));
        System.out.printf("  Max Drawdown:        %s%%%n", result.getMaxDrawdown().setScale(2, RoundingMode.HALF_UP));
        System.out.printf("  Profit Factor:       %s%n", result.getProfitFactor());
        System.out.println();

        System.out.println(thinSep);
        System.out.println("  TRADE STATISTICS");
        System.out.println(thinSep);
        System.out.printf("  Total Trades:        %d%n", result.getTotalTrades());
        System.out.printf("  Winning Trades:      %d%n", result.getWinningTrades());
        System.out.printf("  Losing Trades:       %d%n", result.getLosingTrades());
        System.out.printf("  Win Rate:            %s%%%n", result.getWinRate().setScale(2, RoundingMode.HALF_UP));
        System.out.printf("  Average Win:         $%s%n", result.getAverageWin());
        System.out.printf("  Average Loss:        $%s%n", result.getAverageLoss());
        System.out.println();

        System.out.println(thinSep);
        System.out.println("  OPTIONS-SPECIFIC METRICS");
        System.out.println(thinSep);
        System.out.printf("  Premium Collected:   $%s%n", result.getTotalPremiumCollected().setScale(2, RoundingMode.HALF_UP));
        System.out.printf("  Premium Paid:        $%s%n", result.getTotalPremiumPaid().setScale(2, RoundingMode.HALF_UP));
        System.out.printf("  Net Premium:         $%s%n",
                result.getTotalPremiumCollected().subtract(result.getTotalPremiumPaid()).setScale(2, RoundingMode.HALF_UP));
        System.out.printf("  Assigned:            %d%n", result.getAssignedCount());
        System.out.printf("  Expired Worthless:   %d%n", result.getExpiredWorthlessCount());
        System.out.printf("  Assignment Rate:     %s%%%n", result.getAssignmentRate().setScale(2, RoundingMode.HALF_UP));
        System.out.printf("  Call Trades:         %d%n", result.getTotalCallTrades());
        System.out.printf("  Put Trades:          %d%n", result.getTotalPutTrades());
        System.out.println();

        System.out.println(thinSep);
        System.out.println("  VOLUME ANALYSIS");
        System.out.println(thinSep);
        System.out.printf("  Avg Entry Volume:    %,d%n", result.getAverageEntryVolume());
        System.out.println();

        List<OptionTrade> closed = result.getClosedTrades();
        if (!closed.isEmpty()) {
            System.out.println(thinSep);
            System.out.println("  TRADE LOG");
            System.out.println(thinSep);
            int count = 1;
            for (OptionTrade t : closed) {
                System.out.printf("  #%-4d %s%n", count++, t);
            }
        }

        System.out.println();
        System.out.println(separator);
    }

    public static void printComparison(List<OptionsBacktestResult> results, List<PriceData> data) {
        String separator = "=".repeat(110);
        String thinSep = "-".repeat(110);

        System.out.println();
        System.out.println(separator);
        System.out.println("  OPTIONS STRATEGY COMPARISON SUMMARY");
        System.out.println(separator);
        System.out.println();

        BigDecimal buyAndHold = BigDecimal.ZERO;
        if (!data.isEmpty()) {
            BigDecimal first = data.getFirst().close();
            BigDecimal last = data.getLast().close();
            buyAndHold = last.subtract(first)
                    .divide(first, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        System.out.printf("  %-40s %10s %8s %8s %8s %8s %10s %10s%n",
                "Strategy", "Return", "Sharpe", "MaxDD", "Trades", "WinRate", "Premium$", "AvgVol");
        System.out.println(thinSep);
        System.out.printf("  %-40s %9s%% %8s %8s %8s %8s %10s %10s%n",
                "Buy & Hold (Benchmark)",
                buyAndHold.setScale(2, RoundingMode.HALF_UP),
                "N/A", "N/A", "1", "N/A", "N/A", "N/A");

        for (OptionsBacktestResult r : results) {
            BigDecimal netPremium = r.getTotalPremiumCollected().subtract(r.getTotalPremiumPaid());
            System.out.printf("  %-40s %9s%% %8s %7s%% %8d %7s%% %10s %,10d%n",
                    r.getStrategyName(),
                    r.getTotalReturn().setScale(2, RoundingMode.HALF_UP),
                    r.getSharpeRatio(TRADING_DAYS_PER_YEAR),
                    r.getMaxDrawdown().setScale(2, RoundingMode.HALF_UP),
                    r.getTotalTrades(),
                    r.getWinRate().setScale(2, RoundingMode.HALF_UP),
                    "$" + netPremium.setScale(0, RoundingMode.HALF_UP),
                    r.getAverageEntryVolume());
        }

        System.out.println(thinSep);

        OptionsBacktestResult best = null;
        for (OptionsBacktestResult r : results) {
            if (best == null || r.getTotalReturn().compareTo(best.getTotalReturn()) > 0) {
                best = r;
            }
        }

        if (best != null) {
            System.out.println();
            System.out.println("  Best performing options strategy: " + best.getStrategyName());
            System.out.printf("  Return: %s%% | Sharpe: %s | Max Drawdown: %s%% | Win Rate: %s%%%n",
                    best.getTotalReturn().setScale(2, RoundingMode.HALF_UP),
                    best.getSharpeRatio(TRADING_DAYS_PER_YEAR),
                    best.getMaxDrawdown().setScale(2, RoundingMode.HALF_UP),
                    best.getWinRate().setScale(2, RoundingMode.HALF_UP));

            BigDecimal diff = best.getTotalReturn().subtract(buyAndHold).setScale(2, RoundingMode.HALF_UP);
            if (best.getTotalReturn().compareTo(buyAndHold) > 0) {
                System.out.println("  >> Outperformed Buy & Hold by " + diff + " percentage points");
            } else {
                System.out.println("  >> Underperformed Buy & Hold by " + diff.abs() + " percentage points");
            }
        }

        System.out.println();
        System.out.println(separator);
        System.out.println();
        System.out.println("  DISCLAIMER: This is for educational purposes only.");
        System.out.println("  Options trading involves substantial risk of loss.");
        System.out.println("  Past performance does not guarantee future results.");
        System.out.println("  Always do your own research before trading options.");
        System.out.println();
    }
}
