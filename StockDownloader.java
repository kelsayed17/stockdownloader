import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.TreeSet;

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

	private TreeSet<String> nasdaqList;
	private TreeSet<String> othersList;
	private TreeSet<String> mfundsList;
	private TreeSet<String> zacksList;
	private TreeSet<String> earningsList;
	private TreeSet<String> incompleteList;

	public StockDownloader() {
		nasdaqList		= new TreeSet<String>();
		othersList		= new TreeSet<String>();
		mfundsList		= new TreeSet<String>();
		zacksList		= new TreeSet<String>();
		earningsList		= new TreeSet<String>();
		incompleteList	= new TreeSet<String>();
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

		try {
			CSVParser parser = new CSVParserBuilder().withSeparator('\t').build();
			CSVReader reader = new CSVReaderBuilder(new FileReader(file)).withSkipLines(2).withCSVParser(parser).build();

			String [] nextLine;
			while ((nextLine = reader.readNext()) != null)
				zacksList.add(nextLine[0]);

			reader.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void downloadYahooEarnings(Calendar calendar) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String date = dateFormat.format(calendar.getTime());
		String url = "https://finance.yahoo.com/calendar/earnings?day=" + date;
		ListMultimap<String, String> YahooEarnings = ArrayListMultimap.create();

		try {
			Document doc = Jsoup.connect(url).get();
			Element table = doc.select("#fin-cal-table > div:eq(2) > table > tbody").first();

			for (Element tr : table.children()) {
				String ticker = tr.child(1).text();
				String time = tr.child(3).text();
				YahooEarnings.put(time, ticker);
			}

			// Create separate list
			for (String time : YahooEarnings.keySet()) {
				List<String> list = YahooEarnings.get(time);
				for (String ticker : list)
					earningsList.add(ticker);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
			downloadYahooEarnings(calendar);
		}
		
		for (String time : YahooEarnings.keySet()) {
			System.out.println(date + "\t" + time + ":\t" + YahooEarnings.get(time));
		}

		return;
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
		try {
			URL file_url = new URL(url);
			File file = new File(filename);
			FileUtils.copyURLToFile(file_url, file);
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
			downloadFile(url, filename);
		}
		catch (IOException e) {
			e.printStackTrace();
			downloadFile(url, filename);
		}
	}

	private void writeFile(TreeSet<String> list, String filename) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(filename));

			for (String ticker : list)
				bw.write(ticker);

			bw.close();
		}
		catch (IOException e) {
			e.printStackTrace();
			writeFile(list, filename);
		}
	}

	private TreeSet<String> readFile(String filename) {
		TreeSet<String> list = new TreeSet<String>();

		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));

			for (String line = br.readLine(); line != null; line = br.readLine())
				list.add(line);

			br.close();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
			readFile(filename);
		}

		return list;
	}

	public void deleteFile(String filename) {
		File file = new File(filename);
		if (file.delete())
			System.out.println(file.getName() + " deleted.");
		else
			System.out.println("Delete operation failed.");
	}

	private void appendToFile(String str, String filename) {
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

	private static TreeSet<String> readDownloadedFile(String filename) {
		TreeSet<String> list = new TreeSet<String>();

		try {
			BufferedReader reader = new BufferedReader(new FileReader(filename));

			// Skip first line
			reader.readLine();

			String current = reader.readLine();
			String next = reader.readLine();

			while (current != null) {

				// Skip last line
				if (next == null)
					break;

				String ticker = current.substring(0, current.indexOf('|'));
				list.add(ticker);
				current = next;
				next = reader.readLine();
			}

			reader.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return list;
	}

	public TreeSet<String> getNasdaqList() {
		return nasdaqList;
	}

	public void setNasdaqList(TreeSet<String> nasdaqList) {
		this.nasdaqList = nasdaqList;
	}

	public TreeSet<String> getOthersList() {
		return othersList;
	}

	public void setOthersList(TreeSet<String> othersList) {
		this.othersList = othersList;
	}

	public TreeSet<String> getMfundsList() {
		return mfundsList;
	}

	public void setMfundsList(TreeSet<String> mfundsList) {
		this.mfundsList = mfundsList;
	}

	public TreeSet<String> getZacksList() {
		return zacksList;
	}

	public void setZacksList(TreeSet<String> zacksList) {
		this.zacksList = zacksList;
	}

	public TreeSet<String> getEarningsList() {
		return earningsList;
	}

	public void setEarningsList(TreeSet<String> earningsList) {
		this.earningsList = earningsList;
	}

	public TreeSet<String> getIncompleteList() {
		return incompleteList;
	}

	public void setIncompleteList(TreeSet<String> incompleteList) {
		this.incompleteList = incompleteList;
	}
}
