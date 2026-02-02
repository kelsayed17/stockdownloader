package com.stockdownloader.app;

import com.stockdownloader.backtest.OptionsBacktestEngine;
import com.stockdownloader.backtest.OptionsBacktestReportFormatter;
import com.stockdownloader.backtest.OptionsBacktestResult;
import com.stockdownloader.data.CsvPriceDataLoader;
import com.stockdownloader.data.YahooDataClient;
import com.stockdownloader.model.PriceData;
import com.stockdownloader.strategy.CoveredCallStrategy;
import com.stockdownloader.strategy.OptionsStrategy;
import com.stockdownloader.strategy.ProtectivePutStrategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main entry point for backtesting options strategies on historical price data.
 * Supports dynamic data fetching from Yahoo Finance or loading from CSV.
 *
 * Usage:
 *   java -cp ... OptionsBacktestApp                  # Fetches SPY data
 *   java -cp ... OptionsBacktestApp AAPL             # Fetches AAPL data
 *   java -cp ... OptionsBacktestApp --csv data.csv   # Loads from CSV file
 */
public final class OptionsBacktestApp {

    private static final Logger LOGGER = Logger.getLogger(OptionsBacktestApp.class.getName());
    private static final BigDecimal INITIAL_CAPITAL = new BigDecimal("100000.00");
    private static final BigDecimal COMMISSION = new BigDecimal("0.65");

    private OptionsBacktestApp() {}

    public static void main(String[] args) {
        System.out.println("================================================");
        System.out.println("  Options Strategy Backtester");
        System.out.println("================================================");
        System.out.println();

        String symbol = "SPY";
        List<PriceData> data;

        if (args.length > 0 && "--csv".equals(args[0]) && args.length > 1) {
            System.out.println("Loading data from file: " + args[1]);
            data = CsvPriceDataLoader.loadFromFile(args[1]);
        } else {
            if (args.length > 0 && !args[0].startsWith("-")) {
                symbol = args[0].toUpperCase();
            }
            data = fetchData(symbol);
        }

        if (data.isEmpty()) {
            System.out.println("ERROR: No price data loaded.");
            System.out.println("Usage:");
            System.out.println("  OptionsBacktestApp                  # Fetch SPY from Yahoo Finance");
            System.out.println("  OptionsBacktestApp AAPL             # Fetch AAPL from Yahoo Finance");
            System.out.println("  OptionsBacktestApp --csv data.csv   # Load from CSV file");
            return;
        }

        System.out.println("Loaded " + data.size() + " trading days for " + symbol);
        System.out.println("Date range: " + data.getFirst().date() + " to " + data.getLast().date());
        System.out.println("Starting capital: $" + INITIAL_CAPITAL.setScale(2, RoundingMode.HALF_UP));
        System.out.println("Commission: $" + COMMISSION + " per contract");
        System.out.println();

        List<OptionsStrategy> strategies = List.of(
                // Covered calls: varying OTM% and DTE
                new CoveredCallStrategy(20, new BigDecimal("0.03"), 30, new BigDecimal("0.03")),
                new CoveredCallStrategy(20, new BigDecimal("0.05"), 30, new BigDecimal("0.03")),
                new CoveredCallStrategy(50, new BigDecimal("0.05"), 45, new BigDecimal("0.04")),
                // Protective puts: varying OTM% and DTE
                new ProtectivePutStrategy(20, new BigDecimal("0.05"), 30, 5),
                new ProtectivePutStrategy(20, new BigDecimal("0.03"), 45, 10),
                new ProtectivePutStrategy(50, new BigDecimal("0.05"), 60, 10)
        );

        var engine = new OptionsBacktestEngine(INITIAL_CAPITAL, COMMISSION);
        List<OptionsBacktestResult> results = new ArrayList<>(strategies.size());

        for (OptionsStrategy strategy : strategies) {
            System.out.println("Running options backtest: " + strategy.getName() + "...");
            OptionsBacktestResult result = engine.run(strategy, data);
            results.add(result);
            OptionsBacktestReportFormatter.printReport(result);
        }

        OptionsBacktestReportFormatter.printComparison(results);
    }

    private static List<PriceData> fetchData(String symbol) {
        System.out.println("Fetching %s data from Yahoo Finance...".formatted(symbol));
        try {
            var client = new YahooDataClient();
            List<PriceData> data = client.fetchPriceData(symbol, "5y", "1d");
            if (!data.isEmpty()) {
                System.out.println("Fetched " + data.size() + " days of " + symbol + " data");
                return data;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not fetch {0} data: {1}",
                    new Object[]{symbol, e.getMessage()});
        }
        return List.of();
    }
}
