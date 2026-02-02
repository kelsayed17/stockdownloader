package com.stockdownloader.data;

import com.stockdownloader.model.OptionContract;
import com.stockdownloader.model.OptionType;
import com.stockdownloader.model.OptionsChain;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Downloads options chain data from Yahoo Finance v7 options API.
 * Captures full contract details including volume, open interest, IV, and greeks.
 *
 * Endpoint: https://query1.finance.yahoo.com/v7/finance/options/{ticker}
 * Supports querying specific expiration dates via ?date={epoch} parameter.
 */
public final class YahooOptionsClient {

    private static final Logger LOGGER = Logger.getLogger(YahooOptionsClient.class.getName());
    private static final int MAX_RETRIES = 3;
    private static final String OPTIONS_URL =
            "https://query1.finance.yahoo.com/v7/finance/options/%s";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final YahooAuthHelper auth;

    public YahooOptionsClient() {
        this(new YahooAuthHelper());
    }

    public YahooOptionsClient(YahooAuthHelper auth) {
        this.auth = auth;
    }

    /**
     * Downloads the full options chain for a ticker (all available expirations).
     */
    public OptionsChain download(String ticker) {
        var chain = new OptionsChain(ticker);

        if (auth.getCrumb() == null) {
            auth.authenticate();
        }

        // First request gets expiration dates and the nearest expiration chain
        RetryExecutor.execute(() -> {
            String url = (OPTIONS_URL + "?crumb=%s").formatted(ticker, auth.getCrumb());
            String json = fetchJson(url);
            parseOptionsJson(json, chain);
        }, MAX_RETRIES, LOGGER, "options chain download for " + ticker);

        // Fetch remaining expirations
        for (String expDate : chain.getExpirationDates()) {
            if (chain.getCalls(expDate).isEmpty() && chain.getPuts(expDate).isEmpty()) {
                RetryExecutor.execute(() -> {
                    long epoch = LocalDate.parse(expDate, DATE_FMT)
                            .atStartOfDay(ZoneId.of("America/New_York"))
                            .toEpochSecond();
                    String url = (OPTIONS_URL + "?date=%d&crumb=%s").formatted(ticker, epoch, auth.getCrumb());
                    String json = fetchJson(url);
                    parseOptionsJson(json, chain);
                }, MAX_RETRIES, LOGGER, "options chain for " + ticker + " exp:" + expDate);
            }
        }

        return chain;
    }

    /**
     * Downloads options chain for a specific expiration date.
     */
    public OptionsChain downloadForExpiration(String ticker, String expirationDate) {
        var chain = new OptionsChain(ticker);

        if (auth.getCrumb() == null) {
            auth.authenticate();
        }

        RetryExecutor.execute(() -> {
            long epoch = LocalDate.parse(expirationDate, DATE_FMT)
                    .atStartOfDay(ZoneId.of("America/New_York"))
                    .toEpochSecond();
            String url = (OPTIONS_URL + "?date=%d&crumb=%s").formatted(ticker, epoch, auth.getCrumb());
            String json = fetchJson(url);
            parseOptionsJson(json, chain);
        }, MAX_RETRIES, LOGGER, "options chain for " + ticker + " exp:" + expirationDate);

        return chain;
    }

    private String fetchJson(String url) throws Exception {
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
            return sb.toString();
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    private void parseOptionsJson(String json, OptionsChain chain) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject optionChain = root.getAsJsonObject("optionChain");
            if (optionChain == null) return;

            JsonArray results = optionChain.getAsJsonArray("result");
            if (results == null || results.isEmpty()) return;

            JsonObject result = results.get(0).getAsJsonObject();

            // Parse underlying price
            JsonObject quote = result.getAsJsonObject("quote");
            if (quote != null) {
                chain.setUnderlyingPrice(getDecimal(quote, "regularMarketPrice"));
            }

            // Parse expiration dates
            JsonArray expirations = result.getAsJsonArray("expirationDates");
            if (expirations != null) {
                for (JsonElement exp : expirations) {
                    String dateStr = epochToDate(exp.getAsLong());
                    chain.addExpirationDate(dateStr);
                }
            }

            // Parse options contracts
            JsonArray options = result.getAsJsonArray("options");
            if (options == null || options.isEmpty()) return;

            JsonObject optionData = options.get(0).getAsJsonObject();

            String expDate = null;
            if (optionData.has("expirationDate")) {
                expDate = epochToDate(optionData.get("expirationDate").getAsLong());
            }

            // Parse calls
            JsonArray calls = optionData.getAsJsonArray("calls");
            if (calls != null && expDate != null) {
                for (JsonElement el : calls) {
                    OptionContract contract = parseContract(el.getAsJsonObject(), OptionType.CALL, expDate);
                    if (contract != null) {
                        chain.addCall(expDate, contract);
                    }
                }
            }

            // Parse puts
            JsonArray puts = optionData.getAsJsonArray("puts");
            if (puts != null && expDate != null) {
                for (JsonElement el : puts) {
                    OptionContract contract = parseContract(el.getAsJsonObject(), OptionType.PUT, expDate);
                    if (contract != null) {
                        chain.addPut(expDate, contract);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error parsing options chain JSON: {0}", e.getMessage());
        }
    }

    private OptionContract parseContract(JsonObject obj, OptionType type, String expDate) {
        try {
            return new OptionContract(
                    getString(obj, "contractSymbol"),
                    type,
                    getDecimal(obj, "strike"),
                    expDate,
                    getDecimal(obj, "lastPrice"),
                    getDecimal(obj, "bid"),
                    getDecimal(obj, "ask"),
                    getLong(obj, "volume"),
                    getLong(obj, "openInterest"),
                    getDecimal(obj, "impliedVolatility"),
                    getDecimal(obj, "delta"),
                    getDecimal(obj, "gamma"),
                    getDecimal(obj, "theta"),
                    getDecimal(obj, "vega"),
                    getBoolean(obj, "inTheMoney")
            );
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Skipping malformed option contract: {0}", e.getMessage());
            return null;
        }
    }

    private static String epochToDate(long epoch) {
        return Instant.ofEpochSecond(epoch)
                .atZone(ZoneId.of("America/New_York"))
                .toLocalDate()
                .format(DATE_FMT);
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

    private static boolean getBoolean(JsonObject obj, String field) {
        JsonElement el = obj.get(field);
        if (el == null || el.isJsonNull()) return false;
        return el.getAsBoolean();
    }
}
