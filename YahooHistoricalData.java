import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.HashMultimap;
import com.opencsv.CSVReader;

public class YahooHistoricalData implements Callable<YahooHistoricalData> {

    private static final Logger LOGGER = Logger.getLogger(YahooHistoricalData.class.getName());
    private static final int MAX_RETRIES = 3;
    private static final int PATTERN_DAYS = 7;

    private final String ticker;
    private final Dates dates;

    private BigDecimal highestPriceThisQtr = BigDecimal.ZERO;
    private BigDecimal lowestPriceThisQtr = BigDecimal.ZERO;
    private BigDecimal highestPriceLastQtr = BigDecimal.ZERO;
    private BigDecimal lowestPriceLastQtr = BigDecimal.ZERO;

    private String highestCloseDateThisQtrStr = "";
    private String lowestCloseDateThisQtrStr = "";
    private String highestCloseDateLastQtrStr = "";
    private String lowestCloseDateLastQtrStr = "";

    private List<String> historicalPrices = new ArrayList<>();
    private HashMultimap<String, String> patterns = HashMultimap.create();

    private boolean incomplete;
    private boolean error;

    public YahooHistoricalData(String ticker, Dates dates) {
        this.ticker = ticker;
        this.dates = dates;
    }

    public void downloadYahooHistorical(String ticker) {
        downloadYahooHistorical(ticker, 0);
    }

    private void downloadYahooHistorical(String ticker, int retryCount) {
        String url = "http://www.google.com/finance/historical?q=" + ticker + "&output=csv";

        try (InputStream input = URI.create(url).toURL().openStream()) {
            saveData(input);
        } catch (IOException e) {
            if (retryCount < MAX_RETRIES) {
                LOGGER.log(Level.FINE, "Retrying download for {0}, attempt {1}",
                        new Object[]{ticker, retryCount + 1});
                downloadYahooHistorical(ticker, retryCount + 1);
            } else {
                LOGGER.log(Level.WARNING, "Failed to download historical data for {0}", ticker);
                incomplete = true;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error processing historical data for {0}", ticker);
            incomplete = true;
        }
    }

    private void saveData(InputStream input) throws IOException {
        var mc = new MathContext(2);

        try (var reader = new CSVReader(new InputStreamReader(input))) {
            List<Integer> upDownList = new ArrayList<>();
            BigDecimal previousClosePrice = BigDecimal.ZERO;

            // Skip header
            reader.readNext();

            int i = 0;
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                BigDecimal closePrice = new BigDecimal(nextLine[4]);

                if (i > 0) {
                    BigDecimal closeChange = closePrice.subtract(previousClosePrice)
                            .divide(previousClosePrice, 10, RoundingMode.CEILING)
                            .multiply(new BigDecimal(100), mc);

                    int direction = closeChange.signum();
                    upDownList.add(direction);

                    String pattern = upDownList.toString();
                    patterns.put(pattern, ticker);
                }

                previousClosePrice = closePrice;

                if (i == PATTERN_DAYS) break;
                i++;
            }
        }
    }

    // Getters and setters
    public BigDecimal getHighestPriceThisQtr() { return highestPriceThisQtr; }
    public void setHighestPriceThisQtr(BigDecimal highestPriceThisQtr) { this.highestPriceThisQtr = highestPriceThisQtr; }

    public BigDecimal getLowestPriceThisQtr() { return lowestPriceThisQtr; }
    public void setLowestPriceThisQtr(BigDecimal lowestPriceThisQtr) { this.lowestPriceThisQtr = lowestPriceThisQtr; }

    public BigDecimal getHighestPriceLastQtr() { return highestPriceLastQtr; }
    public void setHighestPriceLastQtr(BigDecimal highestPriceLastQtr) { this.highestPriceLastQtr = highestPriceLastQtr; }

    public BigDecimal getLowestPriceLastQtr() { return lowestPriceLastQtr; }
    public void setLowestPriceLastQtr(BigDecimal lowestPriceLastQtr) { this.lowestPriceLastQtr = lowestPriceLastQtr; }

    public String getHighestCloseDateThisQtrStr() { return highestCloseDateThisQtrStr; }
    public void setHighestCloseDateThisQtrStr(String s) { this.highestCloseDateThisQtrStr = s; }

    public String getLowestCloseDateThisQtrStr() { return lowestCloseDateThisQtrStr; }
    public void setLowestCloseDateThisQtrStr(String s) { this.lowestCloseDateThisQtrStr = s; }

    public String getHighestCloseDateLastQtrStr() { return highestCloseDateLastQtrStr; }
    public void setHighestCloseDateLastQtrStr(String s) { this.highestCloseDateLastQtrStr = s; }

    public String getLowestCloseDateLastQtrStr() { return lowestCloseDateLastQtrStr; }
    public void setLowestCloseDateLastQtrStr(String s) { this.lowestCloseDateLastQtrStr = s; }

    public List<String> getHistorcalPrices() { return List.copyOf(historicalPrices); }
    public void setHistorcalPrices(ArrayList<String> historicalPrices) { this.historicalPrices = historicalPrices; }

    public HashMultimap<String, String> getPatterns() { return patterns; }
    public void setPatterns(HashMultimap<String, String> patterns) { this.patterns = patterns; }

    public boolean isIncomplete() { return incomplete; }
    public void setIncomplete(boolean incomplete) { this.incomplete = incomplete; }

    public boolean isError() { return error; }
    public void setError(boolean error) { this.error = error; }

    @Override
    public YahooHistoricalData call() throws Exception {
        downloadYahooHistorical(ticker);
        return this;
    }
}
