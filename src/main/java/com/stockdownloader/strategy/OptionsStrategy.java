package com.stockdownloader.strategy;

import com.stockdownloader.model.OptionTrade;
import com.stockdownloader.model.PriceData;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interface for options trading strategies that generate option trades
 * based on underlying price data. Strategies specify entry conditions,
 * strike selection, expiration targeting, and exit conditions.
 */
public interface OptionsStrategy {

    enum Action { OPEN, CLOSE, HOLD }

    String getName();

    /**
     * Evaluates whether to open, close, or hold based on current market state.
     */
    Action evaluate(List<PriceData> data, int currentIndex, List<OptionTrade> openTrades);

    /**
     * Creates the option trade(s) for this strategy when an OPEN action is triggered.
     * Returns one or more OptionTrade objects representing the legs of the strategy.
     *
     * @param data historical price data
     * @param currentIndex current bar index
     * @param availableCapital capital available for the trade
     * @return list of trades to open (can be multi-leg)
     */
    List<OptionTrade> createTrades(List<PriceData> data, int currentIndex, BigDecimal availableCapital);

    /**
     * Target days to expiration for this strategy (e.g., 30 for monthly, 7 for weekly).
     */
    int getTargetDTE();

    /**
     * Minimum volume required to enter a trade. Ensures liquidity.
     */
    long getMinVolume();

    int getWarmupPeriod();
}
