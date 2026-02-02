package com.stockdownloader.app;

import com.stockdownloader.backtest.BacktestEngine;
import com.stockdownloader.backtest.BacktestReportFormatter;
import com.stockdownloader.backtest.BacktestResult;
import com.stockdownloader.data.CsvPriceDataLoader;
import com.stockdownloader.data.YahooDataClient;
import com.stockdownloader.model.PriceData;
import com.stockdownloader.strategy.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main entry point for backtesting multiple trading strategies on any symbol.
 * Supports dynamic data fetching from Yahoo Finance or loading from CSV.
 *
 * Usage:
 *   java -cp ... SPYBacktestApp                  # Fetches SPY data from Yahoo Finance
 *   java -cp ... SPYBacktestApp AAPL             # Fetches AAPL data from Yahoo Finance
 *   java -cp ... SPYBacktestApp --csv data.csv   # Loads from CSV file
 */
public final class SPYBacktestApp {

    private static final Logger LOGGER = Logger.getLogger(SPYBacktestApp.class.getName());
    private static final BigDecimal INITIAL_CAPITAL = new BigDecimal("100000.00");
    private static final BigDecimal COMMISSION = BigDecimal.ZERO;

    private SPYBacktestApp() {}

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  Trading Strategy Backtester");
        System.out.println("========================================");
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
            System.out.println("  SPYBacktestApp                  # Fetch SPY from Yahoo Finance");
            System.out.println("  SPYBacktestApp AAPL             # Fetch AAPL from Yahoo Finance");
            System.out.println("  SPYBacktestApp --csv data.csv   # Load from CSV file");
            return;
        }

        System.out.println("Loaded " + data.size() + " trading days for " + symbol);
        System.out.println("Date range: " + data.getFirst().date() + " to " + data.getLast().date());
        System.out.println("Starting capital: $" + INITIAL_CAPITAL.setScale(2, RoundingMode.HALF_UP));
        System.out.println();

        List<TradingStrategy> strategies = List.of(
                new SMACrossoverStrategy(50, 200),
                new SMACrossoverStrategy(20, 50),
                new RSIStrategy(14, 30, 70),
                new RSIStrategy(14, 25, 75),
                new MACDStrategy(12, 26, 9),
                new BollingerBandRSIStrategy(),
                new MomentumConfluenceStrategy(),
                new BreakoutStrategy(),
                new MultiIndicatorStrategy()
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
