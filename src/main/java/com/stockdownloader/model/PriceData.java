package com.stockdownloader.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable representation of a single day's OHLCV price data.
 */
public record PriceData(
        String date,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal adjClose,
        long volume) {

    public PriceData {
        Objects.requireNonNull(date, "date must not be null");
        Objects.requireNonNull(open, "open must not be null");
        Objects.requireNonNull(high, "high must not be null");
        Objects.requireNonNull(low, "low must not be null");
        Objects.requireNonNull(close, "close must not be null");
        Objects.requireNonNull(adjClose, "adjClose must not be null");
        if (volume < 0) {
            throw new IllegalArgumentException("volume must not be negative");
        }
    }

    @Override
    public String toString() {
        return "%s O:%s H:%s L:%s C:%s V:%d".formatted(date, open, high, low, close, volume);
    }
}
