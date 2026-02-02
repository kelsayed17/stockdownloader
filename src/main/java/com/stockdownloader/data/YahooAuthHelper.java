package com.stockdownloader.data;

import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shared Yahoo Finance authentication helper that obtains and caches
 * the cookie/crumb pair required by Yahoo Finance API endpoints.
 *
 * Flow:
 * 1. GET https://fc.yahoo.com to obtain session cookie
 * 2. GET https://query2.finance.yahoo.com/v1/test/getcrumb with cookie to get crumb
 * 3. Reuse cookie + crumb for all subsequent API calls
 */
public final class YahooAuthHelper {

    private static final Logger LOGGER = Logger.getLogger(YahooAuthHelper.class.getName());
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String COOKIE_URL = "https://fc.yahoo.com";
    private static final String CRUMB_URL = "https://query2.finance.yahoo.com/v1/test/getcrumb";

    private final HttpClient client;
    private final HttpClientContext context;
    private String crumb;

    public YahooAuthHelper() {
        CookieStore cookieStore = new BasicCookieStore();
        this.client = HttpClientBuilder.create()
                .setDefaultCookieStore(cookieStore)
                .build();
        this.context = HttpClientContext.create();
        context.setCookieStore(cookieStore);
    }

    /**
     * Initializes authentication by obtaining cookie and crumb.
     * Returns true if successful.
     */
    public boolean authenticate() {
        try {
            // Step 1: Get cookie from fc.yahoo.com
            var cookieRequest = new HttpGet(COOKIE_URL);
            cookieRequest.addHeader("User-Agent", USER_AGENT);
            HttpResponse cookieResponse = client.execute(cookieRequest, context);
            HttpClientUtils.closeQuietly(cookieResponse);

            // Step 2: Get crumb using the cookie
            var crumbRequest = new HttpGet(CRUMB_URL);
            crumbRequest.addHeader("User-Agent", USER_AGENT);
            HttpResponse crumbResponse = client.execute(crumbRequest, context);

            try (var reader = new BufferedReader(
                    new InputStreamReader(crumbResponse.getEntity().getContent()))) {
                this.crumb = reader.readLine();
            } finally {
                HttpClientUtils.closeQuietly(crumbResponse);
            }

            if (crumb != null && !crumb.isEmpty() && !crumb.contains("Too Many Requests")) {
                LOGGER.log(Level.FINE, "Yahoo Finance authentication successful");
                return true;
            } else {
                LOGGER.log(Level.WARNING, "Failed to obtain valid crumb from Yahoo Finance");
                return false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Yahoo Finance authentication failed: {0}", e.getMessage());
            return false;
        }
    }

    public String getCrumb() {
        return crumb;
    }

    public HttpClient getClient() {
        return client;
    }

    public HttpClientContext getContext() {
        return context;
    }

    public String getUserAgent() {
        return USER_AGENT;
    }
}
