import java.util.List;

public interface TradingStrategy {

    enum Signal { BUY, SELL, HOLD }

    String getName();

    Signal evaluate(List<PriceData> data, int currentIndex);

    int getWarmupPeriod();
}
