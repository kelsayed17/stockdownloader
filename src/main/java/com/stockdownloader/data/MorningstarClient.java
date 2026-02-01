package com.stockdownloader.data;

import com.stockdownloader.model.FinancialData;
import com.stockdownloader.util.RetryExecutor;

import com.stockdownloader.util.CsvParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Downloads and parses fundamental financial data from Morningstar,
 * returning a populated FinancialData model.
 */
public class MorningstarClient {

    private static final Logger LOGGER = Logger.getLogger(MorningstarClient.class.getName());
    private static final int MAX_RETRIES = 3;

    public FinancialData download(String ticker) {
        var data = new FinancialData();

        RetryExecutor.execute(() -> {
            String url = "http://financials.morningstar.com/ajax/ReportProcess4CSV.html?&t=" + ticker
                    + "&reportType=is&period=3&dataType=A&order=asc&columnYear=5&rounding=1&view=raw&r=785679&denominatorView=raw&number=1";

            try (InputStream input = URI.create(url).toURL().openStream()) {
                parseData(input, data);
                data.computeRevenuePerShare();
            } catch (ArithmeticException | NullPointerException e) {
                LOGGER.log(Level.WARNING, "{0} has incomplete data from Morningstar.", ticker);
                data.setIncomplete(true);
            }
        }, MAX_RETRIES, LOGGER, "Morningstar download for " + ticker);

        return data;
    }

    private void parseData(InputStream input, FinancialData data) throws IOException {
        try (var parser = new CsvParser(input)) {
            for (String[] nextLine = parser.readNext(); nextLine != null; nextLine = parser.readNext()) {
                if (nextLine[0].contains("Fiscal year ends in")) {
                    for (int i = 0; i < 5; i++) {
                        data.setFiscalQuarter(i, nextLine[i + 1] + "-28");
                    }
                }

                if (isRevenueRow(nextLine[0]) && nextLine.length > 1) {
                    for (int i = 0; i < 6; i++) {
                        data.setRevenue(i, parseLong(nextLine[i + 1]));
                    }
                }

                if (nextLine[0].equals("Weighted average shares outstanding")) {
                    nextLine = parser.readNext();
                    if (nextLine == null) break;

                    if (nextLine[0].equals("Basic") && nextLine.length > 1) {
                        for (int i = 0; i < 6; i++) {
                            data.setBasicShares(i, parseLong(nextLine[i + 1]));
                        }
                    }
                    if (nextLine[0].equals("Diluted") && nextLine.length > 1) {
                        for (int i = 0; i < 6; i++) {
                            data.setDilutedShares(i, parseLong(nextLine[i + 1]));
                        }
                    }
                }
            }
        }
    }

    private static boolean isRevenueRow(String label) {
        return "Revenue".equals(label) || "Total revenues".equals(label) || "Total net revenue".equals(label);
    }

    private static long parseLong(String value) {
        return (value == null || value.isEmpty()) ? 0 : Long.parseLong(value);
    }
}
