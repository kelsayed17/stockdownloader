// Java

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;

// opencsv
import com.opencsv.CSVReader;

public class YahooFinanceData {
    private BigDecimal priceSales;
    private BigDecimal trailingAnnualDividendYield;
    private BigDecimal dilutedEPS;
    private BigDecimal EPSEstimateNextYear;
    private BigDecimal lastTradePriceOnly;
    private BigDecimal yearHigh;
    private BigDecimal yearLow;
    private BigDecimal fiftydayMovingAverage;
    private BigDecimal twoHundreddayMovingAverage;
    private BigDecimal previousCloseOne;
    private BigDecimal open;
    private BigDecimal daysHigh;
    private BigDecimal daysLow;
    private BigDecimal volume;

    private String yearRange;
    private String marketCapitalizationStr;

    private long marketCapitalization;

    private boolean incomplete;
    private boolean error;

    public YahooFinanceData() {
        priceSales = new BigDecimal("0.00");
        trailingAnnualDividendYield = new BigDecimal("0.00");
        dilutedEPS = new BigDecimal("0.00");
        EPSEstimateNextYear = new BigDecimal("0.00");
        lastTradePriceOnly = new BigDecimal("0.00");
        yearHigh = new BigDecimal("0.00");
        yearLow = new BigDecimal("0.00");
        fiftydayMovingAverage = new BigDecimal("0.00");
        twoHundreddayMovingAverage = new BigDecimal("0.00");
        previousCloseOne = new BigDecimal("0.00");
        open = new BigDecimal("0.00");
        daysHigh = new BigDecimal("0.00");
        daysLow = new BigDecimal("0.00");
        volume = new BigDecimal("0.00");
        yearRange = "";
        marketCapitalizationStr = "";
        marketCapitalization = 0;
        incomplete = false;
        error = false;
    }

    public void downloadYahooFinance(String ticker) {
        String tags = "h0g0v0o0d1d2m3m4k2p0p5d0e0e8l1k0j0w0s6j1j2";
        String url = "http://download.finance.yahoo.com/d/quotes.csv?s=" + ticker + "&f=" + tags + "&e=.csv";

        try {
            InputStream input = new URL(url).openStream();
            saveData(input, tags);
        } catch (ArrayIndexOutOfBoundsException a) {
            System.out.println(ticker + " has incomplete data after processing Yahoo finance data.");
            incomplete = true;
        } catch (IOException e) {
            e.printStackTrace();
            downloadYahooFinance(ticker);
        }
    }

    private void saveData(InputStream input, String tags) throws IOException {
        CSVReader reader = new CSVReader(new InputStreamReader(input));

        String[] nextLine;
        while ((nextLine = reader.readNext()) != null) {
            String priceSalesStr = nextLine[tags.indexOf("p5") / 2];
            String trailingAnnualDividendYieldStr = nextLine[tags.indexOf("d0") / 2];
            String dilutedEPSStr = nextLine[tags.indexOf("e0") / 2];
            String EPSEstimateNextYearStr = nextLine[tags.indexOf("e8") / 2];
            String lastTradePriceOnlyStr = nextLine[tags.indexOf("l1") / 2];
            String yearHighStr = nextLine[tags.indexOf("k0") / 2];
            String yearLowStr = nextLine[tags.indexOf("j0") / 2];
            String fiftydayMovingAverageStr = nextLine[tags.indexOf("m3") / 2];
            String twoHundreddayMovingAverageStr = nextLine[tags.indexOf("m4") / 2];
            String previousCloseOneStr = nextLine[tags.indexOf("p0") / 2];
            String openStr = nextLine[tags.indexOf("o0") / 2];
            String daysHighStr = nextLine[tags.indexOf("h0") / 2];
            String daysLowStr = nextLine[tags.indexOf("g0") / 2];
            String volumeStr = nextLine[tags.indexOf("v0") / 2];
            yearRange = nextLine[tags.indexOf("w0") / 2];
            marketCapitalizationStr = nextLine[tags.indexOf("j1") / 2];

            priceSales = new BigDecimal((priceSalesStr.equals("N/A") ? "0" : priceSalesStr));
            trailingAnnualDividendYield = new BigDecimal((trailingAnnualDividendYieldStr.equals("N/A") ? "0" : trailingAnnualDividendYieldStr));
            dilutedEPS = new BigDecimal((dilutedEPSStr.equals("N/A") ? "0" : dilutedEPSStr));
            EPSEstimateNextYear = new BigDecimal((EPSEstimateNextYearStr.equals("N/A") ? "0" : EPSEstimateNextYearStr));
            lastTradePriceOnly = new BigDecimal((lastTradePriceOnlyStr.equals("N/A") ? "0" : lastTradePriceOnlyStr));
            yearHigh = new BigDecimal((yearHighStr.equals("N/A") ? "0" : yearHighStr));
            yearLow = new BigDecimal((yearLowStr.equals("N/A") ? "0" : yearLowStr));
            fiftydayMovingAverage = new BigDecimal((fiftydayMovingAverageStr.equals("N/A") ? "0" : fiftydayMovingAverageStr));
            twoHundreddayMovingAverage = new BigDecimal((twoHundreddayMovingAverageStr.equals("N/A") ? "0" : twoHundreddayMovingAverageStr));
            previousCloseOne = new BigDecimal((previousCloseOneStr.equals("N/A") ? "0" : previousCloseOneStr));
            open = new BigDecimal((openStr.equals("N/A") ? "0" : openStr));
            daysHigh = new BigDecimal((daysHighStr.equals("N/A") ? "0" : daysHighStr));
            daysLow = new BigDecimal((daysLowStr.equals("N/A") ? "0" : daysLowStr));
            volume = new BigDecimal((volumeStr.equals("N/A") ? "0" : volumeStr));

            if (marketCapitalizationStr.contains("M"))
                marketCapitalization = (long) (Double.parseDouble(marketCapitalizationStr.replaceAll("M", "")) * 1000000);
            else if (marketCapitalizationStr.contains("B"))
                marketCapitalization = (long) (Double.parseDouble(marketCapitalizationStr.replaceAll("B", "")) * 1000000000);

            if (lastTradePriceOnly.compareTo(yearLow) == -1)
                yearLow = lastTradePriceOnly;
        }
        reader.close();
    }

    public BigDecimal getPriceSales() {
        return priceSales;
    }

    public void setPriceSales(BigDecimal priceSales) {
        this.priceSales = priceSales;
    }

    public BigDecimal getTrailingAnnualDividendYield() {
        return trailingAnnualDividendYield;
    }

    public void setTrailingAnnualDividendYield(BigDecimal trailingAnnualDividendYield) {
        this.trailingAnnualDividendYield = trailingAnnualDividendYield;
    }

    public BigDecimal getDilutedEPS() {
        return dilutedEPS;
    }

    public void setDilutedEPS(BigDecimal dilutedEPS) {
        this.dilutedEPS = dilutedEPS;
    }

    public BigDecimal getEPSEstimateNextYear() {
        return EPSEstimateNextYear;
    }

    public void setEPSEstimateNextYear(BigDecimal EPSEstimateNextYear) {
        this.EPSEstimateNextYear = EPSEstimateNextYear;
    }

    public BigDecimal getLastTradePriceOnly() {
        return lastTradePriceOnly;
    }

    public void setLastTradePriceOnly(BigDecimal lastTradePriceOnly) {
        this.lastTradePriceOnly = lastTradePriceOnly;
    }

    public BigDecimal getYearHigh() {
        return yearHigh;
    }

    public void setYearHigh(BigDecimal yearHigh) {
        this.yearHigh = yearHigh;
    }

    public BigDecimal getYearLow() {
        return yearLow;
    }

    public void setYearLow(BigDecimal yearLow) {
        this.yearLow = yearLow;
    }

    public BigDecimal getFiftydayMovingAverage() {
        return fiftydayMovingAverage;
    }

    public void setFiftydayMovingAverage(BigDecimal fiftydayMovingAverage) {
        this.fiftydayMovingAverage = fiftydayMovingAverage;
    }

    public BigDecimal getTwoHundreddayMovingAverage() {
        return twoHundreddayMovingAverage;
    }

    public void setTwoHundreddayMovingAverage(BigDecimal twoHundreddayMovingAverage) {
        this.twoHundreddayMovingAverage = twoHundreddayMovingAverage;
    }

    public BigDecimal getPreviousCloseOne() {
        return previousCloseOne;
    }

    public void setPreviousCloseOne(BigDecimal previousCloseOne) {
        this.previousCloseOne = previousCloseOne;
    }

    public BigDecimal getOpen() {
        return open;
    }

    public void setOpen(BigDecimal open) {
        this.open = open;
    }

    public BigDecimal getDaysHigh() {
        return daysHigh;
    }

    public void setDaysHigh(BigDecimal daysHigh) {
        this.daysHigh = daysHigh;
    }

    public BigDecimal getDaysLow() {
        return daysLow;
    }

    public void setDaysLow(BigDecimal daysLow) {
        this.daysLow = daysLow;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public String getYearRange() {
        return yearRange;
    }

    public void setYearRange(String yearRange) {
        this.yearRange = yearRange;
    }

    public String getMarketCapitalizationStr() {
        return marketCapitalizationStr;
    }

    public void setMarketCapitalizationStr(String marketCapitalizationStr) {
        this.marketCapitalizationStr = marketCapitalizationStr;
    }

    public long getMarketCapitalization() {
        return marketCapitalization;
    }

    public void setMarketCapitalization(long marketCapitalization) {
        this.marketCapitalization = marketCapitalization;
    }

    public boolean isIncomplete() {
        return incomplete;
    }

    public void setIncomplete(boolean incomplete) {
        this.incomplete = incomplete;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }
}