package com.stockdownloader.data;

import com.stockdownloader.model.PriceData;
import com.stockdownloader.util.RetryExecutor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fetches historical OHLCV price data directly from Yahoo Finance v8 chart API
 * for any ticker symbol. Returns data as List&lt;PriceData&gt; in memory without
 * intermediate CSV files.
 *
 * Supports configurable time ranges: 1mo, 3mo, 6mo, 1y, 2y, 5y, 10y, max
 * and intervals: 1d, 1wk, 1mo
 */
public final class YahooDataClient {

    private static final Logger LOGGER = Logger.getLogger(YahooDataClient.class.getName());
    private static final int MAX_RETRIES = 3;
    private static final String CHART_URL =
            "https://query1.finance.yahoo.com/v8/finance/chart/%s?range=%s&interval=%s";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final YahooAuthHelper auth;

    public YahooDataClient() {
        this(new YahooAuthHelper());
    }

    public YahooDataClient(YahooAuthHelper auth) {
        this.auth = auth;
    }

    /**
     * Fetch historical price data for a symbol with default 5-year range and daily interval.
     */
    public List<PriceData> fetchPriceData(String symbol) {
        return fetchPriceData(symbol, "5y", "1d");
    }

    /**
     * Fetch historical price data for a symbol with configurable range and interval.
     *
     * @param symbol   ticker symbol (e.g., "AAPL", "SPY", "TSLA")
     * @param range    time range: 1mo, 3mo, 6mo, 1y, 2y, 5y, 10y, max
     * @param interval data interval: 1d, 1wk, 1mo
     * @return list of PriceData sorted by date ascending, empty if fetch fails
     */
    public List<PriceData> fetchPriceData(String symbol, String range, String interval) {
        if (auth.getCrumb() == null) {
            auth.authenticate();
        }

        List<PriceData> result = new ArrayList<>();

        RetryExecutor.execute(() -> {
            String url = (CHART_URL + "&crumb=%s").formatted(
                    symbol.toUpperCase(), range, interval, auth.getCrumb());

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
                result.addAll(parseChartResponse(sb.toString(), symbol));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }, MAX_RETRIES, LOGGER, "price data fetch for " + symbol);

        return result;
    }

    /**
     * Fetch price data using explicit epoch timestamps for precise date ranges.
     */
    public List<PriceData> fetchPriceData(String symbol, long startEpoch, long endEpoch) {
        if (auth.getCrumb() == null) {
            auth.authenticate();
        }

        List<PriceData> result = new ArrayList<>();
        String periodUrl = "https://query1.finance.yahoo.com/v8/finance/chart/%s?period1=%d&period2=%d&interval=1d";

        RetryExecutor.execute(() -> {
            String url = (periodUrl + "&crumb=%s").formatted(
                    symbol.toUpperCase(), startEpoch, endEpoch, auth.getCrumb());

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
                result.addAll(parseChartResponse(sb.toString(), symbol));
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }, MAX_RETRIES, LOGGER, "price data fetch for " + symbol);

        return result;
    }

    private List<PriceData> parseChartResponse(String json, String symbol) {
        List<PriceData> data = new ArrayList<>();

        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject chart = root.getAsJsonObject("chart");
            if (chart == null) {
                LOGGER.warning("No chart data in response for " + symbol);
                return data;
            }

            JsonArray results = chart.getAsJsonArray("result");
            if (results == null || results.isEmpty()) {
                LOGGER.warning("Empty results for " + symbol);
                return data;
            }

            JsonObject result = results.get(0).getAsJsonObject();
            JsonArray timestamps = result.getAsJsonArray("timestamp");
            if (timestamps == null || timestamps.isEmpty()) {
                LOGGER.warning("No timestamps for " + symbol);
                return data;
            }

            JsonObject indicators = result.getAsJsonObject("indicators");
            JsonArray quoteArray = indicators.getAsJsonArray("quote");
            if (quoteArray == null || quoteArray.isEmpty()) {
                return data;
            }

            JsonObject quote = quoteArray.get(0).getAsJsonObject();
            JsonArray openArray = quote.getAsJsonArray("open");
            JsonArray highArray = quote.getAsJsonArray("high");
            JsonArray lowArray = quote.getAsJsonArray("low");
            JsonArray closeArray = quote.getAsJsonArray("close");
            JsonArray volumeArray = quote.getAsJsonArray("volume");

            // Check for adjusted close
            JsonArray adjCloseArray = null;
            JsonArray adjCloseWrapper = indicators.getAsJsonArray("adjclose");
            if (adjCloseWrapper != null && !adjCloseWrapper.isEmpty()) {
                adjCloseArray = adjCloseWrapper.get(0).getAsJsonObject().getAsJsonArray("adjclose");
            }

            for (int i = 0; i < timestamps.size(); i++) {
                try {
                    JsonElement tsEl = timestamps.get(i);
                    if (tsEl == null || tsEl.isJsonNull()) continue;

                    long epoch = tsEl.getAsLong();
                    String date = Instant.ofEpochSecond(epoch)
                            .atZone(ZoneId.of("America/New_York"))
                            .toLocalDate()
                            .format(DATE_FMT);

                    BigDecimal open = getDecimalAt(openArray, i);
                    BigDecimal high = getDecimalAt(highArray, i);
                    BigDecimal low = getDecimalAt(lowArray, i);
                    BigDecimal close = getDecimalAt(closeArray, i);
                    long volume = getLongAt(volumeArray, i);

                    BigDecimal adjClose = adjCloseArray != null
                            ? getDecimalAt(adjCloseArray, i) : close;

                    // Skip bars with null/zero close prices
                    if (close.compareTo(BigDecimal.ZERO) == 0) continue;

                    data.add(new PriceData(date, open, high, low, close, adjClose, volume));
                } catch (Exception e) {
                    // Skip malformed bars
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error parsing chart data for {0}: {1}",
                    new Object[]{symbol, e.getMessage()});
        }

        return data;
    }

    private static BigDecimal getDecimalAt(JsonArray arr, int index) {
        if (arr == null || index >= arr.size()) return BigDecimal.ZERO;
        JsonElement el = arr.get(index);
        if (el == null || el.isJsonNull()) return BigDecimal.ZERO;
        return el.getAsBigDecimal();
    }

    private static long getLongAt(JsonArray arr, int index) {
        if (arr == null || index >= arr.size()) return 0;
        JsonElement el = arr.get(index);
        if (el == null || el.isJsonNull()) return 0;
        return el.getAsLong();
    }

    public YahooAuthHelper getAuth() {
        return auth;
    }
}
