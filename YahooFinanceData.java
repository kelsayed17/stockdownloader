import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.opencsv.CSVReader;

public class YahooFinanceData {

    private static final Logger LOGGER = Logger.getLogger(YahooFinanceData.class.getName());
    private static final int MAX_RETRIES = 3;

    private BigDecimal priceSales = BigDecimal.ZERO;
    private BigDecimal trailingAnnualDividendYield = BigDecimal.ZERO;
    private BigDecimal dilutedEPS = BigDecimal.ZERO;
    private BigDecimal epsEstimateNextYear = BigDecimal.ZERO;
    private BigDecimal lastTradePriceOnly = BigDecimal.ZERO;
    private BigDecimal yearHigh = BigDecimal.ZERO;
    private BigDecimal yearLow = BigDecimal.ZERO;
    private BigDecimal fiftyDayMovingAverage = BigDecimal.ZERO;
    private BigDecimal twoHundredDayMovingAverage = BigDecimal.ZERO;
    private BigDecimal previousClose = BigDecimal.ZERO;
    private BigDecimal open = BigDecimal.ZERO;
    private BigDecimal daysHigh = BigDecimal.ZERO;
    private BigDecimal daysLow = BigDecimal.ZERO;
    private BigDecimal volume = BigDecimal.ZERO;

    private String yearRange = "";
    private String marketCapitalizationStr = "";
    private long marketCapitalization;

    private boolean incomplete;
    private boolean error;

    public void downloadYahooFinance(String ticker) {
        downloadYahooFinance(ticker, 0);
    }

    private void downloadYahooFinance(String ticker, int retryCount) {
        String tags = "h0g0v0o0d1d2m3m4k2p0p5d0e0e8l1k0j0w0s6j1j2";
        String url = "http://download.finance.yahoo.com/d/quotes.csv?s=" + ticker + "&f=" + tags + "&e=.csv";

        try (InputStream input = URI.create(url).toURL().openStream()) {
            saveData(input, tags);
        } catch (ArrayIndexOutOfBoundsException a) {
            LOGGER.log(Level.WARNING, "{0} has incomplete data after processing Yahoo finance data.", ticker);
            incomplete = true;
        } catch (IOException e) {
            if (retryCount < MAX_RETRIES) {
                LOGGER.log(Level.FINE, "Retrying download for {0}, attempt {1}",
                        new Object[]{ticker, retryCount + 1});
                downloadYahooFinance(ticker, retryCount + 1);
            } else {
                LOGGER.log(Level.WARNING, "Failed to download Yahoo finance data for {0} after {1} retries",
                        new Object[]{ticker, MAX_RETRIES});
                error = true;
            }
        }
    }

    private void saveData(InputStream input, String tags) throws IOException {
        try (var reader = new CSVReader(new InputStreamReader(input))) {
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                priceSales = parseBigDecimal(nextLine[tags.indexOf("p5") / 2]);
                trailingAnnualDividendYield = parseBigDecimal(nextLine[tags.indexOf("d0") / 2]);
                dilutedEPS = parseBigDecimal(nextLine[tags.indexOf("e0") / 2]);
                epsEstimateNextYear = parseBigDecimal(nextLine[tags.indexOf("e8") / 2]);
                lastTradePriceOnly = parseBigDecimal(nextLine[tags.indexOf("l1") / 2]);
                yearHigh = parseBigDecimal(nextLine[tags.indexOf("k0") / 2]);
                yearLow = parseBigDecimal(nextLine[tags.indexOf("j0") / 2]);
                fiftyDayMovingAverage = parseBigDecimal(nextLine[tags.indexOf("m3") / 2]);
                twoHundredDayMovingAverage = parseBigDecimal(nextLine[tags.indexOf("m4") / 2]);
                previousClose = parseBigDecimal(nextLine[tags.indexOf("p0") / 2]);
                open = parseBigDecimal(nextLine[tags.indexOf("o0") / 2]);
                daysHigh = parseBigDecimal(nextLine[tags.indexOf("h0") / 2]);
                daysLow = parseBigDecimal(nextLine[tags.indexOf("g0") / 2]);
                volume = parseBigDecimal(nextLine[tags.indexOf("v0") / 2]);
                yearRange = nextLine[tags.indexOf("w0") / 2];
                marketCapitalizationStr = nextLine[tags.indexOf("j1") / 2];

                if (marketCapitalizationStr.contains("M")) {
                    marketCapitalization = (long) (Double.parseDouble(marketCapitalizationStr.replace("M", "")) * 1_000_000);
                } else if (marketCapitalizationStr.contains("B")) {
                    marketCapitalization = (long) (Double.parseDouble(marketCapitalizationStr.replace("B", "")) * 1_000_000_000);
                }

                if (lastTradePriceOnly.compareTo(yearLow) < 0) {
                    yearLow = lastTradePriceOnly;
                }
            }
        }
    }

    private static BigDecimal parseBigDecimal(String value) {
        return "N/A".equals(value) ? BigDecimal.ZERO : new BigDecimal(value);
    }

    // Getters and setters
    public BigDecimal getPriceSales() { return priceSales; }
    public void setPriceSales(BigDecimal priceSales) { this.priceSales = priceSales; }

    public BigDecimal getTrailingAnnualDividendYield() { return trailingAnnualDividendYield; }
    public void setTrailingAnnualDividendYield(BigDecimal trailingAnnualDividendYield) { this.trailingAnnualDividendYield = trailingAnnualDividendYield; }

    public BigDecimal getDilutedEPS() { return dilutedEPS; }
    public void setDilutedEPS(BigDecimal dilutedEPS) { this.dilutedEPS = dilutedEPS; }

    public BigDecimal getEPSEstimateNextYear() { return epsEstimateNextYear; }
    public void setEPSEstimateNextYear(BigDecimal epsEstimateNextYear) { this.epsEstimateNextYear = epsEstimateNextYear; }

    public BigDecimal getLastTradePriceOnly() { return lastTradePriceOnly; }
    public void setLastTradePriceOnly(BigDecimal lastTradePriceOnly) { this.lastTradePriceOnly = lastTradePriceOnly; }

    public BigDecimal getYearHigh() { return yearHigh; }
    public void setYearHigh(BigDecimal yearHigh) { this.yearHigh = yearHigh; }

    public BigDecimal getYearLow() { return yearLow; }
    public void setYearLow(BigDecimal yearLow) { this.yearLow = yearLow; }

    public BigDecimal getFiftydayMovingAverage() { return fiftyDayMovingAverage; }
    public void setFiftydayMovingAverage(BigDecimal fiftyDayMovingAverage) { this.fiftyDayMovingAverage = fiftyDayMovingAverage; }

    public BigDecimal getTwoHundreddayMovingAverage() { return twoHundredDayMovingAverage; }
    public void setTwoHundreddayMovingAverage(BigDecimal twoHundredDayMovingAverage) { this.twoHundredDayMovingAverage = twoHundredDayMovingAverage; }

    public BigDecimal getPreviousCloseOne() { return previousClose; }
    public void setPreviousCloseOne(BigDecimal previousClose) { this.previousClose = previousClose; }

    public BigDecimal getOpen() { return open; }
    public void setOpen(BigDecimal open) { this.open = open; }

    public BigDecimal getDaysHigh() { return daysHigh; }
    public void setDaysHigh(BigDecimal daysHigh) { this.daysHigh = daysHigh; }

    public BigDecimal getDaysLow() { return daysLow; }
    public void setDaysLow(BigDecimal daysLow) { this.daysLow = daysLow; }

    public BigDecimal getVolume() { return volume; }
    public void setVolume(BigDecimal volume) { this.volume = volume; }

    public String getYearRange() { return yearRange; }
    public void setYearRange(String yearRange) { this.yearRange = yearRange; }

    public String getMarketCapitalizationStr() { return marketCapitalizationStr; }
    public void setMarketCapitalizationStr(String marketCapitalizationStr) { this.marketCapitalizationStr = marketCapitalizationStr; }

    public long getMarketCapitalization() { return marketCapitalization; }
    public void setMarketCapitalization(long marketCapitalization) { this.marketCapitalization = marketCapitalization; }

    public boolean isIncomplete() { return incomplete; }
    public void setIncomplete(boolean incomplete) { this.incomplete = incomplete; }

    public boolean isError() { return error; }
    public void setError(boolean error) { this.error = error; }
}
