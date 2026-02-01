package com.stockdownloader.data;

import com.stockdownloader.model.HistoricalData;
import com.stockdownloader.util.RetryExecutor;

import com.stockdownloader.util.CsvParser;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Downloads historical price data and computes price movement patterns.
 * Returns a populated HistoricalData model.
 */
public final class YahooHistoricalClient {

    private static final Logger LOGGER = Logger.getLogger(YahooHistoricalClient.class.getName());
    private static final int MAX_RETRIES = 3;
    private static final int PATTERN_DAYS = 7;

    public HistoricalData download(String ticker) {
        var data = new HistoricalData(ticker);

        RetryExecutor.execute(() -> {
            String url = "http://www.google.com/finance/historical?q=" + ticker + "&output=csv";

            try (InputStream input = URI.create(url).toURL().openStream()) {
                parsePatterns(input, data);
            }
        }, MAX_RETRIES, LOGGER, "historical download for " + ticker);

        return data;
    }

    private void parsePatterns(InputStream input, HistoricalData data) throws IOException {
        var mc = new MathContext(2);

        try (var parser = new CsvParser(input)) {
            List<Integer> upDownList = new ArrayList<>();
            BigDecimal previousClosePrice = BigDecimal.ZERO;

            parser.readNext(); // skip header

            int i = 0;
            String[] nextLine;
            while ((nextLine = parser.readNext()) != null) {
                BigDecimal closePrice = new BigDecimal(nextLine[4]);

                if (i > 0) {
                    BigDecimal closeChange = closePrice.subtract(previousClosePrice)
                            .divide(previousClosePrice, 10, RoundingMode.CEILING)
                            .multiply(new BigDecimal(100), mc);

                    upDownList.add(closeChange.signum());
                    data.getPatterns().put(upDownList.toString(), data.getTicker());
                }

                previousClosePrice = closePrice;
                if (i == PATTERN_DAYS) break;
                i++;
            }
        }
    }
}
