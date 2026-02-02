package com.stockdownloader.app;

import com.stockdownloader.analysis.SignalGenerator;
import com.stockdownloader.backtest.BacktestEngine;
import com.stockdownloader.backtest.BacktestReportFormatter;
import com.stockdownloader.backtest.BacktestResult;
import com.stockdownloader.backtest.OptionsBacktestEngine;
import com.stockdownloader.backtest.OptionsBacktestReportFormatter;
import com.stockdownloader.backtest.OptionsBacktestResult;
import com.stockdownloader.data.YahooDataClient;
import com.stockdownloader.model.AlertResult;
import com.stockdownloader.model.PriceData;
import com.stockdownloader.strategy.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Unified entry point for dynamic symbol analysis, backtesting, and alert generation.
 *
 * Usage: java -cp ... com.stockdownloader.app.SymbolAnalysisApp SYMBOL [RANGE]
 *
 * Fetches live data from Yahoo Finance for any symbol, then:
 * 1. Displays current indicator snapshot
 * 2. Generates trading alerts with buy/sell signals
 * 3. Provides options recommendations (calls, puts, strike prices)
 * 4. Runs all equity strategies as backtests
 * 5. Runs all options strategies as backtests
 * 6. Compares results
 *
 * Supported ranges: 1mo, 3mo, 6mo, 1y, 2y, 5y, 10y, max (default: 5y)
 */
public final class SymbolAnalysisApp {

    private static final Logger LOGGER = Logger.getLogger(SymbolAnalysisApp.class.getName());
    private static final BigDecimal INITIAL_CAPITAL = new BigDecimal("100000.00");
    private static final BigDecimal EQUITY_COMMISSION = BigDecimal.ZERO;
    private static final BigDecimal OPTIONS_COMMISSION = new BigDecimal("0.65");

    private SymbolAnalysisApp() {}

    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            return;
        }

        String symbol = args[0].toUpperCase();
        String range = args.length > 1 ? args[1] : "5y";

        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║           STOCK ANALYSIS & BACKTESTING PLATFORM                 ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        System.out.println();

        // === PHASE 1: Fetch Data ===
        System.out.println("Fetching %s data from Yahoo Finance (range: %s)...".formatted(symbol, range));
        var client = new YahooDataClient();
        List<PriceData> data = client.fetchPriceData(symbol, range, "1d");

        if (data.isEmpty()) {
            System.out.println("ERROR: Could not fetch data for symbol '%s'.".formatted(symbol));
            System.out.println("Verify the symbol is valid and try again.");
            return;
        }

        System.out.println("Loaded %d trading days for %s".formatted(data.size(), symbol));
        System.out.println("Date range: %s to %s".formatted(data.getFirst().date(), data.getLast().date()));
        System.out.printf("Current price: $%s%n", data.getLast().close().setScale(2, RoundingMode.HALF_UP));
        System.out.println("Starting capital: $%s".formatted(INITIAL_CAPITAL.setScale(2, RoundingMode.HALF_UP)));
        System.out.println();

        // === PHASE 2: Generate Alerts ===
        System.out.println("Analyzing indicators and generating signals...");
        System.out.println();
        AlertResult alert = SignalGenerator.generateAlert(symbol, data);
        System.out.println(alert);

        // === PHASE 3: Run Equity Backtests ===
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    EQUITY STRATEGY BACKTESTS                    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        System.out.println();

        List<TradingStrategy> equityStrategies = List.of(
                // Classic strategies
                new SMACrossoverStrategy(50, 200),
                new SMACrossoverStrategy(20, 50),
                new RSIStrategy(14, 30, 70),
                new RSIStrategy(14, 25, 75),
                new MACDStrategy(12, 26, 9),
                // New multi-indicator strategies
                new BollingerBandRSIStrategy(),
                new MomentumConfluenceStrategy(),
                new BreakoutStrategy(),
                new MultiIndicatorStrategy()
        );

        var equityEngine = new BacktestEngine(INITIAL_CAPITAL, EQUITY_COMMISSION);
        List<BacktestResult> equityResults = new ArrayList<>(equityStrategies.size());

        for (TradingStrategy strategy : equityStrategies) {
            try {
                System.out.println("Running: %s...".formatted(strategy.getName()));
                BacktestResult result = equityEngine.run(strategy, data);
                equityResults.add(result);
                BacktestReportFormatter.printReport(result, data);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to run strategy {0}: {1}",
                        new Object[]{strategy.getName(), e.getMessage()});
            }
        }

        if (!equityResults.isEmpty()) {
            BacktestReportFormatter.printComparison(equityResults, data);
        }

        // === PHASE 4: Run Options Backtests ===
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                   OPTIONS STRATEGY BACKTESTS                    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        System.out.println();

        List<OptionsStrategy> optionsStrategies = List.of(
                new CoveredCallStrategy(20, new BigDecimal("0.03"), 30, new BigDecimal("0.03")),
                new CoveredCallStrategy(20, new BigDecimal("0.05"), 30, new BigDecimal("0.03")),
                new CoveredCallStrategy(50, new BigDecimal("0.05"), 45, new BigDecimal("0.04")),
                new ProtectivePutStrategy(20, new BigDecimal("0.05"), 30, 5),
                new ProtectivePutStrategy(20, new BigDecimal("0.03"), 45, 10),
                new ProtectivePutStrategy(50, new BigDecimal("0.05"), 60, 10)
        );

        var optionsEngine = new OptionsBacktestEngine(INITIAL_CAPITAL, OPTIONS_COMMISSION);
        List<OptionsBacktestResult> optionsResults = new ArrayList<>(optionsStrategies.size());

        for (OptionsStrategy strategy : optionsStrategies) {
            try {
                System.out.println("Running: %s...".formatted(strategy.getName()));
                OptionsBacktestResult result = optionsEngine.run(strategy, data);
                optionsResults.add(result);
                OptionsBacktestReportFormatter.printReport(result);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to run options strategy {0}: {1}",
                        new Object[]{strategy.getName(), e.getMessage()});
            }
        }

        if (!optionsResults.isEmpty()) {
            OptionsBacktestReportFormatter.printComparison(optionsResults);
        }

        // === PHASE 5: Summary ===
        printSummary(symbol, data, alert, equityResults, optionsResults);
    }

    private static void printSummary(String symbol, List<PriceData> data,
                                      AlertResult alert,
                                      List<BacktestResult> equityResults,
                                      List<OptionsBacktestResult> optionsResults) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      ANALYSIS SUMMARY                           ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("  Symbol:            %s".formatted(symbol));
        System.out.printf("  Current Price:     $%s%n", data.getLast().close().setScale(2, RoundingMode.HALF_UP));
        System.out.println("  Signal:            %s".formatted(alert.direction()));
        System.out.println("  Confluence:        %s".formatted(alert.getSignalStrength()));
        System.out.println();

        System.out.println("  Call Recommendation:");
        System.out.println("    %s".formatted(alert.callRecommendation()));
        System.out.println("  Put Recommendation:");
        System.out.println("    %s".formatted(alert.putRecommendation()));
        System.out.println();

        // Best equity strategy
        BacktestResult bestEquity = null;
        for (BacktestResult r : equityResults) {
            if (bestEquity == null || r.getTotalReturn().compareTo(bestEquity.getTotalReturn()) > 0) {
                bestEquity = r;
            }
        }
        if (bestEquity != null) {
            System.out.printf("  Best Equity Strategy:    %s (%s%% return)%n",
                    bestEquity.getStrategyName(),
                    bestEquity.getTotalReturn().setScale(2, RoundingMode.HALF_UP));
        }

        // Best options strategy
        OptionsBacktestResult bestOptions = null;
        for (OptionsBacktestResult r : optionsResults) {
            if (bestOptions == null || r.getTotalReturn().compareTo(bestOptions.getTotalReturn()) > 0) {
                bestOptions = r;
            }
        }
        if (bestOptions != null) {
            System.out.printf("  Best Options Strategy:   %s (%s%% return)%n",
                    bestOptions.getStrategyName(),
                    bestOptions.getTotalReturn().setScale(2, RoundingMode.HALF_UP));
        }

        System.out.println();
        System.out.println("  DISCLAIMER: This is for educational purposes only.");
        System.out.println("  Not financial advice. Past performance does not guarantee future results.");
        System.out.println("  Always do your own research before trading.");
        System.out.println();
    }

    private static void printUsage() {
        System.out.println("Stock Analysis & Backtesting Platform");
        System.out.println();
        System.out.println("Usage: SymbolAnalysisApp SYMBOL [RANGE]");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  SYMBOL   Ticker symbol to analyze (e.g., AAPL, SPY, TSLA, MSFT)");
        System.out.println("  RANGE    Historical data range (default: 5y)");
        System.out.println("           Options: 1mo, 3mo, 6mo, 1y, 2y, 5y, 10y, max");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  SymbolAnalysisApp AAPL          # Analyze Apple with 5 years data");
        System.out.println("  SymbolAnalysisApp SPY 2y        # Analyze SPY with 2 years data");
        System.out.println("  SymbolAnalysisApp TSLA 1y       # Analyze Tesla with 1 year data");
        System.out.println("  SymbolAnalysisApp MSFT max      # Analyze Microsoft with all data");
        System.out.println();
        System.out.println("The tool will:");
        System.out.println("  1. Fetch live data from Yahoo Finance");
        System.out.println("  2. Compute 20+ technical indicators");
        System.out.println("  3. Generate buy/sell alerts with confluence scoring");
        System.out.println("  4. Recommend options trades (calls/puts with strike prices)");
        System.out.println("  5. Backtest 9 equity strategies and 6 options strategies");
        System.out.println("  6. Compare all strategy performance");
    }
}
