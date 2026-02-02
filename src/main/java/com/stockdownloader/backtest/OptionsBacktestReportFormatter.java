package com.stockdownloader.backtest;

import com.stockdownloader.model.OptionsTrade;
import com.stockdownloader.model.PriceData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Formats and prints options backtest reports to the console.
 * Includes options-specific metrics like premium collected, volume traded, and theta decay.
 */
public final class OptionsBacktestReportFormatter {

    private static final int TRADING_DAYS_PER_YEAR = 252;

    private OptionsBacktestReportFormatter() {}

    public static void printReport(OptionsBacktestResult result) {
        String sep = "=".repeat(80);
        String thin = "-".repeat(80);

        System.out.println();
        System.out.println(sep);
        System.out.println("  OPTIONS BACKTEST REPORT: " + result.getStrategyName());
        System.out.println(sep);
        System.out.println();
        System.out.println("  Period:              " + result.getStartDate() + " to " + result.getEndDate());
        System.out.println("  Initial Capital:     $" + result.getInitialCapital().setScale(2, RoundingMode.HALF_UP));
        System.out.println("  Final Capital:       $" + result.getFinalCapital().setScale(2, RoundingMode.HALF_UP));
        System.out.println();

        System.out.println(thin);
        System.out.println("  PERFORMANCE METRICS");
        System.out.println(thin);
        System.out.printf("  Total Return:        %s%%%n", result.getTotalReturn().setScale(2, RoundingMode.HALF_UP));
        System.out.printf("  Total P/L:           $%s%n", result.getTotalProfitLoss().setScale(2, RoundingMode.HALF_UP));
        System.out.printf("  Sharpe Ratio:        %s%n", result.getSharpeRatio(TRADING_DAYS_PER_YEAR));
        System.out.printf("  Max Drawdown:        %s%%%n", result.getMaxDrawdown().setScale(2, RoundingMode.HALF_UP));
        System.out.printf("  Profit Factor:       %s%n", result.getProfitFactor());
        System.out.println();

        System.out.println(thin);
        System.out.println("  TRADE STATISTICS");
        System.out.println(thin);
        System.out.printf("  Total Trades:        %d%n", result.getTotalTrades());
        System.out.printf("  Winning Trades:      %d%n", result.getWinningTrades());
        System.out.printf("  Losing Trades:       %d%n", result.getLosingTrades());
        System.out.printf("  Win Rate:            %s%%%n", result.getWinRate().setScale(2, RoundingMode.HALF_UP));
        System.out.printf("  Average Win:         $%s%n", result.getAverageWin());
        System.out.printf("  Average Loss:        $%s%n", result.getAverageLoss());
        System.out.println();

        System.out.println(thin);
        System.out.println("  OPTIONS-SPECIFIC METRICS");
        System.out.println(thin);
        System.out.printf("  Avg Premium/Trade:   $%s%n", result.getAveragePremiumCollected());
        System.out.printf("  Total Volume:        %,d contracts%n", result.getTotalVolumeTraded());
        System.out.println();

        List<OptionsTrade> closed = result.getClosedTrades();
        if (!closed.isEmpty()) {
            System.out.println(thin);
            System.out.println("  TRADE LOG");
            System.out.println(thin);
            int count = 1;
            for (OptionsTrade t : closed) {
                System.out.printf("  #%-4d %s%n", count++, t);
            }
        }

        System.out.println();
        System.out.println(sep);
    }

    public static void printComparison(List<OptionsBacktestResult> results) {
        String sep = "=".repeat(100);
        String thin = "-".repeat(100);

        System.out.println();
        System.out.println(sep);
        System.out.println("  OPTIONS STRATEGY COMPARISON");
        System.out.println(sep);
        System.out.println();

        System.out.printf("  %-40s %10s %10s %10s %10s %10s %10s%n",
                "Strategy", "Return", "Sharpe", "MaxDD", "Trades", "WinRate", "Volume");
        System.out.println(thin);

        for (OptionsBacktestResult r : results) {
            System.out.printf("  %-40s %9s%% %10s %9s%% %10d %9s%% %,10d%n",
                    r.getStrategyName(),
                    r.getTotalReturn().setScale(2, RoundingMode.HALF_UP),
                    r.getSharpeRatio(TRADING_DAYS_PER_YEAR),
                    r.getMaxDrawdown().setScale(2, RoundingMode.HALF_UP),
                    r.getTotalTrades(),
                    r.getWinRate().setScale(2, RoundingMode.HALF_UP),
                    r.getTotalVolumeTraded());
        }

        System.out.println(thin);

        OptionsBacktestResult best = null;
        for (OptionsBacktestResult r : results) {
            if (best == null || r.getTotalReturn().compareTo(best.getTotalReturn()) > 0) {
                best = r;
            }
        }

        if (best != null) {
            System.out.println();
            System.out.println("  Best performing strategy: " + best.getStrategyName());
            System.out.printf("  Return: %s%% | Sharpe: %s | Max Drawdown: %s%%%n",
                    best.getTotalReturn().setScale(2, RoundingMode.HALF_UP),
                    best.getSharpeRatio(TRADING_DAYS_PER_YEAR),
                    best.getMaxDrawdown().setScale(2, RoundingMode.HALF_UP));
        }

        System.out.println();
        System.out.println(sep);
        System.out.println();
        System.out.println("  DISCLAIMER: This is for educational purposes only.");
        System.out.println("  Options trading involves significant risk. Past performance");
        System.out.println("  does not guarantee future results.");
        System.out.println();
    }
}
