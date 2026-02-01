package com.stockdownloader.backtest;

import com.stockdownloader.model.PriceData;
import com.stockdownloader.model.Trade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Formats and prints backtest reports to the console.
 * Separated from BacktestResult to keep result data and presentation concerns distinct.
 */
public final class BacktestReportFormatter {

    private static final int TRADING_DAYS_PER_YEAR = 252;

    private BacktestReportFormatter() {}

    public static void printReport(BacktestResult result, List<PriceData> data) {
        String separator = "=".repeat(70);
        String thinSep = "-".repeat(70);

        System.out.println();
        System.out.println(separator);
        System.out.println("  BACKTEST REPORT: " + result.getStrategyName());
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
        System.out.printf("  Buy & Hold Return:   %s%%%n", result.getBuyAndHoldReturn(data).setScale(2, RoundingMode.HALF_UP));
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

        List<Trade> closed = result.getClosedTrades();
        if (!closed.isEmpty()) {
            System.out.println(thinSep);
            System.out.println("  TRADE LOG");
            System.out.println(thinSep);
            int count = 1;
            for (Trade t : closed) {
                System.out.printf("  #%-4d %s%n", count++, t);
            }
        }

        System.out.println();
        System.out.println(separator);
    }

    public static void printComparison(List<BacktestResult> results, List<PriceData> data) {
        String separator = "=".repeat(90);
        String thinSep = "-".repeat(90);

        System.out.println();
        System.out.println(separator);
        System.out.println("  STRATEGY COMPARISON SUMMARY");
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

        System.out.printf("  %-35s %10s %10s %10s %10s %10s%n",
                "Strategy", "Return", "Sharpe", "MaxDD", "Trades", "Win Rate");
        System.out.println(thinSep);
        System.out.printf("  %-35s %9s%% %10s %9s%% %10s %10s%n",
                "Buy & Hold (Benchmark)",
                buyAndHold.setScale(2, RoundingMode.HALF_UP),
                "N/A", "N/A", "1", "N/A");

        for (BacktestResult r : results) {
            System.out.printf("  %-35s %9s%% %10s %9s%% %10d %9s%%%n",
                    r.getStrategyName(),
                    r.getTotalReturn().setScale(2, RoundingMode.HALF_UP),
                    r.getSharpeRatio(TRADING_DAYS_PER_YEAR),
                    r.getMaxDrawdown().setScale(2, RoundingMode.HALF_UP),
                    r.getTotalTrades(),
                    r.getWinRate().setScale(2, RoundingMode.HALF_UP));
        }

        System.out.println(thinSep);

        BacktestResult best = null;
        for (BacktestResult r : results) {
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
        System.out.println("  Past performance does not guarantee future results.");
        System.out.println("  Always do your own research before trading.");
        System.out.println();
    }
}
