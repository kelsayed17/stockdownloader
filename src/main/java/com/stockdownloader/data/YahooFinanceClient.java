package com.stockdownloader.data;

import com.stockdownloader.model.QuoteData;
import com.stockdownloader.util.RetryExecutor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Downloads real-time stock quote data from Yahoo Finance v7 quote JSON API
 * and returns a populated QuoteData model.
 *
 * Replaces the deprecated download.finance.yahoo.com/d/quotes.csv endpoint
 * which was shut down in 2017.
 */
public final class YahooFinanceClient {

    private static final Logger LOGGER = Logger.getLogger(YahooFinanceClient.class.getName());
    private static final int MAX_RETRIES = 3;
    private static final String QUOTE_URL = "https://query1.finance.yahoo.com/v7/finance/quote?symbols=%s";

    private final YahooAuthHelper auth;

    public YahooFinanceClient() {
        this(new YahooAuthHelper());
    }

    public YahooFinanceClient(YahooAuthHelper auth) {
        this.auth = auth;
    }

    public QuoteData download(String ticker) {
        var data = new QuoteData();

        if (auth.getCrumb() == null) {
            auth.authenticate();
        }

        RetryExecutor.execute(() -> {
            String url = (QUOTE_URL + "&crumb=%s").formatted(ticker, auth.getCrumb());

            var request = new HttpGet(url);
            request.addHeader("User-Agent", auth.getUserAgent());

            HttpResponse response = auth.getClient().execute(request, auth.getContext());
            try (var reader = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()))) {
                var sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                parseQuoteJson(sb.toString(), data);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }, MAX_RETRIES, LOGGER, "Yahoo Finance download for " + ticker);

        return data;
    }

    private void parseQuoteJson(String json, QuoteData data) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject quoteResponse = root.getAsJsonObject("quoteResponse");

            if (quoteResponse == null || quoteResponse.getAsJsonArray("result").isEmpty()) {
                LOGGER.log(Level.WARNING, "Empty quote response from Yahoo Finance");
                data.setIncomplete(true);
                return;
            }

            JsonObject quote = quoteResponse.getAsJsonArray("result").get(0).getAsJsonObject();

            data.setPriceSales(getDecimal(quote, "priceToSalesTrailing12Months"));
            data.setTrailingAnnualDividendYield(getDecimal(quote, "trailingAnnualDividendYield"));
            data.setDilutedEPS(getDecimal(quote, "epsTrailingTwelveMonths"));
            data.setEpsEstimateNextYear(getDecimal(quote, "epsForward"));
            data.setLastTradePriceOnly(getDecimal(quote, "regularMarketPrice"));
            data.setYearHigh(getDecimal(quote, "fiftyTwoWeekHigh"));
            data.setYearLow(getDecimal(quote, "fiftyTwoWeekLow"));
            data.setFiftyDayMovingAverage(getDecimal(quote, "fiftyDayAverage"));
            data.setTwoHundredDayMovingAverage(getDecimal(quote, "twoHundredDayAverage"));
            data.setPreviousClose(getDecimal(quote, "regularMarketPreviousClose"));
            data.setOpen(getDecimal(quote, "regularMarketOpen"));
            data.setDaysHigh(getDecimal(quote, "regularMarketDayHigh"));
            data.setDaysLow(getDecimal(quote, "regularMarketDayLow"));
            data.setVolume(getDecimal(quote, "regularMarketVolume"));

            String range = getString(quote, "fiftyTwoWeekRange");
            data.setYearRange(range);

            long marketCap = getLong(quote, "marketCap");
            data.setMarketCapitalization(marketCap);
            data.setMarketCapitalizationStr(formatMarketCap(marketCap));

            if (data.getLastTradePriceOnly().compareTo(data.getYearLow()) < 0) {
                data.setYearLow(data.getLastTradePriceOnly());
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error parsing Yahoo Finance quote JSON: {0}", e.getMessage());
            data.setIncomplete(true);
        }
    }

    private static BigDecimal getDecimal(JsonObject obj, String field) {
        JsonElement el = obj.get(field);
        if (el == null || el.isJsonNull()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(el.getAsString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static long getLong(JsonObject obj, String field) {
        JsonElement el = obj.get(field);
        if (el == null || el.isJsonNull()) return 0;
        try {
            return el.getAsLong();
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String getString(JsonObject obj, String field) {
        JsonElement el = obj.get(field);
        if (el == null || el.isJsonNull()) return "";
        return el.getAsString();
    }

    private static String formatMarketCap(long marketCap) {
        if (marketCap >= 1_000_000_000L) {
            return "%.2fB".formatted(marketCap / 1_000_000_000.0);
        } else if (marketCap >= 1_000_000L) {
            return "%.2fM".formatted(marketCap / 1_000_000.0);
        }
        return String.valueOf(marketCap);
    }
}
