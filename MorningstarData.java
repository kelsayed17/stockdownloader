import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.opencsv.CSVReader;

public class MorningstarData implements Callable<MorningstarData> {

    private static final Logger LOGGER = Logger.getLogger(MorningstarData.class.getName());
    private static final int MAX_RETRIES = 3;

    private final String ticker;

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
    private BigDecimal revenuePerShareQtr1 = BigDecimal.ZERO;
    private BigDecimal revenuePerShareQtr2 = BigDecimal.ZERO;
    private BigDecimal revenuePerShareQtr3 = BigDecimal.ZERO;
    private BigDecimal revenuePerShareQtr4 = BigDecimal.ZERO;
    private BigDecimal revenuePerShareQtr5 = BigDecimal.ZERO;
    private BigDecimal revenuePerShareTTM = BigDecimal.ZERO;
    private BigDecimal revenuePerShareTTMLastQtr = BigDecimal.ZERO;
    private String fiscalQtr1 = "";
    private String fiscalQtr2 = "";
    private String fiscalQtr3 = "";
    private String fiscalQtr4 = "";
    private String fiscalQtrPrevious = "";
    private String fiscalQtrCurrent = "";

    private boolean incomplete;
    private boolean error;

    public MorningstarData(String ticker) {
        this.ticker = ticker;
    }

    public void downloadMorningstar(String ticker) {
        downloadMorningstar(ticker, 0);
    }

    private void downloadMorningstar(String ticker, int retryCount) {
        String url = "http://financials.morningstar.com/ajax/ReportProcess4CSV.html?&t=" + ticker
                + "&reportType=is&period=3&dataType=A&order=asc&columnYear=5&rounding=1&view=raw&r=785679&denominatorView=raw&number=1";

        try (InputStream input = URI.create(url).toURL().openStream()) {
            saveData(input);
        } catch (ArithmeticException | NullPointerException e) {
            LOGGER.log(Level.WARNING, "{0} has incomplete data after processing Morningstar data.", ticker);
            incomplete = true;
        } catch (Exception e) {
            if (retryCount < MAX_RETRIES) {
                LOGGER.log(Level.FINE, "Retrying Morningstar download for {0}, attempt {1}",
                        new Object[]{ticker, retryCount + 1});
                downloadMorningstar(ticker, retryCount + 1);
            } else {
                LOGGER.log(Level.WARNING, "Failed to download Morningstar data for {0}", ticker);
                error = true;
            }
        }
    }

    private void saveData(InputStream input) throws IOException {
        var mc = new MathContext(2);

        try (var reader = new CSVReader(new InputStreamReader(input))) {
            for (String[] nextLine = reader.readNext(); nextLine != null; nextLine = reader.readNext()) {

                if (nextLine[0].contains("Fiscal year ends in")) {
                    fiscalQtr1 = nextLine[1] + "-28";
                    fiscalQtr2 = nextLine[2] + "-28";
                    fiscalQtr3 = nextLine[3] + "-28";
                    fiscalQtrPrevious = nextLine[4] + "-28";
                    fiscalQtrCurrent = nextLine[5] + "-28";
                }

                if (isRevenueRow(nextLine[0]) && nextLine.length > 1) {
                    revenueQtr1 = parseLong(nextLine[1]);
                    revenueQtr2 = parseLong(nextLine[2]);
                    revenueQtr3 = parseLong(nextLine[3]);
                    revenueQtr4 = parseLong(nextLine[4]);
                    revenueQtr5 = parseLong(nextLine[5]);
                    revenueTTM = parseLong(nextLine[6]);
                }

                if (nextLine[0].equals("Weighted average shares outstanding")) {
                    nextLine = reader.readNext();
                    if (nextLine == null) break;

                    if (nextLine[0].equals("Basic") && nextLine.length > 1) {
                        basicSharesOutstandingQtr1 = parseLong(nextLine[1]);
                        basicSharesOutstandingQtr2 = parseLong(nextLine[2]);
                        basicSharesOutstandingQtr3 = parseLong(nextLine[3]);
                        basicSharesOutstandingQtr4 = parseLong(nextLine[4]);
                        basicSharesOutstandingQtr5 = parseLong(nextLine[5]);
                        basicSharesOutstandingTTM = parseLong(nextLine[6]);
                    }
                    if (nextLine[0].equals("Diluted") && nextLine.length > 1) {
                        dilutedSharesOutstandingQtr1 = parseLong(nextLine[1]);
                        dilutedSharesOutstandingQtr2 = parseLong(nextLine[2]);
                        dilutedSharesOutstandingQtr3 = parseLong(nextLine[3]);
                        dilutedSharesOutstandingQtr4 = parseLong(nextLine[4]);
                        dilutedSharesOutstandingQtr5 = parseLong(nextLine[5]);
                        dilutedSharesOutstandingTTM = parseLong(nextLine[6]);
                    }
                }
            }
        }

        // Fall back to basic shares if diluted not available
        if (dilutedSharesOutstandingQtr1 == 0) dilutedSharesOutstandingQtr1 = basicSharesOutstandingQtr1;
        if (dilutedSharesOutstandingQtr2 == 0) dilutedSharesOutstandingQtr2 = basicSharesOutstandingQtr2;
        if (dilutedSharesOutstandingQtr3 == 0) dilutedSharesOutstandingQtr3 = basicSharesOutstandingQtr3;
        if (dilutedSharesOutstandingQtr4 == 0) dilutedSharesOutstandingQtr4 = basicSharesOutstandingQtr4;
        if (dilutedSharesOutstandingQtr5 == 0) dilutedSharesOutstandingQtr5 = basicSharesOutstandingQtr5;
        if (dilutedSharesOutstandingTTM == 0) dilutedSharesOutstandingTTM = basicSharesOutstandingTTM;

        // Revenue Per Share Data
        revenuePerShareQtr1 = divideRevenue(revenueQtr1, dilutedSharesOutstandingQtr1);
        revenuePerShareQtr2 = divideRevenue(revenueQtr2, dilutedSharesOutstandingQtr2);
        revenuePerShareQtr3 = divideRevenue(revenueQtr3, dilutedSharesOutstandingQtr3);
        revenuePerShareQtr4 = divideRevenue(revenueQtr4, dilutedSharesOutstandingQtr4);
        revenuePerShareQtr5 = divideRevenue(revenueQtr5, dilutedSharesOutstandingQtr5);
        revenuePerShareTTM = divideRevenue(revenueTTM, dilutedSharesOutstandingTTM);
        revenuePerShareTTMLastQtr = revenuePerShareQtr1.add(revenuePerShareQtr2)
                .add(revenuePerShareQtr3).add(revenuePerShareQtr4, mc);
    }

    private static boolean isRevenueRow(String label) {
        return "Revenue".equals(label) || "Total revenues".equals(label) || "Total net revenue".equals(label);
    }

    private static long parseLong(String value) {
        return (value == null || value.isEmpty()) ? 0 : Long.parseLong(value);
    }

    private static BigDecimal divideRevenue(long revenue, long shares) {
        if (shares == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(revenue).divide(BigDecimal.valueOf(shares), 2, RoundingMode.CEILING);
    }

    // Getters and setters
    public long getRevenueQtr1() { return revenueQtr1; }
    public void setRevenueQtr1(long v) { this.revenueQtr1 = v; }
    public long getRevenueQtr2() { return revenueQtr2; }
    public void setRevenueQtr2(long v) { this.revenueQtr2 = v; }
    public long getRevenueQtr3() { return revenueQtr3; }
    public void setRevenueQtr3(long v) { this.revenueQtr3 = v; }
    public long getRevenueQtr4() { return revenueQtr4; }
    public void setRevenueQtr4(long v) { this.revenueQtr4 = v; }
    public long getRevenueQtr5() { return revenueQtr5; }
    public void setRevenueQtr5(long v) { this.revenueQtr5 = v; }
    public long getRevenueTTM() { return revenueTTM; }
    public void setRevenueTTM(long v) { this.revenueTTM = v; }

    public long getBasicSharesOutstandingQtr1() { return basicSharesOutstandingQtr1; }
    public void setBasicSharesOutstandingQtr1(long v) { this.basicSharesOutstandingQtr1 = v; }
    public long getBasicSharesOutstandingQtr2() { return basicSharesOutstandingQtr2; }
    public void setBasicSharesOutstandingQtr2(long v) { this.basicSharesOutstandingQtr2 = v; }
    public long getBasicSharesOutstandingQtr3() { return basicSharesOutstandingQtr3; }
    public void setBasicSharesOutstandingQtr3(long v) { this.basicSharesOutstandingQtr3 = v; }
    public long getBasicSharesOutstandingQtr4() { return basicSharesOutstandingQtr4; }
    public void setBasicSharesOutstandingQtr4(long v) { this.basicSharesOutstandingQtr4 = v; }
    public long getBasicSharesOutstandingQtr5() { return basicSharesOutstandingQtr5; }
    public void setBasicSharesOutstandingQtr5(long v) { this.basicSharesOutstandingQtr5 = v; }
    public long getBasicSharesOutstandingTTM() { return basicSharesOutstandingTTM; }
    public void setBasicSharesOutstandingTTM(long v) { this.basicSharesOutstandingTTM = v; }

    public long getDilutedSharesOutstandingQtr1() { return dilutedSharesOutstandingQtr1; }
    public void setDilutedSharesOutstandingQtr1(long v) { this.dilutedSharesOutstandingQtr1 = v; }
    public long getDilutedSharesOutstandingQtr2() { return dilutedSharesOutstandingQtr2; }
    public void setDilutedSharesOutstandingQtr2(long v) { this.dilutedSharesOutstandingQtr2 = v; }
    public long getDilutedSharesOutstandingQtr3() { return dilutedSharesOutstandingQtr3; }
    public void setDilutedSharesOutstandingQtr3(long v) { this.dilutedSharesOutstandingQtr3 = v; }
    public long getDilutedSharesOutstandingQtr4() { return dilutedSharesOutstandingQtr4; }
    public void setDilutedSharesOutstandingQtr4(long v) { this.dilutedSharesOutstandingQtr4 = v; }
    public long getDilutedSharesOutstandingQtr5() { return dilutedSharesOutstandingQtr5; }
    public void setDilutedSharesOutstandingQtr5(long v) { this.dilutedSharesOutstandingQtr5 = v; }
    public long getDilutedSharesOutstandingTTM() { return dilutedSharesOutstandingTTM; }
    public void setDilutedSharesOutstandingTTM(long v) { this.dilutedSharesOutstandingTTM = v; }

    public BigDecimal getRevenuePerShareQtr1() { return revenuePerShareQtr1; }
    public void setRevenuePerShareQtr1(BigDecimal v) { this.revenuePerShareQtr1 = v; }
    public BigDecimal getRevenuePerShareQtr2() { return revenuePerShareQtr2; }
    public void setRevenuePerShareQtr2(BigDecimal v) { this.revenuePerShareQtr2 = v; }
    public BigDecimal getRevenuePerShareQtr3() { return revenuePerShareQtr3; }
    public void setRevenuePerShareQtr3(BigDecimal v) { this.revenuePerShareQtr3 = v; }
    public BigDecimal getRevenuePerShareQtr4() { return revenuePerShareQtr4; }
    public void setRevenuePerShareQtr4(BigDecimal v) { this.revenuePerShareQtr4 = v; }
    public BigDecimal getRevenuePerShareQtr5() { return revenuePerShareQtr5; }
    public void setRevenuePerShareQtr5(BigDecimal v) { this.revenuePerShareQtr5 = v; }
    public BigDecimal getRevenuePerShareTTM() { return revenuePerShareTTM; }
    public void setRevenuePerShareTTM(BigDecimal v) { this.revenuePerShareTTM = v; }
    public BigDecimal getRevenuePerShareTTMLastQtr() { return revenuePerShareTTMLastQtr; }
    public void setRevenuePerShareTTMLastQtr(BigDecimal v) { this.revenuePerShareTTMLastQtr = v; }

    public String getFiscalQtr1() { return fiscalQtr1; }
    public void setFiscalQtr1(String v) { this.fiscalQtr1 = v; }
    public String getFiscalQtr2() { return fiscalQtr2; }
    public void setFiscalQtr2(String v) { this.fiscalQtr2 = v; }
    public String getFiscalQtr3() { return fiscalQtr3; }
    public void setFiscalQtr3(String v) { this.fiscalQtr3 = v; }
    public String getFiscalQtr4() { return fiscalQtr4; }
    public void setFiscalQtr4(String v) { this.fiscalQtr4 = v; }
    public String getFiscalQtrPrevious() { return fiscalQtrPrevious; }
    public void setFiscalQtrPrevious(String v) { this.fiscalQtrPrevious = v; }
    public String getFiscalQtrCurrent() { return fiscalQtrCurrent; }
    public void setFiscalQtrCurrent(String v) { this.fiscalQtrCurrent = v; }

    public boolean isIncomplete() { return incomplete; }
    public void setIncomplete(boolean incomplete) { this.incomplete = incomplete; }
    public boolean isError() { return error; }
    public void setError(boolean error) { this.error = error; }

    @Override
    public MorningstarData call() throws Exception {
        downloadMorningstar(ticker);
        return this;
    }
}
