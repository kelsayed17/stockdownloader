package com.stockdownloader.strategy;

import com.stockdownloader.model.OptionType;
import com.stockdownloader.model.PriceData;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interface for options trading strategies that operate on underlying price data
 * and generate signals for opening/closing options positions at specific strikes
 * and expirations.
 */
public interface OptionsStrategy {

    enum Signal { OPEN, CLOSE, HOLD }

    String getName();

    /**
     * Evaluate the strategy and return a signal.
     *
     * @param data         underlying price history
     * @param currentIndex current bar index
     * @return OPEN to enter a position, CLOSE to exit, HOLD to do nothing
     */
    Signal evaluate(List<PriceData> data, int currentIndex);

    /**
     * Get the option type this strategy trades.
     */
    OptionType getOptionType();

    /**
     * Whether this strategy sells options (writes) or buys them.
     */
    boolean isShort();

    /**
     * Calculate the target strike price based on current market conditions.
     *
     * @param currentPrice current underlying price
     * @return target strike price
     */
    BigDecimal getTargetStrike(BigDecimal currentPrice);

    /**
     * Get the target days to expiration for new positions.
     */
    int getTargetDaysToExpiry();

    /**
     * Number of warmup bars needed before the strategy can generate signals.
     */
    int getWarmupPeriod();
}
