import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;

import org.apache.commons.lang3.StringEscapeUtils;

public class GetYahooQuotes {

    private static final Logger LOGGER = Logger.getLogger(GetYahooQuotes.class.getName());
    private static final String USER_AGENT = "Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2.13) " +
            "Gecko/20101206 Ubuntu/10.10 (maverick) Firefox/3.6.13";

    private final HttpClient client;
    private final HttpClientContext context;

    public GetYahooQuotes() {
        CookieStore cookieStore = new BasicCookieStore();
        this.client = HttpClientBuilder.create().build();
        this.context = HttpClientContext.create();
        context.setCookieStore(cookieStore);
    }

    public String getPage(String symbol) {
        String url = "https://finance.yahoo.com/quote/%s/?p=%s".formatted(symbol, symbol);
        var request = new HttpGet(url);
        request.addHeader("User-Agent", USER_AGENT);

        try {
            HttpResponse response = client.execute(request, context);
            int statusCode = response.getStatusLine().getStatusCode();
            LOGGER.log(Level.FINE, "Response Code: {0}", statusCode);

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

    public List<String> splitPageData(String page) {
        return Arrays.asList(page.split("}"));
    }

    public String findCrumb(List<String> lines) {
        String rtn = "";
        for (String l : lines) {
            if (l.contains("CrumbStore")) {
                rtn = l;
                break;
            }
        }

        if (rtn != null && !rtn.isEmpty()) {
            String[] vals = rtn.split(":");
            String crumb = vals[2].replace("\"", "");
            return StringEscapeUtils.unescapeJava(crumb);
        }
        return "";
    }

    public String getCrumb(String symbol) {
        String page = getPage(symbol);
        if (page == null) return "";
        return findCrumb(splitPageData(page));
    }

    public void downloadData(String symbol, long startDate, long endDate, String crumb) {
        String filename = "%s.csv".formatted(symbol);
        String url = "https://query1.finance.yahoo.com/v7/finance/download/%s?period1=%s&period2=%s&interval=1d&events=history&crumb=%s"
                .formatted(symbol, startDate, endDate, crumb);

        var request = new HttpGet(url);
        request.addHeader("User-Agent", USER_AGENT);

        try {
            HttpResponse response = client.execute(request, context);
            int statusCode = response.getStatusLine().getStatusCode();
            String reasonPhrase = response.getStatusLine().getReasonPhrase();

            LOGGER.log(Level.FINE, "statusCode: {0}, reasonPhrase: {1}",
                    new Object[]{statusCode, reasonPhrase});

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

    public static void main(String[] args) {
        var c = new GetYahooQuotes();
        String symbol = args.length > 0 ? args[0] : "aapl";

        String crumb = c.getCrumb(symbol);
        if (crumb != null && !crumb.isEmpty()) {
            LOGGER.log(Level.INFO, "Downloading data for {0}", symbol);
            c.downloadData(symbol, 0, System.currentTimeMillis() / 1000, crumb);
        } else {
            LOGGER.log(Level.WARNING, "Error retrieving data for {0}", symbol);
        }
    }
}
