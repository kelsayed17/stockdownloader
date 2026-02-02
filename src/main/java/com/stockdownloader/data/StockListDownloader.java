package com.stockdownloader.data;

import com.stockdownloader.util.CsvParser;
import com.stockdownloader.util.FileHelper;
import com.stockdownloader.util.RetryExecutor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Downloads stock ticker lists from Nasdaq API, Zacks, and Yahoo Earnings.
 * Uses RetryExecutor for reliable downloads and FileHelper for I/O.
 *
 * NASDAQ FTP (ftp://ftp.nasdaqtrader.com/) has been replaced with:
 * - Nasdaq screener API: https://api.nasdaq.com/api/screener/stocks
 * - HTTP fallback: http://ftp.nasdaqtrader.com/dynamic/SymDir/ for symbol directory files
 */
public class StockListDownloader {

    private static final Logger LOGGER = Logger.getLogger(StockListDownloader.class.getName());
    private static final int MAX_RETRIES = 3;
    private static final String INCOMPLETE_FILE = "incomplete.txt";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private TreeSet<String> nasdaqList = new TreeSet<>();
    private TreeSet<String> othersList = new TreeSet<>();
    private TreeSet<String> mfundsList = new TreeSet<>();
    private TreeSet<String> zacksList = new TreeSet<>();
    private TreeSet<String> earningsList = new TreeSet<>();
    private TreeSet<String> incompleteList = new TreeSet<>();

    public void downloadNasdaq() {
        // Primary: use Nasdaq screener API
        RetryExecutor.execute(() -> {
            nasdaqList = downloadFromNasdaqApi("nasdaq");
        }, MAX_RETRIES, LOGGER, "Nasdaq stock list download");

        // Fallback: use HTTP symbol directory if API returned nothing
        if (nasdaqList.isEmpty()) {
            String url = "http://ftp.nasdaqtrader.com/dynamic/SymDir/nasdaqlisted.txt";
            String file = "nasdaqlisted.txt";
            downloadFile(url, file);
            nasdaqList = readPipeDelimitedFile(file);
        }
    }

    public void downloadOthers() {
        // Primary: use Nasdaq screener API for NYSE and AMEX
        RetryExecutor.execute(() -> {
            TreeSet<String> nyse = downloadFromNasdaqApi("nyse");
            TreeSet<String> amex = downloadFromNasdaqApi("amex");
            othersList.addAll(nyse);
            othersList.addAll(amex);
        }, MAX_RETRIES, LOGGER, "Other exchanges stock list download");

        // Fallback: use HTTP symbol directory
        if (othersList.isEmpty()) {
            String url = "http://ftp.nasdaqtrader.com/dynamic/SymDir/otherlisted.txt";
            String file = "otherslisted.txt";
            downloadFile(url, file);
            othersList = readPipeDelimitedFile(file);
        }
    }

    public void downloadMutualFunds() {
        String url = "http://ftp.nasdaqtrader.com/dynamic/SymDir/mfundslist.txt";
        String file = "mfundslist.txt";
        downloadFile(url, file);
        mfundsList = readPipeDelimitedFile(file);
    }

    public void downloadZacks() {
        String url = "https://www.zacks.com/portfolios/rank/rank_excel.php?rank=1&reference_id=all";
        String file = "rank_1.txt";
        downloadFile(url, file);

        try (var parser = new CsvParser(new FileReader(file), '\t')) {
            parser.skipLines(2);
            String[] nextLine;
            while ((nextLine = parser.readNext()) != null) {
                zacksList.add(nextLine[0]);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading Zacks file: {0}", e.getMessage());
        }
    }

    public void downloadYahooEarnings(LocalDate marketDate) {
        String date = marketDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String url = "https://finance.yahoo.com/calendar/earnings?day=" + date;
        ListMultimap<String, String> yahooEarnings = ArrayListMultimap.create();

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .get();

            // Try multiple CSS selectors as Yahoo may change their page structure
            Element table = doc.select("table tbody").first();

            if (table != null) {
                for (Element tr : table.children()) {
                    if (tr.children().size() >= 4) {
                        String ticker = tr.child(1).text();
                        String time = tr.child(3).text();
                        yahooEarnings.put(time, ticker);
                    }
                }
            }

            for (String time : yahooEarnings.keySet()) {
                earningsList.addAll(yahooEarnings.get(time));
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error downloading Yahoo earnings for {0}: {1}",
                    new Object[]{date, e.getMessage()});
        }

        for (String time : yahooEarnings.keySet()) {
            System.out.println(date + "\t" + time + ":\t" + yahooEarnings.get(time));
        }
    }

    public void readIncomplete() {
        incompleteList = FileHelper.readLines(INCOMPLETE_FILE);
    }

    public void addIncomplete(String ticker) {
        incompleteList.add(ticker);
    }

    public void appendIncomplete(String ticker) {
        FileHelper.appendLine(ticker, INCOMPLETE_FILE);
    }

    public void writeIncomplete() {
        FileHelper.writeLines(incompleteList, INCOMPLETE_FILE);
    }

    /**
     * Downloads stock tickers from the Nasdaq screener API.
     * Endpoint: https://api.nasdaq.com/api/screener/stocks
     */
    private TreeSet<String> downloadFromNasdaqApi(String exchange) throws Exception {
        var tickers = new TreeSet<String>();

        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.nasdaq.com/api/screener/stocks?tableonly=true&limit=10000&exchange=" + exchange))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body();

        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        JsonObject data = root.getAsJsonObject("data");
        if (data == null) return tickers;

        JsonArray rows = data.getAsJsonArray("rows");
        if (rows == null) return tickers;

        for (JsonElement el : rows) {
            JsonObject row = el.getAsJsonObject();
            JsonElement symbol = row.get("symbol");
            if (symbol != null && !symbol.isJsonNull()) {
                String ticker = symbol.getAsString().trim();
                if (!ticker.isEmpty()) {
                    tickers.add(ticker);
                }
            }
        }

        return tickers;
    }

    private void downloadFile(String url, String filename) {
        RetryExecutor.execute(() -> {
            var fileUrl = URI.create(url).toURL();
            var file = Path.of(filename).toFile();
            FileUtils.copyURLToFile(fileUrl, file);
        }, MAX_RETRIES, LOGGER, "download " + filename);
    }

    private static TreeSet<String> readPipeDelimitedFile(String filename) {
        var list = new TreeSet<String>();
        try (var reader = new BufferedReader(new FileReader(filename))) {
            reader.readLine(); // skip header

            String current = reader.readLine();
            String next = reader.readLine();

            while (current != null) {
                if (next == null) break; // skip last line
                String ticker = current.substring(0, current.indexOf('|'));
                list.add(ticker);
                current = next;
                next = reader.readLine();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading file {0}: {1}",
                    new Object[]{filename, e.getMessage()});
        }
        return list;
    }

    public Set<String> getNasdaqList() { return Set.copyOf(nasdaqList); }
    public Set<String> getOthersList() { return Set.copyOf(othersList); }
    public Set<String> getMfundsList() { return Set.copyOf(mfundsList); }
    public Set<String> getZacksList() { return Set.copyOf(zacksList); }
    public Set<String> getEarningsList() { return Set.copyOf(earningsList); }
    public Set<String> getIncompleteList() { return Set.copyOf(incompleteList); }
}
