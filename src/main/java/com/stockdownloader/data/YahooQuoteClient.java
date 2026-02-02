package com.stockdownloader.data;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;

/**
 * HTTP client for Yahoo Finance that handles authentication via the modern
 * cookie/crumb flow (fc.yahoo.com + /v1/test/getcrumb) and historical
 * CSV data downloads.
 *
 * Replaces the legacy crumb extraction that parsed "CrumbStore" from
 * Yahoo Finance page HTML, which no longer works.
 */
public final class YahooQuoteClient {

    private static final Logger LOGGER = Logger.getLogger(YahooQuoteClient.class.getName());

    private final YahooAuthHelper auth;

    public YahooQuoteClient() {
        this(new YahooAuthHelper());
    }

    public YahooQuoteClient(YahooAuthHelper auth) {
        this.auth = auth;
    }

    public String getPage(String symbol) {
        String url = "https://finance.yahoo.com/quote/%s/".formatted(symbol);
        var request = new HttpGet(url);
        request.addHeader("User-Agent", auth.getUserAgent());

        try {
            HttpResponse response = auth.getClient().execute(request, auth.getContext());
            try (var rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                var result = new StringBuilder();
                String line;
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }
                return result.toString();
            } finally {
                HttpClientUtils.closeQuietly(response);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Failed to fetch page for symbol: {0}", symbol);
        }
        return null;
    }

    /**
     * Obtains a crumb for Yahoo Finance API authentication using the modern
     * fc.yahoo.com cookie flow instead of the deprecated CrumbStore page scraping.
     */
    public String getCrumb(String symbol) {
        if (auth.authenticate()) {
            return auth.getCrumb();
        }
        return "";
    }

    public void downloadData(String symbol, long startDate, long endDate, String crumb) {
        String filename = "%s.csv".formatted(symbol);
        String url = "https://query1.finance.yahoo.com/v7/finance/download/%s?period1=%s&period2=%s&interval=1d&events=history&crumb=%s"
                .formatted(symbol, startDate, endDate, crumb);

        var request = new HttpGet(url);
        request.addHeader("User-Agent", auth.getUserAgent());

        try {
            HttpResponse response = auth.getClient().execute(request, auth.getContext());
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                try (var bis = new BufferedInputStream(entity.getContent());
                     var bos = new BufferedOutputStream(new FileOutputStream(new File(filename)))) {
                    bis.transferTo(bos);
                }
            }
            HttpClientUtils.closeQuietly(response);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Failed to download data for symbol: {0}", symbol);
        }
    }

    /**
     * Returns the shared auth helper for use with other clients.
     */
    public YahooAuthHelper getAuth() {
        return auth;
    }
}
