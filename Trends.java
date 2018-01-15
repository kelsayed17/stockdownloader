// Java Utilities
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;



import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Multiset.Entry;


public class Trends {

	public static void main(String[] args) throws Exception {
		TreeSet<String> stockList		= new TreeSet<String>();
		TreeSet<String> errorList		= new TreeSet<String>();

		// Program Timer
		TimeEstimates timer = new TimeEstimates();

		Dates dates = new Dates();

		System.out.println("Downloading lists, please wait...");

		StockDownloader sd = new StockDownloader();

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

		// Display Message
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

		// Begin processing
		for (String ticker : stockList) {

			// Loop start time
			timer.setLoopStartTime();

			// Multi-threading
			ExecutorService pool = Executors.newFixedThreadPool(3);

			// Yahoo Historical data
			//Future<YahooHistoricalData> yh = pool.submit(new YahooHistorical(ticker, dates));

			// Yahoo Finance data
			//Future<YahooFinanceData> yf = pool.submit(new YahooFinanceData(ticker));
			//YahooFinanceData y = 
			// Morningstar data
			//Future<MorningstarData> ms = pool.submit(new Morningstar(ticker));

			//if (yf.isIncomplete())
			//	sd.appendIncomplete(ticker);

			YahooHistoricalData yh = new YahooHistoricalData(ticker, dates);
			yh.downloadYahooHistorical(ticker);
			
			if (yh.isIncomplete())
				sd.appendIncomplete(ticker);

			patterns.putAll(yh.getPatterns());
			
			if (count == 100) {
				for (String pattern : patterns.keySet())
					System.out.println(pattern + "\t" + patterns.get(pattern));
			}

			// Loop end time
			//timer.setLoopEndTime();

			// Get time estimates
			//timer.getTimeEstimates(i, stockList.size(), dates);

			count++;
		}


		// Print stocks with errors
		System.out.println(errorList);

		int endList = stockList.size() - 1;

		// Get time estimates
		timer.getTimeEstimates(endList, stockList.size(), dates);
	}



	public static void clearConsole() {
		try {
			String os = System.getProperty("os.name");

			if (os.contains("Windows"))
				Runtime.getRuntime().exec("cls");
			else
				Runtime.getRuntime().exec("clear");
		}
		catch (Exception e) {
			for (int i = 0; i < 50; ++i)
				System.out.println();
		}
	}

	public static void writeFile(String ListStr, String filename) throws IOException {
		BufferedWriter writeFile = new BufferedWriter(new FileWriter(filename));

		writeFile.write(ListStr.replace("[", "").replace("]", ""));
		writeFile.close();
	}





	public static TreeSet<String> readFile(String filename) throws IOException {
		TreeSet<String> list = new TreeSet<String>();
		BufferedReader readFile = new BufferedReader(new FileReader(filename));

		for (String line = readFile.readLine(); line != null; line = readFile.readLine())
			Collections.addAll(list, line.split("\\s*,\\s*"));

		readFile.close();
		return list;
	}

	public void deleteFile(String filename) {
		try {
			File file = new File(filename);
			if (file.delete())
				System.out.println(file.getName() + " deleted.");
			else
				System.out.println("Delete operation failed.");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void appendToFile(String str, String filename) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true));
			bw.write(str);
			bw.newLine();
			bw.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}