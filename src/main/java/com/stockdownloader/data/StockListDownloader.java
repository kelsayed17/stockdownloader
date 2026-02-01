package com.stockdownloader.data;

import com.stockdownloader.util.CsvParser;
import com.stockdownloader.util.FileHelper;
import com.stockdownloader.util.RetryExecutor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * Downloads stock ticker lists from NASDAQ FTP, Zacks, and Yahoo Earnings.
 * Uses RetryExecutor for reliable downloads and FileHelper for I/O.
 */
public class StockListDownloader {

    private static final Logger LOGGER = Logger.getLogger(StockListDownloader.class.getName());
    private static final int MAX_RETRIES = 3;
    private static final String INCOMPLETE_FILE = "incomplete.txt";

    private TreeSet<String> nasdaqList = new TreeSet<>();
    private TreeSet<String> othersList = new TreeSet<>();
    private TreeSet<String> mfundsList = new TreeSet<>();
    private TreeSet<String> zacksList = new TreeSet<>();
    private TreeSet<String> earningsList = new TreeSet<>();
    private TreeSet<String> incompleteList = new TreeSet<>();

    public void downloadNasdaq() {
        String url = "ftp://ftp.nasdaqtrader.com/SymbolDirectory/nasdaqlisted.txt";
        String file = "nasdaqlisted.txt";
        downloadFile(url, file);
        nasdaqList = readPipeDelimitedFile(file);
    }

    public void downloadOthers() {
        String url = "ftp://ftp.nasdaqtrader.com/SymbolDirectory/otherlisted.txt";
        String file = "otherslisted.txt";
        downloadFile(url, file);
        othersList = readPipeDelimitedFile(file);
    }

    public void downloadMutualFunds() {
        String url = "ftp://ftp.nasdaqtrader.com/SymbolDirectory/mfundslist.txt";
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
            Document doc = Jsoup.connect(url).get();
            Element table = doc.select("#fin-cal-table > div:eq(2) > table > tbody").first();

            if (table != null) {
                for (Element tr : table.children()) {
                    String ticker = tr.child(1).text();
                    String time = tr.child(3).text();
                    yahooEarnings.put(time, ticker);
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
