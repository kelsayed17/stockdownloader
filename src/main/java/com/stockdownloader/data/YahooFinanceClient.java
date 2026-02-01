package com.stockdownloader.data;

import com.stockdownloader.model.QuoteData;
import com.stockdownloader.util.RetryExecutor;

import com.stockdownloader.util.CsvParser;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Downloads real-time stock quote data from Yahoo Finance CSV API
 * and returns a populated QuoteData model.
 */
public final class YahooFinanceClient {

    private static final Logger LOGGER = Logger.getLogger(YahooFinanceClient.class.getName());
    private static final int MAX_RETRIES = 3;

    public QuoteData download(String ticker) {
        var data = new QuoteData();

        String tags = "h0g0v0o0d1d2m3m4k2p0p5d0e0e8l1k0j0w0s6j1j2";
        String url = "http://download.finance.yahoo.com/d/quotes.csv?s=" + ticker + "&f=" + tags + "&e=.csv";

        RetryExecutor.execute(() -> {
            try (InputStream input = URI.create(url).toURL().openStream()) {
                parseQuote(input, tags, data);
            } catch (ArrayIndexOutOfBoundsException e) {
                LOGGER.log(Level.WARNING, "{0} has incomplete Yahoo finance data.", ticker);
                data.setIncomplete(true);
            }
        }, MAX_RETRIES, LOGGER, "Yahoo Finance download for " + ticker);

        return data;
    }

    private void parseQuote(InputStream input, String tags, QuoteData data) throws IOException {
        try (var parser = new CsvParser(input)) {
            String[] nextLine;
            while ((nextLine = parser.readNext()) != null) {
                data.setPriceSales(parseBigDecimal(nextLine[tags.indexOf("p5") / 2]));
                data.setTrailingAnnualDividendYield(parseBigDecimal(nextLine[tags.indexOf("d0") / 2]));
                data.setDilutedEPS(parseBigDecimal(nextLine[tags.indexOf("e0") / 2]));
                data.setEpsEstimateNextYear(parseBigDecimal(nextLine[tags.indexOf("e8") / 2]));
                data.setLastTradePriceOnly(parseBigDecimal(nextLine[tags.indexOf("l1") / 2]));
                data.setYearHigh(parseBigDecimal(nextLine[tags.indexOf("k0") / 2]));
                data.setYearLow(parseBigDecimal(nextLine[tags.indexOf("j0") / 2]));
                data.setFiftyDayMovingAverage(parseBigDecimal(nextLine[tags.indexOf("m3") / 2]));
                data.setTwoHundredDayMovingAverage(parseBigDecimal(nextLine[tags.indexOf("m4") / 2]));
                data.setPreviousClose(parseBigDecimal(nextLine[tags.indexOf("p0") / 2]));
                data.setOpen(parseBigDecimal(nextLine[tags.indexOf("o0") / 2]));
                data.setDaysHigh(parseBigDecimal(nextLine[tags.indexOf("h0") / 2]));
                data.setDaysLow(parseBigDecimal(nextLine[tags.indexOf("g0") / 2]));
                data.setVolume(parseBigDecimal(nextLine[tags.indexOf("v0") / 2]));
                data.setYearRange(nextLine[tags.indexOf("w0") / 2]);

                String marketCapStr = nextLine[tags.indexOf("j1") / 2];
                data.setMarketCapitalizationStr(marketCapStr);
                data.setMarketCapitalization(parseMarketCap(marketCapStr));

                if (data.getLastTradePriceOnly().compareTo(data.getYearLow()) < 0) {
                    data.setYearLow(data.getLastTradePriceOnly());
                }
            }
        }
    }

    private static BigDecimal parseBigDecimal(String value) {
        return "N/A".equals(value) ? BigDecimal.ZERO : new BigDecimal(value);
    }

    private static long parseMarketCap(String value) {
        if (value.contains("M")) {
            return (long) (Double.parseDouble(value.replace("M", "")) * 1_000_000);
        } else if (value.contains("B")) {
            return (long) (Double.parseDouble(value.replace("B", "")) * 1_000_000_000);
        }
        return 0;
    }
}
