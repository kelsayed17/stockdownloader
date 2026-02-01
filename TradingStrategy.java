import java.util.List;

public sealed interface TradingStrategy
        permits SMACrossoverStrategy, RSIStrategy, MACDStrategy {

    enum Signal { BUY, SELL, HOLD }

    String getName();

    Signal evaluate(List<PriceData> data, int currentIndex);

    int getWarmupPeriod();
}
