import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.HashMultimap;

public class Trends {

    private static final Logger LOGGER = Logger.getLogger(Trends.class.getName());

    public static void main(String[] args) throws Exception {
        var stockList = new TreeSet<String>();
        var errorList = new TreeSet<String>();

        // Program Timer
        TimeEstimates timer = new TimeEstimates();

        var dates = new Dates();

        System.out.println("Downloading lists, please wait...");

        var sd = new StockDownloader();

        sd.downloadNasdaq();
        sd.downloadOthers();
        sd.downloadZacks();
        sd.downloadYahooEarnings(dates.getTodayMarket());
        sd.downloadYahooEarnings(dates.getTomorrowMarket());
        sd.downloadYahooEarnings(dates.getYesterdayMarket());
        sd.readIncomplete();

        stockList.addAll(sd.getNasdaqList());
        stockList.addAll(sd.getOthersList());
        stockList.removeAll(sd.getIncompleteList());

        System.out.println("Nasdaq stocks downloaded: " + sd.getNasdaqList().size());
        System.out.println("Other stocks downloaded: " + sd.getOthersList().size());
        System.out.println("Mutual funds downloaded: " + sd.getMfundsList().size());
        System.out.println("Zacks stocks downloaded: " + sd.getZacksList().size());
        System.out.println("Earnings stocks downloaded: " + sd.getEarningsList().size());
        System.out.println("Stocks with incomplete data: " + sd.getIncompleteList().size());
        System.out.println("Total stocks to be processed: " + stockList.size());
        System.out.println();

        HashMultimap<String, String> patterns = HashMultimap.create();

        int count = 1;

        for (String ticker : stockList) {
            timer.setLoopStartTime();

            var yh = new YahooHistoricalData(ticker, dates);
            yh.downloadYahooHistorical(ticker);

            if (yh.isIncomplete()) {
                sd.appendIncomplete(ticker);
            }

            patterns.putAll(yh.getPatterns());

            if (count == 100) {
                for (String pattern : patterns.keySet()) {
                    System.out.println(pattern + "\t" + patterns.get(pattern));
                }
            }

            count++;
        }

        System.out.println(errorList);

        int endList = stockList.size() - 1;
        timer.getTimeEstimates(endList, stockList.size(), dates);
    }

    public static void writeFile(String content, String filename) throws IOException {
        String cleaned = content.replace("[", "").replace("]", "");
        Files.writeString(Path.of(filename), cleaned, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static TreeSet<String> readFile(String filename) throws IOException {
        var list = new TreeSet<String>();
        for (String line : Files.readAllLines(Path.of(filename), StandardCharsets.UTF_8)) {
            Collections.addAll(list, line.split("\\s*,\\s*"));
        }
        return list;
    }

    public void deleteFile(String filename) {
        try {
            Path path = Path.of(filename);
            if (Files.deleteIfExists(path)) {
                LOGGER.log(Level.INFO, "{0} deleted.", filename);
            } else {
                LOGGER.log(Level.WARNING, "Delete operation failed for {0}.", filename);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error deleting file: {0}", e.getMessage());
        }
    }

    public void appendToFile(String str, String filename) {
        try {
            Files.writeString(Path.of(filename), str + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error appending to file: {0}", e.getMessage());
        }
    }
}
