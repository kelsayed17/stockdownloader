package com.stockdownloader.app;

import com.stockdownloader.backtest.BacktestEngine;
import com.stockdownloader.backtest.BacktestResult;
import com.stockdownloader.data.CsvPriceDataLoader;
import com.stockdownloader.data.YahooQuoteClient;
import com.stockdownloader.model.PriceData;
import com.stockdownloader.strategy.*;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main entry point for backtesting multiple trading strategies on SPY data.
 */
public final class SPYBacktestApp {

    private static final Logger LOGGER = Logger.getLogger(SPYBacktestApp.class.getName());
    private static final BigDecimal INITIAL_CAPITAL = new BigDecimal("100000.00");
    private static final BigDecimal COMMISSION = BigDecimal.ZERO;
    private static final int TRADING_DAYS_PER_YEAR = 252;
    private static final long FIVE_YEARS_IN_SECONDS = 5L * 365 * 24 * 60 * 60;

    private SPYBacktestApp() {}

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  SPY Trading Strategy Backtester");
        System.out.println("========================================");
        System.out.println();

        List<PriceData> data = args.length > 0
                ? loadFromFile(args[0])
                : downloadSPYData();

        if (data.isEmpty()) {
            System.out.println("ERROR: No price data loaded. Provide a CSV file path as argument:");
            System.out.println("  java -cp ... com.stockdownloader.app.SPYBacktestApp spy_data.csv");
            System.out.println();
            System.out.println("CSV format: Date,Open,High,Low,Close,Adj Close,Volume");
            return;
        }

        System.out.println("Loaded " + data.size() + " trading days");
        System.out.println("Date range: " + data.getFirst().date() + " to " + data.getLast().date());
        System.out.println("Starting capital: $" + INITIAL_CAPITAL.setScale(2, RoundingMode.HALF_UP));
        System.out.println();

        List<TradingStrategy> strategies = List.of(
                new SMACrossoverStrategy(50, 200),
                new SMACrossoverStrategy(20, 50),
                new RSIStrategy(14, 30, 70),
                new RSIStrategy(14, 25, 75),
                new MACDStrategy(12, 26, 9)
        );

        var engine = new BacktestEngine(INITIAL_CAPITAL, COMMISSION);
        List<BacktestResult> results = new ArrayList<>(strategies.size());

        for (TradingStrategy strategy : strategies) {
            System.out.println("Running backtest: " + strategy.getName() + "...");
            BacktestResult result = engine.run(strategy, data);
            results.add(result);
            result.printReport(data);
        }

        printComparison(results, data);
    }

    private static List<PriceData> loadFromFile(String filename) {
        System.out.println("Loading SPY data from file: " + filename);
        return CsvPriceDataLoader.loadFromFile(filename);
    }

    private static List<PriceData> downloadSPYData() {
        System.out.println("Loading SPY data from Yahoo Finance...");
        try {
            var client = new YahooQuoteClient();
            String crumb = client.getCrumb("SPY");

            if (crumb != null && !crumb.isEmpty()) {
                long endDate = System.currentTimeMillis() / 1000;
                long startDate = endDate - FIVE_YEARS_IN_SECONDS;

                String url = "https://query1.finance.yahoo.com/v7/finance/download/SPY?period1=%d&period2=%d&interval=1d&events=history&crumb=%s"
                        .formatted(startDate, endDate, crumb);

                try (InputStream input = URI.create(url).toURL().openStream()) {
                    List<PriceData> data = CsvPriceDataLoader.loadFromStream(input);
                    System.out.println("Downloaded " + data.size() + " days of SPY data");
                    return data;
                }
            } else {
                System.out.println("Could not obtain Yahoo Finance authentication crumb.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not download SPY data: {0}", e.getMessage());
        }
        System.out.println("Please provide a CSV file as argument.");
        return List.of();
    }

    private static void printComparison(List<BacktestResult> results, List<PriceData> data) {
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
