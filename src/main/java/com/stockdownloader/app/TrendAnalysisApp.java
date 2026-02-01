package com.stockdownloader.app;

import com.stockdownloader.analysis.PatternAnalyzer;
import com.stockdownloader.data.StockListDownloader;
import com.stockdownloader.data.YahooHistoricalClient;
import com.stockdownloader.model.HistoricalData;
import com.stockdownloader.util.DateHelper;

import com.google.common.collect.HashMultimap;

import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * Main entry point for stock trend pattern analysis.
 * Downloads ticker lists, fetches historical data, and analyzes price movement patterns.
 */
public final class TrendAnalysisApp {

    private static final Logger LOGGER = Logger.getLogger(TrendAnalysisApp.class.getName());

    private TrendAnalysisApp() {}

    public static void main(String[] args) throws Exception {
        var stockList = new TreeSet<String>();

        var dates = new DateHelper();
        var sd = new StockListDownloader();
        var historicalClient = new YahooHistoricalClient();

        System.out.println("Downloading lists, please wait...");

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
            HistoricalData data = historicalClient.download(ticker);

            if (data.isIncomplete()) {
                sd.appendIncomplete(ticker);
            }

            patterns.putAll(data.getPatterns());

            if (count == 100) {
                for (String pattern : patterns.keySet()) {
                    System.out.println(pattern + "\t" + patterns.get(pattern));
                }
            }

            count++;
        }

        var results = PatternAnalyzer.analyze(patterns);
        PatternAnalyzer.printResults(results);
    }
}
