import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

public class StockDownloader {

    private static final Logger LOGGER = Logger.getLogger(StockDownloader.class.getName());
    private static final int MAX_RETRIES = 3;

    private TreeSet<String> nasdaqList = new TreeSet<>();
    private TreeSet<String> othersList = new TreeSet<>();
    private TreeSet<String> mfundsList = new TreeSet<>();
    private TreeSet<String> zacksList = new TreeSet<>();
    private TreeSet<String> earningsList = new TreeSet<>();
    private TreeSet<String> incompleteList = new TreeSet<>();

    private static TreeSet<String> readDownloadedFile(String filename) {
        var list = new TreeSet<String>();

        try (var reader = new BufferedReader(new FileReader(filename))) {
            // Skip first line
            reader.readLine();

            String current = reader.readLine();
            String next = reader.readLine();

            while (current != null) {
                if (next == null) break; // Skip last line

                String ticker = current.substring(0, current.indexOf('|'));
                list.add(ticker);
                current = next;
                next = reader.readLine();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading downloaded file {0}: {1}",
                    new Object[]{filename, e.getMessage()});
        }

        return list;
    }

    public void downloadNasdaq() {
        String url = "ftp://ftp.nasdaqtrader.com/SymbolDirectory/nasdaqlisted.txt";
        String file = "nasdaqlisted.txt";
        downloadFile(url, file);
        nasdaqList = readDownloadedFile(file);
    }

    public void downloadOthers() {
        String url = "ftp://ftp.nasdaqtrader.com/SymbolDirectory/otherlisted.txt";
        String file = "otherslisted.txt";
        downloadFile(url, file);
        othersList = readDownloadedFile(file);
    }

    public void downloadMutualFunds() {
        String url = "ftp://ftp.nasdaqtrader.com/SymbolDirectory/mfundslist.txt";
        String file = "mfundslist.txt";
        downloadFile(url, file);
        mfundsList = readDownloadedFile(file);
    }

    public void downloadZacks() {
        String url = "https://www.zacks.com/portfolios/rank/rank_excel.php?rank=1&reference_id=all";
        String file = "rank_1.txt";
        downloadFile(url, file);

        CSVParser parser = new CSVParserBuilder().withSeparator('\t').build();
        try (var reader = new CSVReaderBuilder(new FileReader(file)).withSkipLines(2).withCSVParser(parser).build()) {
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                zacksList.add(nextLine[0]);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading Zacks file: {0}", e.getMessage());
        }
    }

    public void downloadYahooEarnings(Calendar calendar) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String date = dateFormat.format(calendar.getTime());
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
                List<String> list = yahooEarnings.get(time);
                earningsList.addAll(list);
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
        incompleteList = readFile("incomplete.txt");
    }

    public void addIncomplete(String ticker) {
        incompleteList.add(ticker);
    }

    public void appendIncomplete(String ticker) {
        appendToFile(ticker, "incomplete.txt");
    }

    public void writeIncomplete() {
        writeFile(incompleteList, "incomplete.txt");
    }

    private void downloadFile(String url, String filename) {
        downloadFile(url, filename, 0);
    }

    private void downloadFile(String url, String filename, int retryCount) {
        try {
            var fileUrl = URI.create(url).toURL();
            var file = Path.of(filename).toFile();
            FileUtils.copyURLToFile(fileUrl, file);
        } catch (Exception e) {
            if (retryCount < MAX_RETRIES) {
                LOGGER.log(Level.FINE, "Retrying download for {0}, attempt {1}",
                        new Object[]{filename, retryCount + 1});
                downloadFile(url, filename, retryCount + 1);
            } else {
                LOGGER.log(Level.WARNING, "Failed to download {0} after {1} retries: {2}",
                        new Object[]{filename, MAX_RETRIES, e.getMessage()});
            }
        }
    }

    private void writeFile(Set<String> list, String filename) {
        try {
            Files.writeString(Path.of(filename), String.join(System.lineSeparator(), list),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error writing file {0}: {1}",
                    new Object[]{filename, e.getMessage()});
        }
    }

    private TreeSet<String> readFile(String filename) {
        var list = new TreeSet<String>();

        try {
            Path path = Path.of(filename);
            if (Files.exists(path)) {
                for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                    if (!line.isBlank()) {
                        list.add(line.trim());
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading file {0}: {1}",
                    new Object[]{filename, e.getMessage()});
        }

        return list;
    }

    public void deleteFile(String filename) {
        try {
            if (Files.deleteIfExists(Path.of(filename))) {
                LOGGER.log(Level.INFO, "{0} deleted.", filename);
            } else {
                LOGGER.log(Level.WARNING, "File {0} does not exist.", filename);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error deleting {0}: {1}",
                    new Object[]{filename, e.getMessage()});
        }
    }

    private void appendToFile(String str, String filename) {
        try {
            Files.writeString(Path.of(filename), str + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error appending to {0}: {1}",
                    new Object[]{filename, e.getMessage()});
        }
    }

    // Getters - return unmodifiable views
    public Set<String> getNasdaqList() { return Set.copyOf(nasdaqList); }
    public Set<String> getOthersList() { return Set.copyOf(othersList); }
    public Set<String> getMfundsList() { return Set.copyOf(mfundsList); }
    public Set<String> getZacksList() { return Set.copyOf(zacksList); }
    public Set<String> getEarningsList() { return Set.copyOf(earningsList); }
    public Set<String> getIncompleteList() { return Set.copyOf(incompleteList); }
}
