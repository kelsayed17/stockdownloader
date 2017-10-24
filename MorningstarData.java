import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.URL;
import java.text.ParseException;
import java.util.concurrent.Callable;

import com.opencsv.CSVReader;

public class MorningstarData implements Callable<MorningstarData> {
	private String ticker;
	
	private long revenueQtr1;
	private long revenueQtr2;
	private long revenueQtr3;
	private long revenueQtr4;
	private long revenueQtr5;
	private long revenueTTM;
	private long basicSharesOutstandingQtr1;
	private long basicSharesOutstandingQtr2;
	private long basicSharesOutstandingQtr3;
	private long basicSharesOutstandingQtr4;
	private long basicSharesOutstandingQtr5;
	private long basicSharesOutstandingTTM;
	private long dilutedSharesOutstandingQtr1;
	private long dilutedSharesOutstandingQtr2;
	private long dilutedSharesOutstandingQtr3;
	private long dilutedSharesOutstandingQtr4;
	private long dilutedSharesOutstandingQtr5;
	private long dilutedSharesOutstandingTTM;
	private BigDecimal revenuePerShareQtr1;
	private BigDecimal revenuePerShareQtr2;
	private BigDecimal revenuePerShareQtr3;
	private BigDecimal revenuePerShareQtr4;
	private BigDecimal revenuePerShareQtr5;
	private BigDecimal revenuePerShareTTM;
	private BigDecimal revenuePerShareTTMLastQtr;
	private String fiscalQtr1;
	private String fiscalQtr2;
	private String fiscalQtr3;
	private String fiscalQtr4;
	private String fiscalQtrPrevious;
	private String fiscalQtrCurrent;
	
	private boolean incomplete;
	private boolean error;
	
	public MorningstarData(String ticker) {
		revenueQtr1 = 0;
		revenueQtr2 = 0;
		revenueQtr3 = 0;
		revenueQtr4 = 0;
		revenueQtr5 = 0;
		revenueTTM = 0;
		basicSharesOutstandingQtr1 = 0;
		basicSharesOutstandingQtr2 = 0;
		basicSharesOutstandingQtr3 = 0;
		basicSharesOutstandingQtr4 = 0;
		basicSharesOutstandingQtr5 = 0;
		basicSharesOutstandingTTM = 0;
		dilutedSharesOutstandingQtr1 = 0;
		dilutedSharesOutstandingQtr2 = 0;
		dilutedSharesOutstandingQtr3 = 0;
		dilutedSharesOutstandingQtr4 = 0;
		dilutedSharesOutstandingQtr5 = 0;
		dilutedSharesOutstandingTTM = 0;
		revenuePerShareQtr1 = new BigDecimal("0.00");
		revenuePerShareQtr2 = new BigDecimal("0.00");
		revenuePerShareQtr3 = new BigDecimal("0.00");
		revenuePerShareQtr4 = new BigDecimal("0.00");
		revenuePerShareQtr5 = new BigDecimal("0.00");
		revenuePerShareTTM = new BigDecimal("0.00");
		revenuePerShareTTMLastQtr = new BigDecimal("0.00");
		fiscalQtr1 = "";
		fiscalQtr2 = "";
		fiscalQtr3 = "";
		fiscalQtr4 = "";
		fiscalQtrPrevious = "";
		fiscalQtrCurrent = "";
		
		incomplete = false;
		error = false;
		
		this.ticker = ticker;
	}
	
	public void downloadMorningstar(String ticker) {
		String url = "http://financials.morningstar.com/ajax/ReportProcess4CSV.html?&t=" + ticker + "&reportType=is&period=3&dataType=A&order=asc&columnYear=5&rounding=1&view=raw&r=785679&denominatorView=raw&number=1";

		try {
			InputStream input = new URL(url).openStream();
			saveData(input);
		}
		catch (ArithmeticException a) {
			System.out.println(ticker + " has incomplete data after processing Morningstar data.");
			incomplete = true;
		}
		catch (NullPointerException n) {
			System.out.println(ticker + " has incomplete data after processing Morningstar data.");
			incomplete = true;
		}
		catch (Exception e) {
			e.printStackTrace();
			downloadMorningstar(ticker);
		}
	}
	
	private void saveData(InputStream input) throws IOException, ParseException {
		MathContext mc = new MathContext(2);
		CSVReader reader = new CSVReader(new InputStreamReader(input));

		for (String[] nextLine = reader.readNext(); nextLine != null; nextLine = reader.readNext()) {

			if (nextLine[0].contains("Fiscal year ends in")) {
				fiscalQtr1 = nextLine[1] + "-28";
				fiscalQtr2 = nextLine[2] + "-28";
				fiscalQtr3 = nextLine[3] + "-28";
				fiscalQtrPrevious = nextLine[4] + "-28";
				fiscalQtrCurrent = nextLine[5] + "-28";
			}
			if (nextLine[0].equals("Revenue") && nextLine.length > 1) {
				revenueQtr1 = Long.parseLong((nextLine[1].length() == 0) ? "0" : nextLine[1]);
				revenueQtr2 = Long.parseLong((nextLine[2].length() == 0) ? "0" : nextLine[2]);
				revenueQtr3 = Long.parseLong((nextLine[3].length() == 0) ? "0" : nextLine[3]);
				revenueQtr4 = Long.parseLong((nextLine[4].length() == 0) ? "0" : nextLine[4]);
				revenueQtr5 = Long.parseLong((nextLine[5].length() == 0) ? "0" : nextLine[5]);
				revenueTTM = Long.parseLong((nextLine[6].length() == 0) ? "0" : nextLine[6]);
			}
			if (nextLine[0].equals("Total revenues") && nextLine.length > 1) {
				revenueQtr1 = Long.parseLong(nextLine[1]);
				revenueQtr2 = Long.parseLong(nextLine[2]);
				revenueQtr3 = Long.parseLong(nextLine[3]);
				revenueQtr4 = Long.parseLong(nextLine[4]);
				revenueQtr5 = Long.parseLong(nextLine[5]);
				revenueTTM = Long.parseLong(nextLine[6]);
			}
			if (nextLine[0].equals("Total net revenue") && nextLine.length > 1) {
				revenueQtr1 = Long.parseLong(nextLine[1]);
				revenueQtr2 = Long.parseLong(nextLine[2]);
				revenueQtr3 = Long.parseLong(nextLine[3]);
				revenueQtr4 = Long.parseLong(nextLine[4]);
				revenueQtr5 = Long.parseLong(nextLine[5]);
				revenueTTM = Long.parseLong(nextLine[6]);
			}

			if (nextLine[0].equals("Weighted average shares outstanding")) {
				nextLine = reader.readNext();

				if (nextLine[0].equals("Basic") && nextLine.length > 1) {
					basicSharesOutstandingQtr1 = Long.parseLong((nextLine[1].length() == 0) ? "0" : nextLine[1]);
					basicSharesOutstandingQtr2 = Long.parseLong((nextLine[2].length() == 0) ? "0" : nextLine[2]);
					basicSharesOutstandingQtr3 = Long.parseLong((nextLine[3].length() == 0) ? "0" : nextLine[3]);
					basicSharesOutstandingQtr4 = Long.parseLong((nextLine[4].length() == 0) ? "0" : nextLine[4]);
					basicSharesOutstandingQtr5 = Long.parseLong((nextLine[5].length() == 0) ? "0" : nextLine[5]);
					basicSharesOutstandingTTM = Long.parseLong((nextLine[6].length() == 0) ? "0" : nextLine[6]);
				}
				if (nextLine[0].equals("Diluted") && nextLine.length > 1) {
					dilutedSharesOutstandingQtr1 = Long.parseLong(nextLine[1]);
					dilutedSharesOutstandingQtr2 = Long.parseLong(nextLine[2]);
					dilutedSharesOutstandingQtr3 = Long.parseLong(nextLine[3]);
					dilutedSharesOutstandingQtr4 = Long.parseLong(nextLine[4]);
					dilutedSharesOutstandingQtr5 = Long.parseLong(nextLine[5]);
					dilutedSharesOutstandingTTM = Long.parseLong(nextLine[6]);
				}
			}
		}

		reader.close();

		/*
		// Check consistency of TTM values (Long)
		if (revenueTTM != (revenueQtr5 + revenueQtr4 + revenueQtr3 + revenueQtr2))
			revenueTTM = revenueQtr5 + revenueQtr4 + revenueQtr3 + revenueQtr2;
		if (basicSharesOutstandingTTM != (basicSharesOutstandingQtr5 + basicSharesOutstandingQtr4 + basicSharesOutstandingQtr3 + basicSharesOutstandingQtr2))
			basicSharesOutstandingTTM = basicSharesOutstandingQtr5 + basicSharesOutstandingQtr4 + basicSharesOutstandingQtr3 + basicSharesOutstandingQtr2;
		if (dilutedSharesOutstandingTTM != (dilutedSharesOutstandingQtr5 + dilutedSharesOutstandingQtr4 + dilutedSharesOutstandingQtr3 + dilutedSharesOutstandingQtr2))
			dilutedSharesOutstandingTTM = dilutedSharesOutstandingQtr5 + dilutedSharesOutstandingQtr4 + dilutedSharesOutstandingQtr3 + dilutedSharesOutstandingQtr2;
		 */

		if (dilutedSharesOutstandingQtr1 == 0)
			dilutedSharesOutstandingQtr1 = basicSharesOutstandingQtr1;
		if (dilutedSharesOutstandingQtr2 == 0)
			dilutedSharesOutstandingQtr2 = basicSharesOutstandingQtr2;
		if (dilutedSharesOutstandingQtr3 == 0)
			dilutedSharesOutstandingQtr3 = basicSharesOutstandingQtr3;
		if (dilutedSharesOutstandingQtr4 == 0)
			dilutedSharesOutstandingQtr4 = basicSharesOutstandingQtr4;
		if (dilutedSharesOutstandingQtr5 == 0)
			dilutedSharesOutstandingQtr5 = basicSharesOutstandingQtr5;
		if (dilutedSharesOutstandingTTM == 0)
			dilutedSharesOutstandingTTM = basicSharesOutstandingTTM;

		// Revenue Per Share Data (BigDecimal)
		revenuePerShareQtr1 = BigDecimal.valueOf(revenueQtr1).divide(BigDecimal.valueOf(dilutedSharesOutstandingQtr1), 2, RoundingMode.CEILING);
		revenuePerShareQtr2 = BigDecimal.valueOf(revenueQtr2).divide(BigDecimal.valueOf(dilutedSharesOutstandingQtr2), 2, RoundingMode.CEILING);
		revenuePerShareQtr3 = BigDecimal.valueOf(revenueQtr3).divide(BigDecimal.valueOf(dilutedSharesOutstandingQtr3), 2, RoundingMode.CEILING);
		revenuePerShareQtr4 = BigDecimal.valueOf(revenueQtr4).divide(BigDecimal.valueOf(dilutedSharesOutstandingQtr4), 2, RoundingMode.CEILING);
		revenuePerShareQtr5 = BigDecimal.valueOf(revenueQtr5).divide(BigDecimal.valueOf(dilutedSharesOutstandingQtr5), 2, RoundingMode.CEILING);
		revenuePerShareTTM = BigDecimal.valueOf(revenueTTM).divide(BigDecimal.valueOf(dilutedSharesOutstandingTTM), 2, RoundingMode.CEILING);
		revenuePerShareTTMLastQtr = revenuePerShareQtr1.add(revenuePerShareQtr2).add(revenuePerShareQtr3).add(revenuePerShareQtr4, mc);
	}

	public long getRevenueQtr1() {
		return revenueQtr1;
	}

	public void setRevenueQtr1(long revenueQtr1) {
		this.revenueQtr1 = revenueQtr1;
	}

	public long getRevenueQtr2() {
		return revenueQtr2;
	}

	public void setRevenueQtr2(long revenueQtr2) {
		this.revenueQtr2 = revenueQtr2;
	}

	public long getRevenueQtr3() {
		return revenueQtr3;
	}

	public void setRevenueQtr3(long revenueQtr3) {
		this.revenueQtr3 = revenueQtr3;
	}

	public long getRevenueQtr4() {
		return revenueQtr4;
	}

	public void setRevenueQtr4(long revenueQtr4) {
		this.revenueQtr4 = revenueQtr4;
	}

	public long getRevenueQtr5() {
		return revenueQtr5;
	}

	public void setRevenueQtr5(long revenueQtr5) {
		this.revenueQtr5 = revenueQtr5;
	}

	public long getRevenueTTM() {
		return revenueTTM;
	}

	public void setRevenueTTM(long revenueTTM) {
		this.revenueTTM = revenueTTM;
	}

	public long getBasicSharesOutstandingQtr1() {
		return basicSharesOutstandingQtr1;
	}

	public void setBasicSharesOutstandingQtr1(long basicSharesOutstandingQtr1) {
		this.basicSharesOutstandingQtr1 = basicSharesOutstandingQtr1;
	}

	public long getBasicSharesOutstandingQtr2() {
		return basicSharesOutstandingQtr2;
	}

	public void setBasicSharesOutstandingQtr2(long basicSharesOutstandingQtr2) {
		this.basicSharesOutstandingQtr2 = basicSharesOutstandingQtr2;
	}

	public long getBasicSharesOutstandingQtr3() {
		return basicSharesOutstandingQtr3;
	}

	public void setBasicSharesOutstandingQtr3(long basicSharesOutstandingQtr3) {
		this.basicSharesOutstandingQtr3 = basicSharesOutstandingQtr3;
	}

	public long getBasicSharesOutstandingQtr4() {
		return basicSharesOutstandingQtr4;
	}

	public void setBasicSharesOutstandingQtr4(long basicSharesOutstandingQtr4) {
		this.basicSharesOutstandingQtr4 = basicSharesOutstandingQtr4;
	}

	public long getBasicSharesOutstandingQtr5() {
		return basicSharesOutstandingQtr5;
	}

	public void setBasicSharesOutstandingQtr5(long basicSharesOutstandingQtr5) {
		this.basicSharesOutstandingQtr5 = basicSharesOutstandingQtr5;
	}

	public long getBasicSharesOutstandingTTM() {
		return basicSharesOutstandingTTM;
	}

	public void setBasicSharesOutstandingTTM(long basicSharesOutstandingTTM) {
		this.basicSharesOutstandingTTM = basicSharesOutstandingTTM;
	}

	public long getDilutedSharesOutstandingQtr1() {
		return dilutedSharesOutstandingQtr1;
	}

	public void setDilutedSharesOutstandingQtr1(long dilutedSharesOutstandingQtr1) {
		this.dilutedSharesOutstandingQtr1 = dilutedSharesOutstandingQtr1;
	}

	public long getDilutedSharesOutstandingQtr2() {
		return dilutedSharesOutstandingQtr2;
	}

	public void setDilutedSharesOutstandingQtr2(long dilutedSharesOutstandingQtr2) {
		this.dilutedSharesOutstandingQtr2 = dilutedSharesOutstandingQtr2;
	}

	public long getDilutedSharesOutstandingQtr3() {
		return dilutedSharesOutstandingQtr3;
	}

	public void setDilutedSharesOutstandingQtr3(long dilutedSharesOutstandingQtr3) {
		this.dilutedSharesOutstandingQtr3 = dilutedSharesOutstandingQtr3;
	}

	public long getDilutedSharesOutstandingQtr4() {
		return dilutedSharesOutstandingQtr4;
	}

	public void setDilutedSharesOutstandingQtr4(long dilutedSharesOutstandingQtr4) {
		this.dilutedSharesOutstandingQtr4 = dilutedSharesOutstandingQtr4;
	}

	public long getDilutedSharesOutstandingQtr5() {
		return dilutedSharesOutstandingQtr5;
	}

	public void setDilutedSharesOutstandingQtr5(long dilutedSharesOutstandingQtr5) {
		this.dilutedSharesOutstandingQtr5 = dilutedSharesOutstandingQtr5;
	}

	public long getDilutedSharesOutstandingTTM() {
		return dilutedSharesOutstandingTTM;
	}

	public void setDilutedSharesOutstandingTTM(long dilutedSharesOutstandingTTM) {
		this.dilutedSharesOutstandingTTM = dilutedSharesOutstandingTTM;
	}

	public BigDecimal getRevenuePerShareQtr1() {
		return revenuePerShareQtr1;
	}

	public void setRevenuePerShareQtr1(BigDecimal revenuePerShareQtr1) {
		this.revenuePerShareQtr1 = revenuePerShareQtr1;
	}

	public BigDecimal getRevenuePerShareQtr2() {
		return revenuePerShareQtr2;
	}

	public void setRevenuePerShareQtr2(BigDecimal revenuePerShareQtr2) {
		this.revenuePerShareQtr2 = revenuePerShareQtr2;
	}

	public BigDecimal getRevenuePerShareQtr3() {
		return revenuePerShareQtr3;
	}

	public void setRevenuePerShareQtr3(BigDecimal revenuePerShareQtr3) {
		this.revenuePerShareQtr3 = revenuePerShareQtr3;
	}

	public BigDecimal getRevenuePerShareQtr4() {
		return revenuePerShareQtr4;
	}

	public void setRevenuePerShareQtr4(BigDecimal revenuePerShareQtr4) {
		this.revenuePerShareQtr4 = revenuePerShareQtr4;
	}

	public BigDecimal getRevenuePerShareQtr5() {
		return revenuePerShareQtr5;
	}

	public void setRevenuePerShareQtr5(BigDecimal revenuePerShareQtr5) {
		this.revenuePerShareQtr5 = revenuePerShareQtr5;
	}

	public BigDecimal getRevenuePerShareTTM() {
		return revenuePerShareTTM;
	}

	public void setRevenuePerShareTTM(BigDecimal revenuePerShareTTM) {
		this.revenuePerShareTTM = revenuePerShareTTM;
	}

	public BigDecimal getRevenuePerShareTTMLastQtr() {
		return revenuePerShareTTMLastQtr;
	}

	public void setRevenuePerShareTTMLastQtr(BigDecimal revenuePerShareTTMLastQtr) {
		this.revenuePerShareTTMLastQtr = revenuePerShareTTMLastQtr;
	}

	public String getFiscalQtr1() {
		return fiscalQtr1;
	}

	public void setFiscalQtr1(String fiscalQtr1) {
		this.fiscalQtr1 = fiscalQtr1;
	}

	public String getFiscalQtr2() {
		return fiscalQtr2;
	}

	public void setFiscalQtr2(String fiscalQtr2) {
		this.fiscalQtr2 = fiscalQtr2;
	}

	public String getFiscalQtr3() {
		return fiscalQtr3;
	}

	public void setFiscalQtr3(String fiscalQtr3) {
		this.fiscalQtr3 = fiscalQtr3;
	}

	public String getFiscalQtr4() {
		return fiscalQtr4;
	}

	public void setFiscalQtr4(String fiscalQtr4) {
		this.fiscalQtr4 = fiscalQtr4;
	}

	public String getFiscalQtrPrevious() {
		return fiscalQtrPrevious;
	}

	public void setFiscalQtrPrevious(String fiscalQtrPrevious) {
		this.fiscalQtrPrevious = fiscalQtrPrevious;
	}

	public String getFiscalQtrCurrent() {
		return fiscalQtrCurrent;
	}

	public void setFiscalQtrCurrent(String fiscalQtrCurrent) {
		this.fiscalQtrCurrent = fiscalQtrCurrent;
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
	public MorningstarData call() throws Exception {
		downloadMorningstar(ticker);
		return this;
	}
}