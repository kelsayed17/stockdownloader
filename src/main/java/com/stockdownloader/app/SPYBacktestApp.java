package com.stockdownloader.app;

import com.stockdownloader.backtest.BacktestEngine;
import com.stockdownloader.backtest.BacktestReportFormatter;
import com.stockdownloader.backtest.BacktestResult;
import com.stockdownloader.data.CsvPriceDataLoader;
import com.stockdownloader.data.YahooQuoteClient;
import com.stockdownloader.model.PriceData;
import com.stockdownloader.strategy.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
            BacktestReportFormatter.printReport(result, data);
        }

        BacktestReportFormatter.printComparison(results, data);
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

                client.downloadData("SPY", startDate, endDate, crumb);

                List<PriceData> data = CsvPriceDataLoader.loadFromFile("SPY.csv");
                System.out.println("Downloaded " + data.size() + " days of SPY data");
                return data;
            } else {
                System.out.println("Could not obtain Yahoo Finance authentication crumb.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not download SPY data: {0}", e.getMessage());
        }
        System.out.println("Please provide a CSV file as argument.");
        return List.of();
    }
}
