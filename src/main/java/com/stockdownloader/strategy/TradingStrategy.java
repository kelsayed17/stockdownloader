package com.stockdownloader.strategy;

import com.stockdownloader.model.PriceData;

import java.util.List;

/**
 * Interface defining the contract for trading strategies.
 * Each implementation evaluates price data and produces BUY/SELL/HOLD signals.
 */
public interface TradingStrategy {

    enum Signal { BUY, SELL, HOLD }

    String getName();

    Signal evaluate(List<PriceData> data, int currentIndex);

    int getWarmupPeriod();
}
