package com.stockdownloader.data;

import com.stockdownloader.model.HistoricalData;
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
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Downloads historical price data from Yahoo Finance v8 chart API
 * and computes price movement patterns.
 *
 * Replaces the deprecated Google Finance historical CSV endpoint
 * (www.google.com/finance/historical) which was shut down around 2015.
 */
public final class YahooHistoricalClient {

    private static final Logger LOGGER = Logger.getLogger(YahooHistoricalClient.class.getName());
    private static final int MAX_RETRIES = 3;
    private static final int PATTERN_DAYS = 7;
    private static final String CHART_URL =
            "https://query1.finance.yahoo.com/v8/finance/chart/%s?range=1mo&interval=1d";

    private final YahooAuthHelper auth;

    public YahooHistoricalClient() {
        this(new YahooAuthHelper());
    }

    public YahooHistoricalClient(YahooAuthHelper auth) {
        this.auth = auth;
    }

    public HistoricalData download(String ticker) {
        var data = new HistoricalData(ticker);

        if (auth.getCrumb() == null) {
            auth.authenticate();
        }

        RetryExecutor.execute(() -> {
            String url = (CHART_URL + "&crumb=%s").formatted(ticker, auth.getCrumb());

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
                parseChartJson(sb.toString(), data);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }, MAX_RETRIES, LOGGER, "historical download for " + ticker);

        return data;
    }

    private void parseChartJson(String json, HistoricalData data) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject chart = root.getAsJsonObject("chart");

            if (chart == null) {
                data.setIncomplete(true);
                return;
            }

            JsonArray results = chart.getAsJsonArray("result");
            if (results == null || results.isEmpty()) {
                data.setIncomplete(true);
                return;
            }

            JsonObject result = results.get(0).getAsJsonObject();
            JsonObject indicators = result.getAsJsonObject("indicators");
            JsonArray quoteArray = indicators.getAsJsonArray("quote");

            if (quoteArray == null || quoteArray.isEmpty()) {
                data.setIncomplete(true);
                return;
            }

            JsonObject quote = quoteArray.get(0).getAsJsonObject();
            JsonArray closeArray = quote.getAsJsonArray("close");

            if (closeArray == null || closeArray.size() < 2) {
                data.setIncomplete(true);
                return;
            }

            parsePatterns(closeArray, data);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "{0} has incomplete data from Yahoo chart API: {1}",
                    new Object[]{data.getTicker(), e.getMessage()});
            data.setIncomplete(true);
        }
    }

    private void parsePatterns(JsonArray closeArray, HistoricalData data) {
        var mc = new MathContext(2);
        List<Integer> upDownList = new ArrayList<>();
        BigDecimal previousClosePrice = BigDecimal.ZERO;

        int limit = Math.min(closeArray.size(), PATTERN_DAYS + 1);

        for (int i = 0; i < limit; i++) {
            JsonElement el = closeArray.get(i);
            if (el == null || el.isJsonNull()) continue;

            BigDecimal closePrice = el.getAsBigDecimal();

            if (i > 0 && previousClosePrice.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal closeChange = closePrice.subtract(previousClosePrice)
                        .divide(previousClosePrice, 10, RoundingMode.CEILING)
                        .multiply(new BigDecimal(100), mc);

                upDownList.add(closeChange.signum());
                data.getPatterns().put(upDownList.toString(), data.getTicker());
            }

            previousClosePrice = closePrice;
        }
    }
}
