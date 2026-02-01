import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.opencsv.CSVReader;

public class SPYBacktest {

    private static final BigDecimal INITIAL_CAPITAL = new BigDecimal("100000.00");
    private static final BigDecimal COMMISSION = new BigDecimal("0.00"); // Most brokers are commission-free now

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("  SPY Trading Strategy Backtester");
        System.out.println("========================================");
        System.out.println();

        // Load SPY historical data
        List<PriceData> data;
        if (args.length > 0) {
            System.out.println("Loading SPY data from file: " + args[0]);
            data = loadFromCSV(args[0]);
        } else {
            System.out.println("Loading SPY data from Yahoo Finance...");
            data = downloadSPYData();
        }

        if (data.isEmpty()) {
            System.out.println("ERROR: No price data loaded. Provide a CSV file path as argument:");
            System.out.println("  java SPYBacktest spy_data.csv");
            System.out.println();
            System.out.println("CSV format: Date,Open,High,Low,Close,Adj Close,Volume");
            System.out.println("  (Yahoo Finance historical data format)");
            return;
        }

        System.out.println("Loaded " + data.size() + " trading days");
        System.out.println("Date range: " + data.get(0).getDate() + " to " + data.get(data.size() - 1).getDate());
        System.out.println("Starting capital: $" + INITIAL_CAPITAL.setScale(2, RoundingMode.HALF_UP));
        System.out.println();

        // Define strategies
        List<TradingStrategy> strategies = new ArrayList<>();
        strategies.add(new SMACrossoverStrategy(50, 200));
        strategies.add(new SMACrossoverStrategy(20, 50));
        strategies.add(new RSIStrategy(14, 30, 70));
        strategies.add(new RSIStrategy(14, 25, 75));
        strategies.add(new MACDStrategy(12, 26, 9));

        // Run backtests
        BacktestEngine engine = new BacktestEngine(INITIAL_CAPITAL, COMMISSION);
        List<BacktestResult> results = new ArrayList<>();

        for (TradingStrategy strategy : strategies) {
            System.out.println("Running backtest: " + strategy.getName() + "...");
            BacktestResult result = engine.run(strategy, data);
            results.add(result);
            result.printReport(data);
        }

        // Print comparison summary
        printComparison(results, data);
    }

    private static List<PriceData> loadFromCSV(String filename) {
        List<PriceData> data = new ArrayList<>();

        try {
            CSVReader reader = new CSVReader(new FileReader(filename));

            // Skip header
            String[] header = reader.readNext();

            String[] line;
            while ((line = reader.readNext()) != null) {
                try {
                    String date = line[0];
                    BigDecimal open = new BigDecimal(line[1]);
                    BigDecimal high = new BigDecimal(line[2]);
                    BigDecimal low = new BigDecimal(line[3]);
                    BigDecimal close = new BigDecimal(line[4]);
                    BigDecimal adjClose = line.length > 5 ? new BigDecimal(line[5]) : close;
                    long volume = line.length > 6 ? Long.parseLong(line[6]) : 0;

                    data.add(new PriceData(date, open, high, low, close, adjClose, volume));
                } catch (NumberFormatException e) {
                    // Skip lines with invalid data (e.g., "null" values)
                    continue;
                }
            }

            reader.close();
        } catch (Exception e) {
            System.out.println("Error loading CSV: " + e.getMessage());
        }

        return data;
    }

    private static List<PriceData> downloadSPYData() {
        List<PriceData> data = new ArrayList<>();

        try {
            // Attempt to download from Yahoo Finance via the existing GetYahooQuotes mechanism
            GetYahooQuotes quotes = new GetYahooQuotes();
            String crumb = quotes.getCrumb("SPY");

            if (crumb != null && !crumb.isEmpty()) {
                // Download 5 years of data
                long endDate = System.currentTimeMillis() / 1000;
                long startDate = endDate - (5L * 365 * 24 * 60 * 60); // ~5 years back

                String url = String.format(
                        "https://query1.finance.yahoo.com/v7/finance/download/SPY?period1=%d&period2=%d&interval=1d&events=history&crumb=%s",
                        startDate, endDate, crumb);

                InputStream input = new URL(url).openStream();
                CSVReader reader = new CSVReader(new InputStreamReader(input));

                // Skip header
                reader.readNext();

                String[] line;
                while ((line = reader.readNext()) != null) {
                    try {
                        String date = line[0];
                        BigDecimal open = new BigDecimal(line[1]);
                        BigDecimal high = new BigDecimal(line[2]);
                        BigDecimal low = new BigDecimal(line[3]);
                        BigDecimal close = new BigDecimal(line[4]);
                        BigDecimal adjClose = new BigDecimal(line[5]);
                        long volume = Long.parseLong(line[6]);

                        data.add(new PriceData(date, open, high, low, close, adjClose, volume));
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }

                reader.close();
                System.out.println("Downloaded " + data.size() + " days of SPY data from Yahoo Finance");
            } else {
                System.out.println("Could not obtain Yahoo Finance authentication crumb.");
                System.out.println("Please provide a CSV file instead.");
            }
        } catch (Exception e) {
            System.out.println("Could not download SPY data: " + e.getMessage());
            System.out.println("Please provide a CSV file as argument: java SPYBacktest spy_data.csv");
        }

        return data;
    }

    private static void printComparison(List<BacktestResult> results, List<PriceData> data) {
        String separator = "=".repeat(90);
        String thinSep = "-".repeat(90);

        System.out.println();
        System.out.println(separator);
        System.out.println("  STRATEGY COMPARISON SUMMARY");
        System.out.println(separator);
        System.out.println();

        // Buy and hold benchmark
        BigDecimal buyAndHold = BigDecimal.ZERO;
        if (!data.isEmpty()) {
            BigDecimal first = data.get(0).getClose();
            BigDecimal last = data.get(data.size() - 1).getClose();
            buyAndHold = last.subtract(first).divide(first, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
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
                    r.getSharpeRatio(252),
                    r.getMaxDrawdown().setScale(2, RoundingMode.HALF_UP),
                    r.getTotalTrades(),
                    r.getWinRate().setScale(2, RoundingMode.HALF_UP));
        }

        System.out.println(thinSep);

        // Find best strategy
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
                    best.getSharpeRatio(252),
                    best.getMaxDrawdown().setScale(2, RoundingMode.HALF_UP));

            if (best.getTotalReturn().compareTo(buyAndHold) > 0) {
                System.out.println("  >> Outperformed Buy & Hold by " +
                        best.getTotalReturn().subtract(buyAndHold).setScale(2, RoundingMode.HALF_UP) + " percentage points");
            } else {
                System.out.println("  >> Underperformed Buy & Hold by " +
                        buyAndHold.subtract(best.getTotalReturn()).setScale(2, RoundingMode.HALF_UP) + " percentage points");
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
