package com.stockdownloader.data;

import com.stockdownloader.model.PriceData;
import com.stockdownloader.util.CsvParser;

import java.io.FileReader;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads PriceData from CSV files or streams in Yahoo Finance historical format.
 * Expected columns: Date, Open, High, Low, Close, Adj Close, Volume
 */
public final class CsvPriceDataLoader {

    private static final Logger LOGGER = Logger.getLogger(CsvPriceDataLoader.class.getName());

    private CsvPriceDataLoader() {}

    public static List<PriceData> loadFromFile(String filename) {
        try (var parser = new CsvParser(new FileReader(filename))) {
            return parseRecords(parser);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading CSV file {0}: {1}",
                    new Object[]{filename, e.getMessage()});
            return List.of();
        }
    }

    public static List<PriceData> loadFromStream(InputStream input) {
        try (var parser = new CsvParser(input)) {
            return parseRecords(parser);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading CSV from stream: {0}", e.getMessage());
            return List.of();
        }
    }

    private static List<PriceData> parseRecords(CsvParser parser) throws Exception {
        List<PriceData> data = new ArrayList<>();
        parser.readNext(); // skip header

        String[] line;
        while ((line = parser.readNext()) != null) {
            try {
                String date = line[0];
                var open = new BigDecimal(line[1]);
                var high = new BigDecimal(line[2]);
                var low = new BigDecimal(line[3]);
                var close = new BigDecimal(line[4]);
                var adjClose = line.length > 5 ? new BigDecimal(line[5]) : close;
                long volume = line.length > 6 ? Long.parseLong(line[6]) : 0;
                data.add(new PriceData(date, open, high, low, close, adjClose, volume));
            } catch (NumberFormatException e) {
                // Skip lines with invalid data (e.g., "null" values)
            }
        }
        return data;
    }
}
