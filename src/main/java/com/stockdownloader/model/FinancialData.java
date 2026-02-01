package com.stockdownloader.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Fundamental financial data model holding revenue, shares outstanding,
 * and derived revenue-per-share metrics.
 */
public class FinancialData {

    private long[] revenue = new long[6];       // Qtr1-5 + TTM
    private long[] basicShares = new long[6];   // Qtr1-5 + TTM
    private long[] dilutedShares = new long[6]; // Qtr1-5 + TTM
    private BigDecimal[] revenuePerShare = new BigDecimal[6];
    private BigDecimal revenuePerShareTTMLastQtr = BigDecimal.ZERO;
    private String[] fiscalQuarters = new String[6]; // Qtr1-4, Previous, Current

    private boolean incomplete;
    private boolean error;

    public FinancialData() {
        for (int i = 0; i < 6; i++) {
            revenuePerShare[i] = BigDecimal.ZERO;
            fiscalQuarters[i] = "";
        }
    }

    public void computeRevenuePerShare() {
        for (int i = 0; i < 6; i++) {
            if (dilutedShares[i] == 0) {
                dilutedShares[i] = basicShares[i];
            }
        }
        for (int i = 0; i < 6; i++) {
            revenuePerShare[i] = divideRevenue(revenue[i], dilutedShares[i]);
        }
        revenuePerShareTTMLastQtr = revenuePerShare[0].add(revenuePerShare[1])
                .add(revenuePerShare[2]).add(revenuePerShare[3])
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal divideRevenue(long revenue, long shares) {
        if (shares == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(revenue).divide(BigDecimal.valueOf(shares), 2, RoundingMode.CEILING);
    }

    // Array-indexed accessors
    public long getRevenue(int quarter) { return revenue[quarter]; }
    public void setRevenue(int quarter, long value) { revenue[quarter] = value; }
    public long getBasicShares(int quarter) { return basicShares[quarter]; }
    public void setBasicShares(int quarter, long value) { basicShares[quarter] = value; }
    public long getDilutedShares(int quarter) { return dilutedShares[quarter]; }
    public void setDilutedShares(int quarter, long value) { dilutedShares[quarter] = value; }
    public BigDecimal getRevenuePerShare(int quarter) { return revenuePerShare[quarter]; }
    public String getFiscalQuarter(int index) { return fiscalQuarters[index]; }
    public void setFiscalQuarter(int index, String value) { fiscalQuarters[index] = value; }

    // Convenience accessors for TTM
    public BigDecimal getRevenuePerShareTTM() { return revenuePerShare[5]; }
    public BigDecimal getRevenuePerShareTTMLastQtr() { return revenuePerShareTTMLastQtr; }

    public boolean isIncomplete() { return incomplete; }
    public void setIncomplete(boolean v) { this.incomplete = v; }
    public boolean isError() { return error; }
    public void setError(boolean v) { this.error = v; }
}
