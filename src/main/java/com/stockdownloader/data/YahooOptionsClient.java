package com.stockdownloader.data;

import com.stockdownloader.model.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fetches options chain data from the Yahoo Finance v7 options API.
 * Retrieves all available expirations, strikes, calls, puts, volume,
 * open interest, and Greeks (implied volatility) for a given symbol.
 */
public final class YahooOptionsClient {

    private static final Logger LOGGER = Logger.getLogger(YahooOptionsClient.class.getName());
    private static final int MAX_RETRIES = 3;
    private static final String OPTIONS_URL =
            "https://query1.finance.yahoo.com/v7/finance/options/%s";

    private final YahooAuthHelper auth;

    public YahooOptionsClient() {
        this(new YahooAuthHelper());
    }

    public YahooOptionsClient(YahooAuthHelper auth) {
        this.auth = auth;
    }

    /**
     * Downloads the full options chain for a symbol across all available expirations.
     */
    public OptionsChain downloadOptionsChain(String symbol) {
        if (auth.getCrumb() == null) {
            auth.authenticate();
        }

        // First call without expiration to get list of all expiration dates
        JsonObject firstResponse = fetchOptionsJson(symbol, null);
        if (firstResponse == null) {
            LOGGER.warning("Failed to fetch options data for " + symbol);
            return emptyChain(symbol);
        }

        BigDecimal underlyingPrice = extractUnderlyingPrice(firstResponse);
        List<Long> expirationTimestamps = extractExpirationTimestamps(firstResponse);
        String quoteDate = LocalDate.now().toString();

        List<OptionContract> allCalls = new ArrayList<>();
        List<OptionContract> allPuts = new ArrayList<>();
        List<String> expirationDates = new ArrayList<>();

        // Parse the first expiration from the initial response
        parseOptionsFromResponse(firstResponse, symbol, allCalls, allPuts, expirationDates);

        // Fetch remaining expirations
        for (int i = 1; i < expirationTimestamps.size(); i++) {
            Long expTs = expirationTimestamps.get(i);
            JsonObject response = fetchOptionsJson(symbol, expTs);
            if (response != null) {
                parseOptionsFromResponse(response, symbol, allCalls, allPuts, expirationDates);
            }
        }

        // Deduplicate expiration dates
        List<String> uniqueExpirations = expirationDates.stream().distinct().sorted().toList();

        LOGGER.log(Level.INFO, "Downloaded options chain for {0}: {1} calls, {2} puts, {3} expirations",
                new Object[]{symbol, allCalls.size(), allPuts.size(), uniqueExpirations.size()});

        return new OptionsChain(symbol, underlyingPrice, quoteDate, uniqueExpirations, allCalls, allPuts);
    }

    /**
     * Downloads options chain for a specific expiration date.
     */
    public OptionsChain downloadOptionsForExpiration(String symbol, String expirationDate) {
        if (auth.getCrumb() == null) {
            auth.authenticate();
        }

        long expTimestamp = LocalDate.parse(expirationDate)
                .atStartOfDay(ZoneId.of("America/New_York"))
                .toEpochSecond();

        JsonObject response = fetchOptionsJson(symbol, expTimestamp);
        if (response == null) {
            return emptyChain(symbol);
        }

        BigDecimal underlyingPrice = extractUnderlyingPrice(response);
        List<OptionContract> calls = new ArrayList<>();
        List<OptionContract> puts = new ArrayList<>();
        List<String> expirations = new ArrayList<>();

        parseOptionsFromResponse(response, symbol, calls, puts, expirations);

        return new OptionsChain(symbol, underlyingPrice, LocalDate.now().toString(),
                expirations, calls, puts);
    }

    private JsonObject fetchOptionsJson(String symbol, Long expirationTimestamp) {
        return RetryExecutor.execute(() -> {
            String url = OPTIONS_URL.formatted(symbol);
            if (expirationTimestamp != null) {
                url += "?date=" + expirationTimestamp;
            }
            if (auth.getCrumb() != null) {
                url += (expirationTimestamp != null ? "&" : "?") + "crumb=" + auth.getCrumb();
            }

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
                return JsonParser.parseString(sb.toString()).getAsJsonObject();
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        }, MAX_RETRIES, LOGGER, "options chain fetch for " + symbol);
    }

    private BigDecimal extractUnderlyingPrice(JsonObject root) {
        try {
            JsonObject optionChain = root.getAsJsonObject("optionChain");
            JsonArray results = optionChain.getAsJsonArray("result");
            if (results != null && !results.isEmpty()) {
                JsonObject result = results.get(0).getAsJsonObject();
                JsonObject quote = result.getAsJsonObject("quote");
                if (quote != null && quote.has("regularMarketPrice")) {
                    return quote.get("regularMarketPrice").getAsBigDecimal();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not extract underlying price: {0}", e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    private List<Long> extractExpirationTimestamps(JsonObject root) {
        List<Long> timestamps = new ArrayList<>();
        try {
            JsonObject optionChain = root.getAsJsonObject("optionChain");
            JsonArray results = optionChain.getAsJsonArray("result");
            if (results != null && !results.isEmpty()) {
                JsonArray expirations = results.get(0).getAsJsonObject().getAsJsonArray("expirationDates");
                if (expirations != null) {
                    for (JsonElement el : expirations) {
                        timestamps.add(el.getAsLong());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not extract expiration dates: {0}", e.getMessage());
        }
        return timestamps;
    }

    private void parseOptionsFromResponse(JsonObject root, String symbol,
                                          List<OptionContract> calls, List<OptionContract> puts,
                                          List<String> expirationDates) {
        try {
            JsonObject optionChain = root.getAsJsonObject("optionChain");
            JsonArray results = optionChain.getAsJsonArray("result");
            if (results == null || results.isEmpty()) return;

            JsonObject result = results.get(0).getAsJsonObject();
            JsonArray options = result.getAsJsonArray("options");
            if (options == null || options.isEmpty()) return;

            for (JsonElement optionEl : options) {
                JsonObject optionObj = optionEl.getAsJsonObject();

                if (optionObj.has("expirationDate")) {
                    long expTs = optionObj.get("expirationDate").getAsLong();
                    String expDate = Instant.ofEpochSecond(expTs)
                            .atZone(ZoneId.of("America/New_York"))
                            .toLocalDate().toString();
                    expirationDates.add(expDate);
                }

                JsonArray callsArray = optionObj.getAsJsonArray("calls");
                if (callsArray != null) {
                    for (JsonElement el : callsArray) {
                        OptionContract contract = parseContract(el.getAsJsonObject(), symbol, OptionType.CALL);
                        if (contract != null) calls.add(contract);
                    }
                }

                JsonArray putsArray = optionObj.getAsJsonArray("puts");
                if (putsArray != null) {
                    for (JsonElement el : putsArray) {
                        OptionContract contract = parseContract(el.getAsJsonObject(), symbol, OptionType.PUT);
                        if (contract != null) puts.add(contract);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error parsing options response for {0}: {1}",
                    new Object[]{symbol, e.getMessage()});
        }
    }

    private OptionContract parseContract(JsonObject json, String symbol, OptionType type) {
        try {
            String contractSymbol = getStringOrDefault(json, "contractSymbol", "");
            BigDecimal strike = getBigDecimalOrZero(json, "strike");
            BigDecimal lastPrice = getBigDecimalOrZero(json, "lastPrice");
            BigDecimal bid = getBigDecimalOrZero(json, "bid");
            BigDecimal ask = getBigDecimalOrZero(json, "ask");
            long volume = getLongOrZero(json, "volume");
            long openInterest = getLongOrZero(json, "openInterest");
            boolean itm = json.has("inTheMoney") && json.get("inTheMoney").getAsBoolean();
            BigDecimal iv = getBigDecimalOrZero(json, "impliedVolatility");

            long expTs = json.has("expiration") ? json.get("expiration").getAsLong() : 0;
            String expDate = expTs > 0
                    ? Instant.ofEpochSecond(expTs).atZone(ZoneId.of("America/New_York")).toLocalDate().toString()
                    : "";

            Greeks greeks = new Greeks(
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, iv);

            return new OptionContract(contractSymbol, symbol, type, strike, expDate,
                    lastPrice, bid, ask, volume, openInterest, greeks, itm);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error parsing option contract: {0}", e.getMessage());
            return null;
        }
    }

    private static String getStringOrDefault(JsonObject json, String field, String defaultValue) {
        return json.has(field) && !json.get(field).isJsonNull()
                ? json.get(field).getAsString() : defaultValue;
    }

    private static BigDecimal getBigDecimalOrZero(JsonObject json, String field) {
        return json.has(field) && !json.get(field).isJsonNull()
                ? json.get(field).getAsBigDecimal() : BigDecimal.ZERO;
    }

    private static long getLongOrZero(JsonObject json, String field) {
        return json.has(field) && !json.get(field).isJsonNull()
                ? json.get(field).getAsLong() : 0;
    }

    private OptionsChain emptyChain(String symbol) {
        return new OptionsChain(symbol, BigDecimal.ZERO, LocalDate.now().toString(),
                List.of(), List.of(), List.of());
    }
}
