package com.stockdownloader.app;

import com.stockdownloader.analysis.VolumeAnalyzer;
import com.stockdownloader.backtest.OptionsBacktestEngine;
import com.stockdownloader.backtest.OptionsBacktestResult;
import com.stockdownloader.backtest.OptionsReportFormatter;
import com.stockdownloader.data.CsvPriceDataLoader;
import com.stockdownloader.data.YahooOptionsClient;
import com.stockdownloader.data.YahooQuoteClient;
import com.stockdownloader.model.*;
import com.stockdownloader.strategy.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main entry point for options chain backtesting. Demonstrates the unified
 * data pipeline: loads equity price data, fetches options chain data,
 * runs volume analysis, and backtests multiple options strategies.
 *
 * Usage:
 *   java -cp ... com.stockdownloader.app.OptionsBacktestApp [csv_file] [symbol]
 *   - csv_file: path to historical OHLCV CSV (optional, downloads SPY if omitted)
 *   - symbol:   ticker symbol (default: SPY)
 */
public final class OptionsBacktestApp {

    private static final Logger LOGGER = Logger.getLogger(OptionsBacktestApp.class.getName());
    private static final BigDecimal INITIAL_CAPITAL = new BigDecimal("100000.00");
    private static final BigDecimal COMMISSION_PER_CONTRACT = new BigDecimal("0.65");
    private static final long FIVE_YEARS_IN_SECONDS = 5L * 365 * 24 * 60 * 60;

    private OptionsBacktestApp() {}

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  Options Chain Strategy Backtester");
        System.out.println("========================================");
        System.out.println();

        String symbol = args.length > 1 ? args[1] : "SPY";

        // Step 1: Load price data
        List<PriceData> priceData = args.length > 0
                ? loadFromFile(args[0])
                : downloadData(symbol);

        if (priceData.isEmpty()) {
            System.out.println("ERROR: No price data loaded. Provide a CSV file path as argument:");
            System.out.println("  java -cp ... com.stockdownloader.app.OptionsBacktestApp spy_data.csv SPY");
            System.out.println();
            System.out.println("CSV format: Date,Open,High,Low,Close,Adj Close,Volume");
            return;
        }

        System.out.println("Loaded " + priceData.size() + " trading days for " + symbol);
        System.out.println("Date range: " + priceData.getFirst().date() + " to " + priceData.getLast().date());
        System.out.println("Starting capital: $" + INITIAL_CAPITAL.setScale(2, RoundingMode.HALF_UP));
        System.out.println();

        // Step 2: Build volume profiles
        System.out.println("Building volume profiles...");
        List<VolumeProfile> volumeProfiles = VolumeAnalyzer.buildProfiles(symbol, priceData, null);
        System.out.println(VolumeAnalyzer.summarize(volumeProfiles));
        System.out.println();

        // Step 3: Compute volume indicators
        System.out.println("Computing volume indicators...");
        List<Long> obv = VolumeAnalyzer.computeOBV(priceData);
        List<BigDecimal> vwap = VolumeAnalyzer.computeVWAP(priceData);
        List<BigDecimal> mfi = VolumeAnalyzer.computeMFI(priceData, 14);

        if (!priceData.isEmpty()) {
            int last = priceData.size() - 1;
            System.out.printf("  Latest OBV:          %,d%n", obv.get(last));
            System.out.printf("  Latest VWAP:         $%s%n", vwap.get(last).setScale(2, RoundingMode.HALF_UP));
            System.out.printf("  Latest MFI(14):      %s%n", mfi.get(last).setScale(2, RoundingMode.HALF_UP));
        }

        // Step 4: Show unusual volume days
        List<VolumeProfile> unusualDays = VolumeAnalyzer.findUnusualVolume(
                volumeProfiles, new BigDecimal("2.0"));
        System.out.printf("  Unusual volume days (>2x avg): %d%n", unusualDays.size());
        System.out.println();

        // Step 5: Try to fetch live options chain
        System.out.println("Attempting to fetch live options chain for " + symbol + "...");
        OptionsChain optionsChain = fetchOptionsChain(symbol);
        if (optionsChain != null && !optionsChain.getCalls().isEmpty()) {
            printOptionsChainSummary(optionsChain);
        } else {
            System.out.println("  Options chain not available (using simulated premiums for backtest)");
        }
        System.out.println();

        // Step 6: Build unified market data for latest bar
        if (!priceData.isEmpty()) {
            int lastIdx = priceData.size() - 1;
            UnifiedMarketData unified = new UnifiedMarketData.Builder()
                    .symbol(symbol)
                    .date(priceData.get(lastIdx).date())
                    .priceData(priceData.get(lastIdx))
                    .optionsChain(optionsChain)
                    .historicalPrices(priceData, lastIdx)
                    .build();
            System.out.println("Unified Market Data: " + unified);
            System.out.println();
        }

        // Step 7: Run options backtests
        System.out.println("Running options strategy backtests...");
        System.out.println();

        List<OptionsStrategy> strategies = List.of(
                new CoveredCallStrategy(),
                new CoveredCallStrategy(50, new BigDecimal("3"), 45, 0),
                new ProtectivePutStrategy(),
                new ProtectivePutStrategy(20, new BigDecimal("5"), 45, 0, new BigDecimal("1.5")),
                new StraddleStrategy(),
                new StraddleStrategy(20, 45, 0, new BigDecimal("30"), new BigDecimal("0.03")),
                new IronCondorStrategy(),
                new IronCondorStrategy(20, new BigDecimal("7"), new BigDecimal("5"), 45, 0)
        );

        var engine = new OptionsBacktestEngine(INITIAL_CAPITAL, COMMISSION_PER_CONTRACT);
        List<OptionsBacktestResult> results = new ArrayList<>(strategies.size());

        for (OptionsStrategy strategy : strategies) {
            System.out.println("Running backtest: " + strategy.getName() + "...");
            OptionsBacktestResult result = engine.run(strategy, priceData);
            results.add(result);
            OptionsReportFormatter.printReport(result, priceData);
        }

        // Step 8: Print comparison
        OptionsReportFormatter.printComparison(results, priceData);
    }

    private static void printOptionsChainSummary(OptionsChain chain) {
        System.out.println("  Options Chain: " + chain);
        System.out.printf("  Total Call Volume:     %,d%n", chain.getTotalCallVolume());
        System.out.printf("  Total Put Volume:      %,d%n", chain.getTotalPutVolume());
        System.out.printf("  Put/Call Volume Ratio: %s%n", chain.getPutCallVolumeRatio());
        System.out.printf("  Total Call OI:         %,d%n", chain.getTotalCallOpenInterest());
        System.out.printf("  Total Put OI:          %,d%n", chain.getTotalPutOpenInterest());
        System.out.printf("  Put/Call OI Ratio:     %s%n", chain.getPutCallOIRatio());
        System.out.printf("  Expirations:           %d%n", chain.getExpirationDates().size());

        if (!chain.getExpirationDates().isEmpty()) {
            String nearestExp = chain.getExpirationDates().getFirst();
            System.out.printf("  Nearest Expiration:    %s%n", nearestExp);
            System.out.printf("  Max Pain Strike:       $%s%n",
                    chain.getMaxPainStrike(nearestExp).setScale(2, RoundingMode.HALF_UP));
        }
    }

    private static OptionsChain fetchOptionsChain(String symbol) {
        try {
            var client = new YahooOptionsClient();
            return client.downloadOptionsChain(symbol);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not fetch options chain: {0}", e.getMessage());
            return null;
        }
    }

    private static List<PriceData> loadFromFile(String filename) {
        System.out.println("Loading data from file: " + filename);
        return CsvPriceDataLoader.loadFromFile(filename);
    }

    private static List<PriceData> downloadData(String symbol) {
        System.out.println("Loading " + symbol + " data from Yahoo Finance...");
        try {
            var client = new YahooQuoteClient();
            String crumb = client.getCrumb(symbol);

            if (crumb != null && !crumb.isEmpty()) {
                long endDate = System.currentTimeMillis() / 1000;
                long startDate = endDate - FIVE_YEARS_IN_SECONDS;

                client.downloadData(symbol, startDate, endDate, crumb);

                List<PriceData> data = CsvPriceDataLoader.loadFromFile(symbol + ".csv");
                System.out.println("Downloaded " + data.size() + " days of " + symbol + " data");
                return data;
            } else {
                System.out.println("Could not obtain Yahoo Finance authentication crumb.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not download data: {0}", e.getMessage());
        }
        System.out.println("Please provide a CSV file as argument.");
        return List.of();
    }
}
