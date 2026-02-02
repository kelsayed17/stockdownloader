package com.stockdownloader.data;

import com.stockdownloader.model.FinancialData;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Downloads and parses fundamental financial data using Yahoo Finance
 * quoteSummary API, returning a populated FinancialData model.
 *
 * Replaces the deprecated Morningstar ReportProcess4CSV endpoint
 * (financials.morningstar.com) which is no longer functional.
 * Now uses Yahoo Finance's incomeStatementHistory and defaultKeyStatistics
 * modules to obtain revenue and shares outstanding data.
 */
public final class MorningstarClient {

    private static final Logger LOGGER = Logger.getLogger(MorningstarClient.class.getName());
    private static final int MAX_RETRIES = 3;
    private static final String QUOTE_SUMMARY_URL =
            "https://query1.finance.yahoo.com/v10/finance/quoteSummary/%s" +
            "?modules=incomeStatementHistory,incomeStatementHistoryQuarterly,defaultKeyStatistics" +
            "&crumb=%s";

    private final YahooAuthHelper auth;

    public MorningstarClient() {
        this(new YahooAuthHelper());
    }

    public MorningstarClient(YahooAuthHelper auth) {
        this.auth = auth;
    }

    public FinancialData download(String ticker) {
        var data = new FinancialData();

        if (auth.getCrumb() == null) {
            auth.authenticate();
        }

        RetryExecutor.execute(() -> {
            String url = QUOTE_SUMMARY_URL.formatted(ticker, auth.getCrumb());

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
                parseData(sb.toString(), data);
                data.computeRevenuePerShare();
            } catch (ArithmeticException | NullPointerException e) {
                LOGGER.log(Level.WARNING, "{0} has incomplete data from Yahoo Finance.", ticker);
                data.setIncomplete(true);
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }, MAX_RETRIES, LOGGER, "financial data download for " + ticker);

        return data;
    }

    private void parseData(String json, FinancialData data) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject quoteSummary = root.getAsJsonObject("quoteSummary");

        if (quoteSummary == null) {
            data.setIncomplete(true);
            return;
        }

        JsonArray results = quoteSummary.getAsJsonArray("result");
        if (results == null || results.isEmpty()) {
            data.setIncomplete(true);
            return;
        }

        JsonObject result = results.get(0).getAsJsonObject();

        // Parse quarterly income statements for revenue
        parseQuarterlyIncome(result, data);

        // Parse annual income statements for additional revenue data
        parseAnnualIncome(result, data);

        // Parse shares outstanding from defaultKeyStatistics
        parseKeyStatistics(result, data);
    }

    private void parseQuarterlyIncome(JsonObject result, FinancialData data) {
        JsonObject quarterly = result.getAsJsonObject("incomeStatementHistoryQuarterly");
        if (quarterly == null) return;

        JsonArray statements = quarterly.getAsJsonArray("incomeStatementHistory");
        if (statements == null) return;

        // Yahoo returns most recent quarters first
        int count = Math.min(statements.size(), 5);
        for (int i = 0; i < count; i++) {
            JsonObject stmt = statements.get(i).getAsJsonObject();

            long revenue = getRawLong(stmt, "totalRevenue");
            data.setRevenue(i, revenue);

            String endDate = getString(stmt, "endDate");
            if (!endDate.isEmpty()) {
                data.setFiscalQuarter(i, endDate);
            }
        }
    }

    private void parseAnnualIncome(JsonObject result, FinancialData data) {
        JsonObject annual = result.getAsJsonObject("incomeStatementHistory");
        if (annual == null) return;

        JsonArray statements = annual.getAsJsonArray("incomeStatementHistory");
        if (statements == null || statements.isEmpty()) return;

        // Use most recent annual as TTM approximation (index 5)
        JsonObject latestAnnual = statements.get(0).getAsJsonObject();
        long annualRevenue = getRawLong(latestAnnual, "totalRevenue");
        if (annualRevenue > 0) {
            data.setRevenue(5, annualRevenue);
        }
    }

    private void parseKeyStatistics(JsonObject result, FinancialData data) {
        JsonObject stats = result.getAsJsonObject("defaultKeyStatistics");
        if (stats == null) return;

        long sharesOutstanding = getRawLong(stats, "sharesOutstanding");
        long floatShares = getRawLong(stats, "floatShares");

        // Use shares outstanding as basic, float as diluted approximation
        for (int i = 0; i < 6; i++) {
            if (sharesOutstanding > 0) {
                data.setBasicShares(i, sharesOutstanding);
            }
            if (floatShares > 0) {
                data.setDilutedShares(i, floatShares);
            } else if (sharesOutstanding > 0) {
                data.setDilutedShares(i, sharesOutstanding);
            }
        }
    }

    private static long getRawLong(JsonObject obj, String field) {
        JsonElement el = obj.get(field);
        if (el == null || el.isJsonNull()) return 0;

        // Yahoo Finance wraps numeric values in {"raw": 123, "fmt": "123"}
        if (el.isJsonObject()) {
            JsonObject wrapper = el.getAsJsonObject();
            JsonElement raw = wrapper.get("raw");
            if (raw != null && !raw.isJsonNull()) {
                try {
                    return raw.getAsLong();
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }

        // Direct numeric value
        try {
            return el.getAsLong();
        } catch (Exception e) {
            return 0;
        }
    }

    private static String getString(JsonObject obj, String field) {
        JsonElement el = obj.get(field);
        if (el == null || el.isJsonNull()) return "";

        if (el.isJsonObject()) {
            JsonObject wrapper = el.getAsJsonObject();
            JsonElement fmt = wrapper.get("fmt");
            if (fmt != null && !fmt.isJsonNull()) {
                return fmt.getAsString();
            }
        }

        return el.getAsString();
    }
}
