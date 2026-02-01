import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class MACDStrategy implements TradingStrategy {
    private int fastPeriod;
    private int slowPeriod;
    private int signalPeriod;

    public MACDStrategy(int fastPeriod, int slowPeriod, int signalPeriod) {
        this.fastPeriod = fastPeriod;
        this.slowPeriod = slowPeriod;
        this.signalPeriod = signalPeriod;
    }

    @Override
    public String getName() {
        return "MACD (" + fastPeriod + "/" + slowPeriod + "/" + signalPeriod + ")";
    }

    @Override
    public Signal evaluate(List<PriceData> data, int currentIndex) {
        int minRequired = slowPeriod + signalPeriod;
        if (currentIndex < minRequired) {
            return Signal.HOLD;
        }

        BigDecimal currentMACD = calculateMACD(data, currentIndex);
        BigDecimal currentSignal = calculateSignalLine(data, currentIndex);
        BigDecimal prevMACD = calculateMACD(data, currentIndex - 1);
        BigDecimal prevSignal = calculateSignalLine(data, currentIndex - 1);

        boolean macdAboveSignalNow = currentMACD.compareTo(currentSignal) > 0;
        boolean macdAboveSignalPrev = prevMACD.compareTo(prevSignal) > 0;

        // Bullish crossover: MACD crosses above signal line
        if (macdAboveSignalNow && !macdAboveSignalPrev) {
            return Signal.BUY;
        }

        // Bearish crossover: MACD crosses below signal line
        if (!macdAboveSignalNow && macdAboveSignalPrev) {
            return Signal.SELL;
        }

        return Signal.HOLD;
    }

    @Override
    public int getWarmupPeriod() {
        return slowPeriod + signalPeriod;
    }

    private BigDecimal calculateEMA(List<PriceData> data, int endIndex, int period) {
        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (period + 1));
        BigDecimal oneMinusMultiplier = BigDecimal.ONE.subtract(multiplier);

        // Start with SMA as seed
        BigDecimal ema = BigDecimal.ZERO;
        int startIndex = endIndex - period - period; // extra buffer
        if (startIndex < 0) startIndex = 0;

        // SMA seed
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = startIndex; i < startIndex + period && i <= endIndex; i++) {
            sum = sum.add(data.get(i).getClose());
        }
        ema = sum.divide(BigDecimal.valueOf(period), 10, RoundingMode.HALF_UP);

        // Apply EMA formula forward
        for (int i = startIndex + period; i <= endIndex; i++) {
            ema = data.get(i).getClose().multiply(multiplier)
                    .add(ema.multiply(oneMinusMultiplier))
                    .setScale(10, RoundingMode.HALF_UP);
        }

        return ema;
    }

    private BigDecimal calculateMACD(List<PriceData> data, int endIndex) {
        BigDecimal fastEMA = calculateEMA(data, endIndex, fastPeriod);
        BigDecimal slowEMA = calculateEMA(data, endIndex, slowPeriod);
        return fastEMA.subtract(slowEMA);
    }

    private BigDecimal calculateSignalLine(List<PriceData> data, int endIndex) {
        // Signal line is EMA of MACD values over signalPeriod
        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (signalPeriod + 1));
        BigDecimal oneMinusMultiplier = BigDecimal.ONE.subtract(multiplier);

        // Calculate MACD values for the signal period window
        int startIndex = endIndex - signalPeriod + 1;
        if (startIndex < slowPeriod) startIndex = slowPeriod;

        // SMA seed of MACD values
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (int i = startIndex; i < startIndex + signalPeriod && i <= endIndex; i++) {
            sum = sum.add(calculateMACD(data, i));
            count++;
        }
        if (count == 0) return BigDecimal.ZERO;

        BigDecimal signalEMA = sum.divide(BigDecimal.valueOf(count), 10, RoundingMode.HALF_UP);

        // Apply EMA forward for remaining points
        for (int i = startIndex + count; i <= endIndex; i++) {
            BigDecimal macdVal = calculateMACD(data, i);
            signalEMA = macdVal.multiply(multiplier)
                    .add(signalEMA.multiply(oneMinusMultiplier))
                    .setScale(10, RoundingMode.HALF_UP);
        }

        return signalEMA;
    }
}
