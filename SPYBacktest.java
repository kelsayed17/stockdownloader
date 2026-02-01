import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.opencsv.CSVReader;

/**
 * Main entry point for the SPY trading strategy backtester.
 *
 * <p>Loads historical SPY data (from a local CSV file or Yahoo Finance),
 * runs multiple trading strategies through the {@link BacktestEngine},
 * and prints individual reports plus a side-by-side comparison table.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * java SPYBacktest                   # attempts Yahoo Finance download
 * java SPYBacktest spy_data.csv      # loads from local CSV file
 * }</pre>
 */
public final class SPYBacktest {

    private static final BigDecimal INITIAL_CAPITAL = new BigDecimal("100000.00");
    private static final BigDecimal COMMISSION = BigDecimal.ZERO;
    private static final int DISPLAY_SCALE = 2;

    /** All strategies to be backtested. Immutable list via {@code List.of}. */
    private static final List<TradingStrategy> STRATEGIES = List.of(
            new SMACrossoverStrategy(50, 200),
            new SMACrossoverStrategy(20, 50),
            new RSIStrategy(14, 30, 70),
            new RSIStrategy(14, 25, 75),
            new MACDStrategy(12, 26, 9)
    );

    private SPYBacktest() {
        throw new AssertionError("Utility class — do not instantiate");
    }

    public static void main(String[] args) {
        printBanner();

        List<PriceData> data = loadData(args);

        if (data.isEmpty()) {
            System.err.println("ERROR: No price data loaded. Provide a CSV file path as argument:");
            System.err.println("  java SPYBacktest spy_data.csv");
            System.err.println();
            System.err.println("CSV format: Date,Open,High,Low,Close,Adj Close,Volume");
            System.err.println("  (Yahoo Finance historical data format, dates as yyyy-MM-dd)");
            System.exit(1);
        }

        System.out.println("Loaded " + data.size() + " trading days");
        System.out.println("Date range: " + data.getFirst().date() + " to " + data.getLast().date());
        System.out.printf("Starting capital: $%s%n%n", INITIAL_CAPITAL.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP));

        BacktestEngine engine = new BacktestEngine.Builder()
                .initialCapital(INITIAL_CAPITAL)
                .commission(COMMISSION)
                .build();

        List<BacktestResult> results = new ArrayList<>();

        for (TradingStrategy strategy : STRATEGIES) {
            System.out.println("Running backtest: " + strategy.getName() + "...");
            BacktestResult result = engine.run(strategy, data);
            results.add(result);
            result.printReport(data);
        }

        printComparison(results, data);
    }

    // ---- Data Loading ----

    private static List<PriceData> loadData(String[] args) {
        if (args.length > 0) {
            System.out.println("Loading SPY data from file: " + args[0]);
            return loadFromCSV(Path.of(args[0]));
        }

        System.out.println("Loading SPY data from Yahoo Finance...");
        List<PriceData> data = downloadSPYData();
        if (data.isEmpty()) {
            System.out.println("Download failed. Checking for local SPY.csv fallback...");
            Path fallback = Path.of("SPY.csv");
            if (Files.exists(fallback)) {
                return loadFromCSV(fallback);
            }
        }
        return data;
    }

    /**
     * Loads price data from a local CSV file using try-with-resources.
     * Skips rows with unparseable data (e.g., "null" values from Yahoo).
     */
    private static List<PriceData> loadFromCSV(Path path) {
        List<PriceData> data = new ArrayList<>();

        try (CSVReader reader = new CSVReader(Files.newBufferedReader(path))) {
            reader.readNext(); // skip header

            String[] line;
            while ((line = reader.readNext()) != null) {
                try {
                    data.add(PriceData.fromCsvRow(line));
                } catch (NumberFormatException | java.time.format.DateTimeParseException e) {
                    // Skip rows with invalid data (Yahoo sometimes has "null" values)
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error parsing CSV: " + e.getMessage());
        }

        return List.copyOf(data);
    }

    /**
     * Attempts to download ~5 years of SPY data from Yahoo Finance.
     * Uses the project's existing {@link GetYahooQuotes} for authentication.
     */
    private static List<PriceData> downloadSPYData() {
        List<PriceData> data = new ArrayList<>();

        try {
            var quotes = new GetYahooQuotes();
            String crumb = quotes.getCrumb("SPY");

            if (crumb == null || crumb.isEmpty()) {
                System.out.println("Could not obtain Yahoo Finance authentication crumb.");
                return List.of();
            }

            long endEpoch = System.currentTimeMillis() / 1000;
            long startEpoch = endEpoch - (5L * 365 * 24 * 60 * 60);

            String url = "https://query1.finance.yahoo.com/v7/finance/download/SPY?period1=%d&period2=%d&interval=1d&events=history&crumb=%s"
                    .formatted(startEpoch, endEpoch, crumb);

            try (InputStream input = new URL(url).openStream();
                 CSVReader reader = new CSVReader(new InputStreamReader(input))) {

                reader.readNext(); // skip header

                String[] line;
                while ((line = reader.readNext()) != null) {
                    try {
                        data.add(PriceData.fromCsvRow(line));
                    } catch (NumberFormatException | java.time.format.DateTimeParseException e) {
                        // skip invalid rows
                    }
                }
            }

            System.out.println("Downloaded " + data.size() + " days of SPY data from Yahoo Finance");
        } catch (Exception e) {
            System.out.println("Could not download SPY data: " + e.getMessage());
        }

        return List.copyOf(data);
    }

    // ---- Reporting ----

    private static void printBanner() {
        System.out.println("========================================");
        System.out.println("  SPY Trading Strategy Backtester");
        System.out.println("========================================");
        System.out.println();
    }

    private static void printComparison(List<BacktestResult> results, List<PriceData> data) {
        String separator = "=".repeat(90);
        String thinSep = "-".repeat(90);

        System.out.println();
        System.out.println(separator);
        System.out.println("  STRATEGY COMPARISON SUMMARY");
        System.out.println(separator);
        System.out.println();

        BigDecimal buyAndHold = BacktestResult.buyAndHoldReturn(data);

        System.out.printf("  %-35s %10s %10s %10s %10s %10s%n",
                "Strategy", "Return", "Sharpe", "MaxDD", "Trades", "Win Rate");
        System.out.println(thinSep);
        System.out.printf("  %-35s %9s%% %10s %9s%% %10s %10s%n",
                "Buy & Hold (Benchmark)",
                buyAndHold.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP),
                "N/A", "N/A", "1", "N/A");

        for (BacktestResult r : results) {
            System.out.printf("  %-35s %9s%% %10s %9s%% %10d %9s%%%n",
                    r.getStrategyName(),
                    r.getTotalReturn().setScale(DISPLAY_SCALE, RoundingMode.HALF_UP),
                    r.getSharpeRatio(TechnicalIndicators.TRADING_DAYS_PER_YEAR),
                    r.getMaxDrawdown().setScale(DISPLAY_SCALE, RoundingMode.HALF_UP),
                    r.getTotalTrades(),
                    r.getWinRate().setScale(DISPLAY_SCALE, RoundingMode.HALF_UP));
        }

        System.out.println(thinSep);

        results.stream()
                .max(Comparator.comparing(BacktestResult::getTotalReturn))
                .ifPresent(best -> {
                    System.out.println();
                    System.out.println("  Best performing strategy: " + best.getStrategyName());
                    System.out.printf("  Return: %s%% | Sharpe: %s | Max Drawdown: %s%%%n",
                            best.getTotalReturn().setScale(DISPLAY_SCALE, RoundingMode.HALF_UP),
                            best.getSharpeRatio(TechnicalIndicators.TRADING_DAYS_PER_YEAR),
                            best.getMaxDrawdown().setScale(DISPLAY_SCALE, RoundingMode.HALF_UP));

                    BigDecimal diff = best.getTotalReturn().subtract(buyAndHold);
                    if (diff.signum() > 0) {
                        System.out.println("  >> Outperformed Buy & Hold by " +
                                diff.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP) + " percentage points");
                    } else {
                        System.out.println("  >> Underperformed Buy & Hold by " +
                                diff.abs().setScale(DISPLAY_SCALE, RoundingMode.HALF_UP) + " percentage points");
                    }
                });

        System.out.println();
        System.out.println(separator);
        System.out.println();
        System.out.println("  DISCLAIMER: This is for educational purposes only.");
        System.out.println("  Past performance does not guarantee future results.");
        System.out.println("  Always do your own research before trading.");
        System.out.println();
    }
}
