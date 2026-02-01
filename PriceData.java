import java.math.BigDecimal;

public class PriceData {
    private String date;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal adjClose;
    private long volume;

    public PriceData(String date, BigDecimal open, BigDecimal high, BigDecimal low,
                     BigDecimal close, BigDecimal adjClose, long volume) {
        this.date = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.adjClose = adjClose;
        this.volume = volume;
    }

    public String getDate() { return date; }
    public BigDecimal getOpen() { return open; }
    public BigDecimal getHigh() { return high; }
    public BigDecimal getLow() { return low; }
    public BigDecimal getClose() { return close; }
    public BigDecimal getAdjClose() { return adjClose; }
    public long getVolume() { return volume; }

    @Override
    public String toString() {
        return date + " O:" + open + " H:" + high + " L:" + low + " C:" + close + " V:" + volume;
    }
}
