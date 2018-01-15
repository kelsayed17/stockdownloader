import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import com.google.common.collect.HashMultimap;
import com.opencsv.CSVReader;

public class YahooHistoricalData implements Callable<YahooHistoricalData> {
    private String ticker;
    private Dates dates;

    private BigDecimal highestPriceThisQtr = new BigDecimal("0.00");
    private BigDecimal lowestPriceThisQtr = new BigDecimal("0.00");
    private BigDecimal highestPriceLastQtr = new BigDecimal("0.00");
    private BigDecimal lowestPriceLastQtr = new BigDecimal("0.00");

    private String highestCloseDateThisQtrStr = "";
    private String lowestCloseDateThisQtrStr = "";
    private String highestCloseDateLastQtrStr = "";
    private String lowestCloseDateLastQtrStr = "";

    private ArrayList<String> historicalPrices = new ArrayList<String>();
    private HashMultimap<String, String> patterns = HashMultimap.create();

    private boolean incomplete = false;
    private boolean error = false;

    public YahooHistoricalData(String ticker, Dates dates) {
        this.ticker = ticker;
        this.dates = dates;
    }

    public void downloadYahooHistorical(String ticker) {
        //String url = "https://query1.finance.yahoo.com/v7/finance/download/" + ticker + "?period1=" + dates.getFromDate().getTimeInMillis() / 1000 + "&period2=" + dates.getToDate().getTimeInMillis() / 1000 + "&interval=1d&events=history&crumb=DO2xTZ0ANto";
        String url = "http://www.google.com/finance/historical?q=" + ticker + "&output=csv";

        try {
            InputStream input = new URL(url).openStream();
            saveData(input);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            incomplete = true;
        } catch (IOException e) {
            e.printStackTrace();
            incomplete = true;
        } catch (Exception e) {
            e.printStackTrace();
            downloadYahooHistorical(ticker);
        }
    }

    private void saveData(InputStream input) throws IOException, ParseException {
        MathContext mc = new MathContext(2);
        CSVReader reader = new CSVReader(new InputStreamReader(input));

        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        DateFormat yahooFormat = new SimpleDateFormat("yyyy-MM-dd");

        Calendar FiscalQtrCurrent = dates.getFiscalQtrCurrent();
        String FiscalQtrCurrentStr = dateFormat.format(FiscalQtrCurrent.getTime());

        List<Integer> UpDownList = new ArrayList<Integer>();
        TreeMap<BigDecimal, String> thisQtr = new TreeMap<BigDecimal, String>();
        TreeMap<BigDecimal, String> lastQtr = new TreeMap<BigDecimal, String>();

        int i = 0;

        BigDecimal NextClosePrice = new BigDecimal("0.00");

        // Skip first line
        String[] nextLine = reader.readNext();

        while ((nextLine = reader.readNext()) != null) {

            String CloseDateStr = nextLine[0];
            BigDecimal ClosePrice = new BigDecimal(nextLine[4]);

            // Close date
            //Calendar CloseDate = Calendar.getInstance();
            //CloseDate.setTime(yahooFormat.parse(CloseDateStr));
            //CloseDateStr = dateFormat.format(CloseDate.getTime());

            //if (CloseDate.equals(FiscalQtrCurrent) || CloseDate.after(FiscalQtrCurrent))
            //	thisQtr.put(ClosePrice, CloseDateStr);
            //else if (CloseDate.before(FiscalQtrCurrent))
            //	lastQtr.put(ClosePrice, CloseDateStr);

            if (i > 0) {
                BigDecimal CloseChange = ClosePrice.subtract(NextClosePrice).divide(NextClosePrice, 10, RoundingMode.CEILING).multiply(new BigDecimal(100), mc);

                if (CloseChange.compareTo(BigDecimal.valueOf(0.00)) == 1)
                    UpDownList.add(1);
                else if (CloseChange.compareTo(BigDecimal.valueOf(0.00)) == -1)
                    UpDownList.add(-1);
                else
                    UpDownList.add(0);

                String pattern = UpDownList.toString();

                patterns.put(pattern, ticker);
            }

            NextClosePrice = ClosePrice;

            if (i == 7)
                break;

            i++;
        }
        reader.close();

		/*

		// Check if historical close prices this quarter exist
		if (thisQtr.size() == 0) {
			FiscalQtrCurrent.add(Calendar.DAY_OF_MONTH, 1);
			thisQtr.put(new BigDecimal("0.00"), FiscalQtrCurrentStr);
			thisQtr.put(new BigDecimal("0.00"), FiscalQtrCurrentStr);
			thisQtr.put(new BigDecimal("0.00"), FiscalQtrCurrentStr);
			FiscalQtrCurrent.add(Calendar.DAY_OF_MONTH, -1);
		}

		// Check if historical close prices last quarter exist
		if (lastQtr.size() == 0) {
			FiscalQtrCurrent.add(Calendar.DAY_OF_MONTH, 1);
			lastQtr.put(new BigDecimal("0.00"), FiscalQtrCurrentStr);
			lastQtr.put(new BigDecimal("0.00"), FiscalQtrCurrentStr);
			lastQtr.put(new BigDecimal("0.00"), FiscalQtrCurrentStr);
			FiscalQtrCurrent.add(Calendar.DAY_OF_MONTH, -1);
		}

		// Highest and lowest entries for this quarter and last quarter
		Entry<BigDecimal, String> highestEntryThisQtr = thisQtr.lastEntry();
		Entry<BigDecimal, String> lowestEntryThisQtr = thisQtr.firstEntry();
		Entry<BigDecimal, String> highestEntryLastQtr = lastQtr.lastEntry();
		Entry<BigDecimal, String> lowestEntrylastQtr = lastQtr.firstEntry();

		// Highest and lowest prices for this quarter and last quarter
		highestPriceThisQtr = highestEntryThisQtr.getKey();
		lowestPriceThisQtr = lowestEntryThisQtr.getKey();
		highestPriceLastQtr = highestEntryLastQtr.getKey();
		lowestPriceLastQtr = lowestEntrylastQtr.getKey();

		// Get Calendar Dates
		Calendar HighestCloseDateThisQtr = Calendar.getInstance();
		Calendar LowestCloseDateThisQtr = Calendar.getInstance();
		Calendar HighestCloseDateLastQtr = Calendar.getInstance();
		Calendar LowestCloseDateLastQtr = Calendar.getInstance();

		// Highest and lowest close dates for this quarter and last quarter (Calendar)
		HighestCloseDateThisQtr.setTime(dateFormat.parse(highestEntryThisQtr.getValue()));
		LowestCloseDateThisQtr.setTime(dateFormat.parse(lowestEntryThisQtr.getValue()));
		HighestCloseDateLastQtr.setTime(dateFormat.parse(highestEntryLastQtr.getValue()));
		LowestCloseDateLastQtr.setTime(dateFormat.parse(lowestEntrylastQtr.getValue()));

		// Highest and lowest close dates for this quarter and last quarter (String)
		highestCloseDateThisQtrStr = dateFormat.format(HighestCloseDateThisQtr.getTime());
		lowestCloseDateThisQtrStr = dateFormat.format(LowestCloseDateThisQtr.getTime());
		highestCloseDateLastQtrStr = dateFormat.format(HighestCloseDateLastQtr.getTime());
		lowestCloseDateLastQtrStr = dateFormat.format(LowestCloseDateLastQtr.getTime());

		//if (LatestPriceSales.compareTo(MinPSRatioThisQtr) == -1)
		//priceAtMinPSRatioThisQtr = new BigDecimal(bd.SetScaleTwo(bd.BDMultiply(LatestPriceSales, RevenuePerShareTTM)).toString());

		// Set patterns data
		//yh.setPatterns(patterns);
		
		*/
    }

    public BigDecimal getHighestPriceThisQtr() {
        return highestPriceThisQtr;
    }

    public void setHighestPriceThisQtr(BigDecimal highestPriceThisQtr) {
        this.highestPriceThisQtr = highestPriceThisQtr;
    }

    public BigDecimal getLowestPriceThisQtr() {
        return lowestPriceThisQtr;
    }

    public void setLowestPriceThisQtr(BigDecimal lowestPriceThisQtr) {
        this.lowestPriceThisQtr = lowestPriceThisQtr;
    }

    public BigDecimal getHighestPriceLastQtr() {
        return highestPriceLastQtr;
    }

    public void setHighestPriceLastQtr(BigDecimal highestPriceLastQtr) {
        this.highestPriceLastQtr = highestPriceLastQtr;
    }

    public BigDecimal getLowestPriceLastQtr() {
        return lowestPriceLastQtr;
    }

    public void setLowestPriceLastQtr(BigDecimal lowestPriceLastQtr) {
        this.lowestPriceLastQtr = lowestPriceLastQtr;
    }

    public String getHighestCloseDateThisQtrStr() {
        return highestCloseDateThisQtrStr;
    }

    public void setHighestCloseDateThisQtrStr(String highestCloseDateThisQtrStr) {
        this.highestCloseDateThisQtrStr = highestCloseDateThisQtrStr;
    }

    public String getLowestCloseDateThisQtrStr() {
        return lowestCloseDateThisQtrStr;
    }

    public void setLowestCloseDateThisQtrStr(String lowestCloseDateThisQtrStr) {
        this.lowestCloseDateThisQtrStr = lowestCloseDateThisQtrStr;
    }

    public String getHighestCloseDateLastQtrStr() {
        return highestCloseDateLastQtrStr;
    }

    public void setHighestCloseDateLastQtrStr(String highestCloseDateLastQtrStr) {
        this.highestCloseDateLastQtrStr = highestCloseDateLastQtrStr;
    }

    public String getLowestCloseDateLastQtrStr() {
        return lowestCloseDateLastQtrStr;
    }

    public void setLowestCloseDateLastQtrStr(String lowestCloseDateLastQtrStr) {
        this.lowestCloseDateLastQtrStr = lowestCloseDateLastQtrStr;
    }

    public ArrayList<String> getHistorcalPrices() {
        return historicalPrices;
    }

    public void setHistorcalPrices(ArrayList<String> historcalPrices) {
        this.historicalPrices = historcalPrices;
    }

    public HashMultimap<String, String> getPatterns() {
        return patterns;
    }

    public void setPatterns(HashMultimap<String, String> patterns) {
        this.patterns = patterns;
    }

    public boolean isIncomplete() {
        return incomplete;
    }

    public void setIncomplete(boolean incomplete) {
        this.incomplete = incomplete;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    @Override
    public YahooHistoricalData call() throws Exception {
        downloadYahooHistorical(ticker);
        return this;
    }
}