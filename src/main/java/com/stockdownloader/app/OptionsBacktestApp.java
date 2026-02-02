package com.stockdownloader.app;

import com.stockdownloader.backtest.OptionsBacktestEngine;
import com.stockdownloader.backtest.OptionsBacktestReportFormatter;
import com.stockdownloader.backtest.OptionsBacktestResult;
import com.stockdownloader.data.CsvPriceDataLoader;
import com.stockdownloader.model.PriceData;
import com.stockdownloader.strategy.CoveredCallStrategy;
import com.stockdownloader.strategy.OptionsStrategy;
import com.stockdownloader.strategy.ProtectivePutStrategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Main entry point for backtesting options strategies on historical price data.
 * Tests covered calls and protective puts with various strike/expiration configurations.
 */
public final class OptionsBacktestApp {

    private static final BigDecimal INITIAL_CAPITAL = new BigDecimal("100000.00");
    private static final BigDecimal COMMISSION = new BigDecimal("0.65");

    private OptionsBacktestApp() {}

    public static void main(String[] args) {
        System.out.println("================================================");
        System.out.println("  Options Strategy Backtester");
        System.out.println("================================================");
        System.out.println();

        List<PriceData> data = args.length > 0
                ? CsvPriceDataLoader.loadFromFile(args[0])
                : List.of();

        if (data.isEmpty()) {
            System.out.println("ERROR: No price data loaded. Provide a CSV file path as argument:");
            System.out.println("  java -cp ... com.stockdownloader.app.OptionsBacktestApp spy_data.csv");
            System.out.println();
            System.out.println("CSV format: Date,Open,High,Low,Close,Adj Close,Volume");
            return;
        }

        System.out.println("Loaded " + data.size() + " trading days");
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
}
